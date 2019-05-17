/*
   Copyright 2016, 2018 - 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration.classloaders;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;

public class DirectoryClassLoader extends ClassLoaderBase {
	private File directory = null;
	private File scanDirectory = null;

	private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);
	private int scanInterval = 0;
	private ScheduledFuture<?> future;

	public DirectoryClassLoader(ClassLoader parent) throws ConfigurationException {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ConfigurationException {
		super.configure(ibisContext, configurationName);

		if (directory == null) {
			AppConstants appConstants = AppConstants.getInstance();
			String configurationsDirectory = appConstants.getResolvedProperty("configurations.directory");
			if (configurationsDirectory == null) {
				throw new ConfigurationException("Could not find property configurations.directory");
			}

			this.directory = new File(configurationsDirectory);
		}

		if (!this.directory.isDirectory()) {
			throw new ConfigurationException("Could not find directory to load configuration from: " + this.directory);
		}

		if(scanInterval > 0) {
			//Find out the actual root of the directory
			String configurationFile = ibisContext.getConfigurationFile(configurationName);
			int i = configurationFile.lastIndexOf('/');
			String basePath = "";
			if (i != -1) {
				basePath = configurationFile.substring(0, i + 1);
			}
			scanDirectory = new File(directory, basePath);
			log.debug("found scanDirectory ["+scanDirectory+"]");
			if(scanDirectory.exists()) {
				log.info("starting auto-refresh schedule on directory ["+scanDirectory +"]");
				schedule();
			}
			else
				log.warn("unable to determine scanDirectory, unable to auto-refresh configurations");
		}
	}

	/**
	 * Set the directory from which the configuration files should be loaded
	 * @throws ConfigurationException if the directory can't be found
	 */
	public void setDirectory(String directory) throws ConfigurationException {
		File dir = new File(directory);
		if(!dir.isDirectory())
			throw new ConfigurationException("directory ["+directory+"] not found");

		this.directory = dir;
	}

	public void setScanInterval(int interval) throws ConfigurationException {
		if(interval < 5)
			throw new ConfigurationException("Minimum scaninterval is 5 seconds");

		log.debug("scanInterval set to ["+interval+"] seconds");
		this.scanInterval = interval;
	}

	@Override
	public URL getResource(String name) {
		File file = new File(directory, name);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				log.error("Could not create url for '" + name + "'", e);
			}
		}

		return super.getResource(name);
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
	 * @param delay how long the wait until the schedule starts
	 */
	private void schedule(int delay) {
		if (future != null) {
			future.cancel(false);
		}

		log.debug("starting new scheduler, interval ["+scanInterval+"] delay ["+delay+"]");
		future = EXECUTOR.scheduleAtFixedRate(new Runnable() {
			public void run() {
				DirectoryClassLoader.this.scan();
			}
		}, delay, scanInterval, TimeUnit.SECONDS);
	}

	protected synchronized void scan() {
		log.trace("running directory scanner on directory ["+directory+"]");
		File[] files = scanDirectory.listFiles();
		if(hasBeenModified(files)) {
			log.debug("detected file change, reloading configuration");
			getIbisContext().reload(getConfigurationName());

			schedule();
		}
	}

	/**
	 * Loop through a file array and check if one of the files has been modefied.
	 * @see #hasBeenModified(File)
	 */
	private boolean hasBeenModified(File[] files) {
		boolean changed = false;
		for (File file : files) {
			if(file.isDirectory())
				changed = hasBeenModified(file.listFiles());
			else
				changed = hasBeenModified(file);

			if(changed) //Only return something when a change has been detected
				return changed;
		}
		return false;
	}

	/**
	 * @return true if a file has been changed in the last 'scanInterval' seconds
	 */
	private boolean hasBeenModified(File file) {
		log.trace("scanning file ["+ file.getName() +"] lastModDate ["+ file.lastModified() +"]");
		boolean modified = file.lastModified() + scanInterval*1000 >= System.currentTimeMillis();

		if(log.isDebugEnabled() && modified)
			log.debug("file ["+file.getAbsolutePath()+"] has been changed in the last ["+scanInterval+"] seconds");

		return modified;
	}
}