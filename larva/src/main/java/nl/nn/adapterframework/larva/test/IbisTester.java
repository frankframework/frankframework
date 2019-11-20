package nl.nn.adapterframework.larva.test;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.larva.MessageListener;
import nl.nn.adapterframework.larva.TestTool;
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
import java.security.AccessControlException;
import java.util.Date;
import java.util.List;

public class IbisTester {
	private AppConstants appConstants;
	private String webAppPath;
	private IbisContext ibisContext;
	private final int maxTries = 30;
	private final int timeout = 1000;
	private static Logger logger = LogUtil.getLogger(IbisTester.class);
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
	public boolean initTester(){
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
		System.setProperty("otap.stage", "LOC");
		System.setProperty("application.server.type", "IBISTEST");
		System.setProperty("flow.create.url", "");

		debug("Initializing environment...");
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		// remove AppConstants because it can be present from another JUnit test
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
		webAppPath = getWebContentDirectory();
		String projectBaseDir = Misc.getProjectBaseDir();
		appConstants.put("project.basedir", projectBaseDir);
		debug("***set property with name [project.basedir] and value ["
				+ projectBaseDir + "]***");

		System.setProperty("jdbc.migrator.active", "true");
		// appConstants.put("validators.disabled", "true");
		// appConstants.put("xmlValidator.lazyInit", "true");
		// appConstants.put("xmlValidator.maxInitialised", "200");

		debug("Starting Ibis Context...");
		ibisContext = new IbisContext();
		debug("Initializing Ibis Context...");
		long configLoadStartTime = System.currentTimeMillis();
		ibisContext.init();
		long configLoadEndTime = System.currentTimeMillis();
		debug("***configuration loaded in ["
				+ (configLoadEndTime - configLoadStartTime) + "] msec***");

		boolean adaptersStarted = startAllAdapters(maxTries, timeout);
		debug("Successfully initialized the environment.");

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
		int adaptersStarted = 0;
		int adaptersCount = 0;
		List<IAdapter> registeredAdapters = ibisContext.getIbisManager()
				.getRegisteredAdapters();
		for (IAdapter adapter : registeredAdapters) {
			adaptersCount++;
			if(startAdapter(adapter, maxTries, timeout)) {
				adaptersStarted++;
			}

		}
		String msg = "adapters started [" + adaptersStarted + "] from ["
				+ adaptersCount + "]";
		if (adaptersCount == adaptersStarted) {
			debug(msg);
			return true;
		} else {
			error(msg);
			return false;
		}
	}

	private boolean startAdapter(IAdapter adapter, int maxTries, int timeout) {
		RunStateEnum runState = adapter.getRunState();
		int count = maxTries;
		if (!(RunStateEnum.STARTED).equals(runState)) {
			debug("adapter [" + adapter.getName() + "] has state ["
					+ runState + "], will retry...");
			while (count-- > 0 && !(RunStateEnum.STARTED).equals(runState)) {
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				runState = adapter.getRunState();
				if (!(RunStateEnum.STARTED).equals(runState)) {
					debug("Adapter [" + adapter.getName() + "] has state ["
							+ runState + "], retries left [" + count + "]");
				} else {
					debug("adapter [" + adapter.getName() + "] has state ["
							+ runState + "]");
				}
			}
			if (!(RunStateEnum.STARTED).equals(runState)) {
				error("Adapter [" + adapter.getName() + "] has failed to start with " + maxTries + " tries, current state ["
						+ runState + "]");
				return false;
			}
		}

		debug("Adapter [" + adapter.getName() + "] has state ["
				+ runState + "]");
		return true;
	}

	private static void debug(String string) {
		System.out.println(getIsoTimeStamp() + " " + getMemoryInfo() + " "
				+ string);
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

	private static String getWebContentDirectory() {
		String buildOutputDirectory = Misc.getBuildOutputDirectory();
		if (buildOutputDirectory != null
				&& buildOutputDirectory.endsWith("classes")) {
			String wcDirectory = null;
			File file = new File(buildOutputDirectory);
			while (wcDirectory == null) {
				try {
					File file2 = new File(file, "WebContent");
					if (file2.exists() && file2.isAbsolute()) {
						wcDirectory = file2.getPath();
					} else {
						file2 = new File(file, "src/main");
						if (file2.exists() && file2.isAbsolute()) {
							wcDirectory = new File(file2, "webapp").getPath();
						} else {
							file = file.getParentFile();
							if (file == null) {
								return null;
							}
						}
					}
				} catch (AccessControlException e) {
					return null;
				}
			}
			return wcDirectory;
		} else {
			return null;
		}
	}

	public IbisContext getIbisContext() {
		return ibisContext;
	}
}
