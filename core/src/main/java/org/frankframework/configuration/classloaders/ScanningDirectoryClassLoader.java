/*
   Copyright 2019-2021, 2023-2026 WeAreFrank!

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

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.management.Action;

/**
 * Actively scans the configuration directory for file changes.
 * When it finds a file change it will automatically try to reload the configuration
 * Has a default cooldown after a change has been detected before it can be triggered again.
 *
 * @author Niels Meijer
 */
public class ScanningDirectoryClassLoader extends DirectoryClassLoader {

	private ScheduledThreadPoolExecutor executor;
	private ScheduledFuture<?> future;

	private int scanInterval = 10;
	private long lastScanTime;

	public ScanningDirectoryClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ClassLoaderException {
		super.configure(ibisContext, configurationName);

		createTaskExecutor();

		lastScanTime = System.currentTimeMillis();
		if (scanInterval > 0) {
			schedule();
		}
	}

	private void createTaskExecutor() {
		String threadName = this.toString();
		ThreadFactory namedThreadFactory = runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName(threadName);
			thread.setDaemon(true);
			return thread;
		};
		executor = new ScheduledThreadPoolExecutor(1, namedThreadFactory);
		executor.setRemoveOnCancelPolicy(false);
		executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
	}

	@Override
	public synchronized void destroy() {
		super.destroy();

		if (future != null) {
			future.cancel(false);
			future = null;
		}

		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
	}

	public void setScanInterval(int interval) throws ConfigurationException {
		if (interval < 10)
			throw new ConfigurationException("minimum scaninterval is 10 seconds");

		log.debug("scanInterval set to [{}] seconds", interval);
		this.scanInterval = interval;
	}

	/**
	 * Create a new schedule with a default delay of 30 seconds
	 * @see #schedule(int)
	 */
	private void schedule() {
		schedule(30);
	}

	/**
	 * Create a new schedule to check if file in the given directory have been changed
	 * @param delay cooldown/startup delay before the scheduler should start looking for file changes
	 */
	private synchronized void schedule(int delay) {
		if (future != null) {
			future.cancel(false);
		}

		if (executor == null) {
			log.debug("Cannot reschedule tasks because executor is null");
			return;
		}
		log.debug("starting new scheduler, interval [{}] delay [{}]", scanInterval, delay);
		future = executor.scheduleAtFixedRate(ScanningDirectoryClassLoader.this::scan, delay, scanInterval, TimeUnit.SECONDS);
	}

	protected void scan() {
		if (log.isTraceEnabled()) log.trace("running directory scanner on directory [{}]", getDirectory());
		long scanTime = System.currentTimeMillis();
		File[] files = getDirectory().listFiles();
		if (hasBeenModified(files)) {
			log.debug("detected file change, reloading configuration");
			getIbisManager().handleAction(Action.RELOAD, getConfigurationName(), null, null, toString(), false);
		}
		lastScanTime = scanTime;
	}

	/**
	 * Loop through a file array and check if one of the files has been modefied.
	 * @see #hasBeenModified(File)
	 */
	private boolean hasBeenModified(File @Nullable [] files) {
		if (files == null) {
			return false;
		}
		boolean changed;
		for (File file : files) {
			if (file.isDirectory()) {
				changed = hasBeenModified(file.listFiles());
			} else {
				changed = hasBeenModified(file);
			}

			if (changed) {
				// Only return something when a change has been detected
				return changed;
			}
		}
		return false;
	}

	/**
	 * @return true if a file has been changed in the last 'scanInterval' seconds
	 */
	private boolean hasBeenModified(File file) {
		if (log.isTraceEnabled()) log.trace("scanning file [{}] lastModDate [{}]", file.getName(), file.lastModified());
		boolean modified = file.lastModified() >= lastScanTime;

		if (log.isDebugEnabled() && modified) {
			log.debug("file [{}] has been changed in the last [{}] seconds", file.getAbsolutePath(), scanInterval);
		}

		return modified;
	}
}
