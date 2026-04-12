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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.CleanerProvider;

public class ClassLoadingLeakDetector {
	private static final Logger LEAK_LOG = LogManager.getLogger("LEAK_LOG");

	private ClassLoadingLeakDetector() {
		// Private constructor to prevent creation of instances
	}

	private static final ConcurrentMap<String, ClassLoaderMeta> classLoaders = new ConcurrentHashMap<>();


	private static class ClassLoaderMeta implements Runnable {
		final String configurationName;
		final String className;
		final Set<String> resourceLeakTracker = new HashSet<>();
		boolean destroyed; // Track if destroy has been called on a ClassLoader; a true leak is when a destroyed classloader cannot be garbage-collected

		private ClassLoaderMeta(String configurationName, String className) {
			this.configurationName = configurationName;
			this.className = className;
		}

		@Override
		public void run() {
			classLoaders.remove(configurationName);
			LEAK_LOG.info("Cleaning [{}] for configuration [{}], classes not unloaded: [{}]", className, configurationName, resourceLeakTracker);
		}

		@Override
		public String toString() {
			return "%s for configuration [%s], destroyed: [%b], loaded classes: %s".formatted(className, configurationName, destroyed, resourceLeakTracker);
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
				LEAK_LOG.info("No classloaders registered with the leak-detector");
				return;
			}
			LEAK_LOG.warn("Currently registered classloaders:");
			for (ClassLoaderMeta classLoaderMeta : classLoaders.values()) {
				LEAK_LOG.warn(classLoaderMeta.toString());
			}
		} catch (Throwable e) {
			// Ignore log exceptions which may cause the application to not terminate properly.
			// Such as `Exception in thread "Thread-462" java.lang.NoClassDefFoundError: org/apache/logging/log4j/message/ParameterizedNoReferenceMessageFactory$StatusMessage`

		}
	}

	public static void registerClassLoader(@NonNull String configurationName, @NonNull ClassLoader classLoader) {
		classLoaders.computeIfAbsent(
				configurationName, k -> {
					ClassLoaderMeta classLoaderMeta = new ClassLoaderMeta(configurationName, classLoader.getClass().getName());
					CleanerProvider.CLEANER.register(classLoader, classLoaderMeta);
					return classLoaderMeta;
				}
		);
	}

	static void destroyed(@NonNull String configurationName) {
		ClassLoaderMeta classLoaderMeta = classLoaders.get(configurationName);
		if (classLoaderMeta != null) {
			classLoaderMeta.destroyed = true;
		}
	}

	static void registerResource(@NonNull String configurationName, Class<?> definedClass) {
		ClassLoaderMeta classLoaderMeta = classLoaders.get(configurationName);
		if (classLoaderMeta == null) {
			LEAK_LOG.warn("CassLoader for configuration [{}] not registered", configurationName);
			return;
		}
		String className = definedClass.getName();
		classLoaderMeta.resourceLeakTracker.add(className);
		CleanerProvider.CLEANER.register(definedClass, new ResourceTrackingCleanup(configurationName, classLoaderMeta.resourceLeakTracker, className));
	}
}
