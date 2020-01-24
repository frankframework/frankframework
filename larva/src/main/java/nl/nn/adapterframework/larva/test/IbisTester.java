/*
   Copyright 2019-2020 Integration Partners

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
package nl.nn.adapterframework.larva.test;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.larva.MessageListener;
import nl.nn.adapterframework.larva.TestTool;
import nl.nn.adapterframework.lifecycle.IbisApplicationContext.BootState;
import nl.nn.adapterframework.util.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is a class for automatically starting Ibis for command line based testing tools
 * that does not rely on Servlet implementations.
 */
public class IbisTester {

	private AppConstants appConstants;
	private IbisContext ibisContext;
	private final int MAX_TRIES = 30;
	private final int TIMEOUT = 10000;
	private final int CONTEXT_TIMEOUT = 300000;

	public static void main(String[] args) throws IllegalArgumentException, IOException, URISyntaxException {
		if(args.length != 7) {
			throw new IllegalArgumentException("Given argument size does not match the expected size! " +
					"The expected arguments are as follows:\n" +
					"'[Current Path]' '[Execute Path]' '[Root Directories Path]' '[Number of threads]' '[Timeout]' '[Wait Before Cleanup]' '[Output File]'");
		}
		// Parse arguments
		File current = new File(args[0].replace("'", ""));

		// Make sure rootDirectories and paramExecute are absolute paths.
		Path executePath = Paths.get(args[1]);
		String paramExecute =  executePath.toString();
		if(! executePath.isAbsolute())
			paramExecute = new File(current, paramExecute).getCanonicalPath();

		Path rootDirectoriesPath = Paths.get(args[2]);
		String rootDirectories = rootDirectoriesPath.toString();
		if(! rootDirectoriesPath.isAbsolute())
			rootDirectories = new File(current, rootDirectories).getCanonicalPath();

		FileWriter writer = null;
		String outputFile = Paths.get(args[6]).toString();
		if(! outputFile.equalsIgnoreCase("")) {
			Path outputPath = Paths.get(outputFile);
			if (!outputPath.isAbsolute())
				outputFile = new File(current, outputFile).getCanonicalPath();
			writer = new FileWriter(outputFile);
		}
		int threads = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);
		int waitBeforeCleanup = Integer.parseInt(args[5]);

		IbisTester ibisTester = new IbisTester();
		ibisTester.initTester();

		MessageListener messageListener = new MessageListener();

		TestTool testTool = new TestTool(messageListener);

		int testsFailing = testTool.runScenarios(paramExecute, waitBeforeCleanup, rootDirectories, threads, timeout);

		debug(testsFailing + " test(s) failed!");

