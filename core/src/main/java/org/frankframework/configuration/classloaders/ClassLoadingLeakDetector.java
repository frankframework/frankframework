/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.configuration.classloaders;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;

import org.frankframework.util.CleanerProvider;

@NullMarked
public class ClassLoadingLeakDetector {
	private static final Logger LEAK_LOG = LogManager.getLogger("LEAK_LOG");

	private static final AtomicInteger COUNTER = new AtomicInteger();

	private ClassLoadingLeakDetector() {
		// Private constructor to prevent creation of instances
	}

	private static final ConcurrentMap<Integer, ClassLoaderMeta> classLoaders = new ConcurrentHashMap<>();


	private static class ClassLoaderMeta implements Runnable {
		final int sequenceNr;
		final Integer classLoaderId;
		final String configurationName;
		final String className;
		final WeakReference<ClassLoader> classLoaderRef;
		final Set<String> resourceLeakTracker = new HashSet<>();
		boolean destroyed; // Track if destroy has been called on a ClassLoader; a true leak is when a destroyed classloader cannot be garbage-collected

		private ClassLoaderMeta(String configurationName, ClassLoader classLoader) {
			this.sequenceNr = COUNTER.incrementAndGet();
			this.configurationName = configurationName;
			this.classLoaderId = getClassLoaderKey(classLoader);
			this.className = classLoader.getClass().getName();
			this.classLoaderRef = new WeakReference<>(classLoader);
		}

		@Override
		public void run() {
			classLoaders.remove(classLoaderId);
			LEAK_LOG.info("Cleaning [{}] for configuration [{}], classes not unloaded: [{}]", className, configurationName, resourceLeakTracker);
		}

		@Override
		public String toString() {
			return "[%d] %s for configuration [%s], destroyed: [%b], potentially leaked: [%b], loaded classes: %s"
					.formatted(sequenceNr, className, configurationName, destroyed, isPotentiallyLeaked(), resourceLeakTracker);
		}

		/**
		 * Check if a classloader may potentially have been leaked. Note that a {@code true}
		 * return from this check does not mean it is an actual leak.
		 *
		 * <p>
		 *     A potential leak is decided considering the following states:
		 *     <ul>
		 *         <li>If a classloader is not yet destroyed, it is still in use so it is not a potential leak.</li>
		 *         <li>If the weak reference to the classloader has been cleared, it will eventually be garbage-collected< so it is not leaked./li>
		 *         <li>If the classloader has been destroyed but the wek reference still exists, this may be an indication that there are still live active references but this does not have to be so.</li>
		 *     </ul>
		 *     Unfortunately the only way to know for sure if a classloader can never be garbage-collected is to make a heapdump and
		 *     do a full heap analysis.
		 * </p>
		 */
		private boolean isPotentiallyLeaked() {
			return destroyed && !classLoaderRef.refersTo(null);
		}
	}

	/**
	 * Static inner class that cleans up for the leak-detector, without needing any references to the ClassLoader.
	 * This class removes names of classes when they're unloaded / garbage-collected so we do not report erroneously on classes that are not leaking.
	 */
	private record ResourceTrackingCleanup(String configurationName, Set<String> resourceLeakTracker, String name) implements Runnable {
		@Override
		public void run() {
			resourceLeakTracker.remove(name);
		}
	}

	static {
		// Shutdown hook to generate report of leaks at end of process
		Runtime.getRuntime().addShutdownHook(new Thread(ClassLoadingLeakDetector::logLeakStatistics));
	}

	@SuppressWarnings({"java:S1215", "java:S1181"}) // Ignore warnings for calling gc() and catching Throwable
	public static void logLeakStatistics() {
		// Force system to run garbage collection to clean up as much as possible and collect as much leak-info before logging it
		System.gc();
		try {
			// Give time to GC thread.
			Thread.sleep(500L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		try {
			if (classLoaders.isEmpty()) {
				LEAK_LOG.info("No remaining classloaders registered with the leak-detector; total that have been registered: [{}]", COUNTER::get);
				return;
			}
			LEAK_LOG.warn("Currently registered classloaders out of total [{}] registered:", COUNTER::get);
			for (ClassLoaderMeta classLoaderMeta : classLoaders.values()) {
				LEAK_LOG.warn(" {}", classLoaderMeta);
			}
		} catch (Throwable e) {
			// Ignore log exceptions which may cause the application to not terminate properly.
			// Such as `Exception in thread "Thread-462" java.lang.NoClassDefFoundError: org/apache/logging/log4j/message/ParameterizedNoReferenceMessageFactory$StatusMessage`
		}
	}

	public static void registerClassLoader(String configurationName, ClassLoader classLoader) {
		classLoaders.computeIfAbsent(
				getClassLoaderKey(classLoader), k -> {
					ClassLoaderMeta classLoaderMeta = new ClassLoaderMeta(configurationName, classLoader);
					CleanerProvider.CLEANER.register(classLoader, classLoaderMeta);
					return classLoaderMeta;
				}
		);
	}

	private static int getClassLoaderKey(ClassLoader classLoader) {
		return System.identityHashCode(classLoader);
	}

	static void destroyed(ClassLoader classLoader) {
		ClassLoaderMeta classLoaderMeta = classLoaders.get(getClassLoaderKey(classLoader));
		if (classLoaderMeta != null) {
			classLoaderMeta.destroyed = true;
		}
	}

	static void registerResource(ClassLoader classLoader, Class<?> definedClass) {
		ClassLoaderMeta classLoaderMeta = classLoaders.get(getClassLoaderKey(classLoader));
		if (classLoaderMeta == null) {
			LEAK_LOG.warn("CassLoader [{}] not registered", classLoader.getClass().getName());
			return;
		}
		String className = definedClass.getName();
		classLoaderMeta.resourceLeakTracker.add(className);
		CleanerProvider.CLEANER.register(definedClass, new ResourceTrackingCleanup(classLoaderMeta.configurationName, classLoaderMeta.resourceLeakTracker, className));
	}
}
