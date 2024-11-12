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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.gc();
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			LEAK_MAP.forEach((creationTrace, value) -> {
				LEAK_LOG.warn("Class [{}] has {} leaks recorded from instances created at:\n{}", creationTrace.owningClassName, value.get(),
						Arrays.stream(creationTrace.topOfStackTrace).map(StackTraceElement::toString).collect(Collectors.joining("\n    at ")));
			});
		}));
	}

	public static int register(Object obj, Runnable cleaningAction) {
		CleaningActionWrapper wrapper = new CleaningActionWrapper(obj, cleaningAction);
		CLEANER_MAP.put(wrapper.actionId, wrapper);
		wrapper.cleanable = CLEANER.register(obj, wrapper);
		return wrapper.actionId;
	}

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
			if (!cleaned && !isProxyClass) {
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

		LeakedResourceException(Object owner) {
			super(owner.getClass().getName());
			this.owningClassName = owner.getClass().getName();
			StackTraceElement[] originalStackTrace = getStackTrace();
			this.topOfStackTrace = Arrays.copyOfRange(originalStackTrace, 2, Math.min(6, originalStackTrace.length));
			setStackTrace(topOfStackTrace);
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