		if(writer != null) {
			JSONArray messages = messageListener.getMessages();
			writer.write(messages.toString());
			writer.close();
		}
	}

	/**
	 * Initializes the environment, including ibisContext and all the registered adapters.
	 * @return true if everything has been initialized successfully.
	 */
	public boolean initTester() {
		try {
			// fix for GitLab Runner
			File file = new File("target/log");
			String canonicalPath = file.getCanonicalPath();
			canonicalPath = canonicalPath.replace("\\", "/");
			System.setProperty("log.dir", canonicalPath);
		} catch (IOException e) {
			e.printStackTrace();
			System.setProperty("log.dir", "target/log");
		}
		debug("Starting Larva CLI...");
		System.setProperty("log.level", "INFO");
		System.setProperty("dtap.stage", "LOC");
		System.setProperty("application.server.type", "IBISTEST");
//		System.setProperty("flow.create.url", "");

		debug("Initializing environment...");
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		// remove AppConstants because it can be present from another JUnit test
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
		String projectBaseDir = Misc.getProjectBaseDir();
		appConstants.put("project.basedir", projectBaseDir);
		debug("***set property with name [project.basedir] and value [" + projectBaseDir + "]***");

		System.setProperty("jdbc.migrator.active", "true");
		// appConstants.put("validators.disabled", "true");
		// appConstants.put("xmlValidator.lazyInit", "true");
		// appConstants.put("xmlValidator.maxInitialised", "200");

		debug("Starting Ibis Context...");
		ibisContext = new IbisContext();
		debug("Initializing Ibis Context...");
		long configLoadStartTime = System.currentTimeMillis();
		ibisContext.init();

		// Wait until runstate is started
		try {
			debug("Waiting for ibis context!");
			long a = (System.currentTimeMillis() - configLoadStartTime);
			while (!ibisContext.getBootState().equals(BootState.STARTED) && a < CONTEXT_TIMEOUT) {
				Thread.sleep(100);
				a = (System.currentTimeMillis() - configLoadStartTime);
			}
			debug("Waiting for ibis context!");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(!ibisContext.getBootState().equals(BootState.STARTED)) {
			error("Ibis Context did not start in given timeout [" + CONTEXT_TIMEOUT + "]");
			System.exit(42);
		}

		debug("Got Ibis context!");
		long configLoadEndTime = System.currentTimeMillis();
		debug("***configuration loaded in [" + (configLoadEndTime - configLoadStartTime) + "] msec***");

		boolean adaptersStarted = startAllAdapters(MAX_TRIES, TIMEOUT);
		debug("Successfully initialized the environment.");

		TestTool.setIbisContext(ibisContext);
		debug("Setting up ibis context for larva.");

		return adaptersStarted;
	}

	public void closeTest() {
		if (ibisContext != null) {
			ibisContext.destroy();
		}
		debug("Larva CLI has ended.");
	}

	/**
	 * Tries to start all of the adapters registered in Ibis
	 * @return true if all the adapters have been started successfully.
	 */
	private boolean startAllAdapters(int maxTries, int timeout) {
		IbisManager ibisManager = ibisContext.getIbisManager();
		List<IAdapter> registeredAdapters = ibisManager.getRegisteredAdapters();
		// is a magic number, increasing it more will add overhead processing on operating system side for thread management
		// decreasing will cause suboptimal utilization
		ExecutorService threadPool = Executors.newFixedThreadPool(100);
		for (IAdapter adapter : registeredAdapters) {
			AdapterStarter starter = new AdapterStarter(adapter, maxTries, timeout);
			threadPool.execute(starter);
		}
		try {
			threadPool.shutdown();
			threadPool.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int started = getRunningAdapterCount();
		int all = registeredAdapters.size();
		String msg = "adapters started [" + started + "] from [" + all + "]";
		if (started == all) {
			debug(msg);
			return true;
		}
		error(msg);
		return false;
	}

	public int getRunningAdapterCount() {
		int result = 0;
		for(IAdapter adapter : ibisContext.getIbisManager().getRegisteredAdapters()) {
			result += adapter.getRunState().isState("Started") ? 1 : 0;
		}
		return result;
	}

	private static void debug(String string) {
		System.out.println(getIsoTimeStamp() + " " + getMemoryInfo() + " " + string);
	}

	private static String error(String string) {
		System.err.println(getIsoTimeStamp() + " " + getMemoryInfo() + " "
				+ string);
		return string;
	}

	private static String getIsoTimeStamp() {
		return DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
	}

	private static String getMemoryInfo() {
		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		return "[" + ProcessMetrics.normalizedNotation(totalMem - freeMem)
				+ "/" + ProcessMetrics.normalizedNotation(totalMem) + "]";
	}

	public IbisContext getIbisContext() {
		return ibisContext;
	}

	class AdapterStarter implements Runnable {
		private IAdapter adapter;
		int maxTries, timeout;
		public AdapterStarter(IAdapter adapter, int maxTries, int timeout) {
			super();
			this.adapter = adapter;
			this.maxTries = maxTries;
			this.timeout = timeout;
		}

		@Override
		public void run() {
			startAdapter();
		}

		private boolean startAdapter() {
			RunStateEnum runState = adapter.getRunState();
			int count = maxTries;
			if (!checkState(runState)) {
				debug("adapter [" + adapter.getName() + "] has state ["
						+ runState + "], will retry...");
				while (count-- > 0 && !checkState(runState)) {
					try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					runState = adapter.getRunState();
					if (!checkState(runState)) {
						debug("Adapter [" + adapter.getName() + "] has state ["
								+ runState + "], retries left [" + count + "]");
					} else {
						debug("adapter [" + adapter.getName() + "] has state ["
								+ runState + "]");
					}
				}
				if (!checkState(runState)) {
					error("Adapter [" + adapter.getName() + "] has failed to start with " + maxTries + " tries, current state ["
							+ runState + "]");
					return false;
				}
			}

			debug("Adapter [" + adapter.getName() + "] has state ["
					+ runState + "]");
			return true;
		}

		private boolean checkState(RunStateEnum runState) {
			return runState.equals(RunStateEnum.STARTED) || runState.equals(RunStateEnum.ERROR);
		}
	}
}
