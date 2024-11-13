/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.util;

import java.lang.ref.Cleaner;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.NoArgsConstructor;

/**
 * Starts the singleton Cleaner thread of F!F, to clean a resource when it becomes phantom reachable.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class CleanerProvider {
	private static final Logger LEAK_LOG = LogManager.getLogger("LEAK_LOG");

	private static final Map<Integer, CleaningActionWrapper> CLEANER_MAP = new HashMap<>();
	private static final Map<LeakedResourceException, AtomicInteger> LEAK_MAP = new HashMap<>();
	private static final Cleaner CLEANER = Cleaner.create();

	static {
		 // Shutdown hook to generate report of leaks at end of process
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			// Force system to run garbage collection to clean up as much as possible
			System.runFinalization();
			System.gc();
			try {
				// Give time to GC thread.
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			// Count total leaks and write a log message with each stack-trace, for finding the traces where problems originate.
			int totalLeaks = LEAK_MAP.entrySet().stream()
					.sorted(Comparator.comparingInt(entry -> entry.getValue().get()))
					.peek(entry -> LEAK_LOG.warn("Class [{}] has {} leaks recorded from instances created at:", entry.getKey().owningClassName, entry.getValue().get(), entry.getKey()))
					.map(entry -> entry.getValue().get())
					.reduce(0, Integer::sum);
			LEAK_LOG.warn("Total of {} leaks from {} locations", totalLeaks, LEAK_MAP.size());
		}));
	}

	/**
	 * Method to register a cleaning-action for a method.
	 *
	 * @param obj Object for which the cleaning-action should be registered
	 * @param cleaningAction Cleaning Action to register
	 * @return ID of the cleaning action, which can be used to execute it.
	 */
	public static int register(Object obj, Runnable cleaningAction) {
		CleaningActionWrapper wrapper = new CleaningActionWrapper(obj, cleaningAction);
		CLEANER_MAP.put(wrapper.actionId, wrapper);
		wrapper.cleanable = CLEANER.register(obj, wrapper);
		return wrapper.actionId;
	}

	/**
	 * Execute the cleaning action, if it needs to be manually invoked before the object
	 * is out of scope, for instance from a close-method.
	 * This de-registers the cleaning action and marks it so that it is not counted as leaked.
	 *
	 * @param cleaningAction Cleaning Action to execute.
	 */
	public static void clean(Runnable cleaningAction) {
		if (cleaningAction == null) {
			return;
		}
		int actionId = getActionId(cleaningAction);
		CleaningActionWrapper wrapper = CLEANER_MAP.get(actionId);
		if (wrapper != null) {
			wrapper.cleaned = true;
			wrapper.cleanable.clean();
		} else {
			cleaningAction.run();
		}
	}

	/**
	 * Execute cleaning action by ID.
	 *
	 * @param actionId ID of the cleaning action.
	 */
	public static void clean(int actionId) {
		CleaningActionWrapper wrapper = CLEANER_MAP.get(actionId);
		if (wrapper != null) {
			wrapper.cleaned = true;
			wrapper.cleanable.clean();
		}
	}

	private static int recordLeak(LeakedResourceException leakedResourceException) {
		AtomicInteger counter = LEAK_MAP.computeIfAbsent(leakedResourceException, key -> new AtomicInteger());
		return counter.incrementAndGet();
	}

	private static class CleaningActionWrapper implements Runnable {
		private final Runnable cleaningAction;
		private final int actionId;
		private final String owningClassName;
		private final boolean isProxyClass;
		private final LeakedResourceException creationTrace;
		private boolean cleaned = false;
		private Cleaner.Cleanable cleanable;

		private CleaningActionWrapper(Object owner, Runnable cleaningAction) {
			this.cleaningAction = cleaningAction;
			actionId = getActionId(cleaningAction);
			owningClassName = owner.getClass().getName();
			isProxyClass = Proxy.isProxyClass(owner.getClass()) || owningClassName.contains("$$");
			creationTrace = new LeakedResourceException(owner);
		}

		@Override
		public void run() {
			if (!cleaned && !isProxyClass && !creationTrace.looksLikeUnitTest) {
				int leaks = recordLeak(creationTrace);
				LEAK_LOG.debug("Cleaning action {}, class [{}] has not been cleaned, {} times for this trace", actionId, owningClassName, leaks, creationTrace);
			}
			cleaningAction.run();
			CLEANER_MAP.remove(actionId);
		}
	}

	private static int getActionId(Runnable cleaningAction) {
		return System.identityHashCode(cleaningAction);
	}

	private static class LeakedResourceException extends RuntimeException {
		private final String owningClassName;
		private final StackTraceElement[] topOfStackTrace;
		private final boolean looksLikeUnitTest;

		LeakedResourceException(Object owner) {
			super(owner.getClass().getName());
			this.owningClassName = owner.getClass().getName();
			// Find part of stacktrace that is relevant to point where leaking item was created
			StackTraceElement[] originalStackTrace = getStackTrace();
			int bottom = findOriginPointInStackTrace(originalStackTrace);
			this.topOfStackTrace = Arrays.copyOfRange(originalStackTrace, 2, bottom+1);
			this.looksLikeUnitTest = isLikelyUnitTest(originalStackTrace[bottom]);
			setStackTrace(topOfStackTrace);
		}

		/**
		 * Use some heuristics to find point in stacktrace where leaking item originated.
		 *
		 * @param fullStackTrace stack trace elements in array
		 * @return index of likely origin element
		 */
		private int findOriginPointInStackTrace(StackTraceElement[] fullStackTrace) {
			if (fullStackTrace.length < 3) {
				return fullStackTrace.length - 1;
			}
			StackTraceElement first = fullStackTrace[2];
			for (int i = 3; i < fullStackTrace.length; i++) {
				StackTraceElement element = fullStackTrace[i];
				if (((element.getFileName() != null && !element.getFileName().equals(first.getFileName()))
				|| (element.getFileName() == null && !element.getClassName().equals(first.getClassName())))
						&& (element.getClassName().startsWith("org.frankframework.") || element.getClassName().startsWith("nl.nn."))
				// Some special-case exceptions
						&& !element.getClassName().endsWith("Message") && !element.getClassName().endsWith("OverflowToDiskOutputStream")
				&& !element.getClassName().endsWith("Result") && !element.getClassName().endsWith("MessageBuilder") && !element.getMethodName().endsWith("createResultMessage")
				&& !(element.getClassName().endsWith("TransformerPool") && element.getMethodName().endsWith("transform"))) {
					return i;
				}
			}
			return fullStackTrace.length - 1;
		}

		/**
		 * Apply some heuristics to check if an element is likely unit-test code -- leaks originating in unit tests should be ignored.
		 *
		 * @param element Element to check
		 * @return true/false
		 */
		private boolean isLikelyUnitTest(StackTraceElement element) {
			return element.getClassName().contains("Test") || element.getMethodName().startsWith("test");
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			return obj instanceof LeakedResourceException other && Arrays.equals(this.topOfStackTrace, other.topOfStackTrace);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(topOfStackTrace);
		}
	}
}
