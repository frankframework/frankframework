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

/*
TODO: Include old Larva project, refactor this one
TODO: Get rid of all the absolute paths, pretty much :P
TODO: Create a profile to test ibis itself.
 */
public class IbisTester {
	private AppConstants appConstants;
	private String webAppPath;
	private IbisContext ibisContext;
	//MockServletContext application;
	private final int maxTries = 30;
	private final int timeout = 1000;
	private static Logger logger = LogUtil.getLogger(IbisTester.class);
	public static void main(String[] args) throws IllegalArgumentException, IOException, URISyntaxException {
		System.out.println("Here are the given! " + args.length );
		for (String arg : args) {
			System.out.println(arg);
		}
		if(args.length != 7) {
			throw new IllegalArgumentException("Given argument size does not match the expected size! " +
					"The expected arguments are as follows:\n" +
					"'[Current Path]' '[Execute Path]' '[Root Directories Path]' '[Number of threads]' '[Timeout]' '[Wait Before Cleanup]' '[Output File]'");
		}
		// Parse arguments
		File current = new File(args[0].replace("'", ""));
		if(current.isDirectory())
			System.out.println("yep");
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
//
//	private class Result {
//		private String resultString;
//		private long duration;
//
//		public Result(String resultString, long duration) {
//			this.resultString = resultString;
//			this.duration = duration;
//		}
//	}
//
//	private class ScenarioRunner implements Callable<String> {
//		private String scenariosRootDir;
//		private String scenario;
//
//		public ScenarioRunner(String scenariosRootDir, String scenario) {
//			this.scenariosRootDir = scenariosRootDir;
//			this.scenario = scenario;
//		}
//
//		public String call() throws Exception {
//			MockHttpServletRequest request = new MockHttpServletRequest();
//			request.setServletPath("/larva/index.jsp");
//			boolean silent;
//			if (scenario == null) {
//				String ibisContextKey = appConstants
//						.getResolvedProperty(ConfigurationServlet.KEY_CONTEXT);
//				application = new MockServletContext("file:" + webAppPath, null);
//				application.setAttribute(ibisContextKey, ibisContext);
//				silent = false;
//			} else {
//				request.setParameter("loglevel", "scenario passed/failed");
//				request.setParameter("execute", scenario);
//				silent = true;
//			}
//			if (scenariosRootDir != null) {
//				request.setParameter("scenariosrootdirectory", scenariosRootDir);
//			}
//			Writer writer = new StringWriter();
//			runScenarios(application, request, writer, silent);
//			if (scenario == null) {
//				String htmlString = "<html><head/><body>" + writer.toString()
//						+ "</body></html>";
//				return XmlUtils.toXhtml(htmlString);
//			} else {
//				return writer.toString();
//			}
//		}
//
//		public void runScenarios(ServletContext application,
//								 HttpServletRequest request, Writer out, boolean silent)
//				throws IllegalArgumentException, SecurityException,
//				IllegalAccessException, InvocationTargetException,
//				NoSuchMethodException, ClassNotFoundException {
//
//			Class<?>[] args_types = new Class<?>[4];
//			args_types[0] = ServletContext.class;
//			args_types[1] = HttpServletRequest.class;
//			args_types[2] = Writer.class;
//			args_types[3] = boolean.class;
//			Object[] args = new Object[4];
//			args[0] = application;
//			args[1] = request;
//			args[2] = out;
//			args[3] = silent;
//			Class.forName("nl.nn.adapterframework.testtool.TestTool")
//					.getMethod("runScenarios", args_types).invoke(null, args);
//		}
//	}

//	public String doTest() {
//		try {
//			String result;
//			if (result==null) {
//				result = testLarva();
//			}
//			return result;
//		} finally {
//			closeTest();
//		}
//	}

	// all called methods in doTest must be public so they can also be called
	// from outside

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
//
//	public Map<String, String> getRootDirectories(boolean forceReread) {
//		String realPath = AppConstants.getInstance().getResolvedProperty("webapp.realpath") + "larva/";
//		if(forceReread || TestPreparer.scenariosRootDirectories == null)
//			TestPreparer.initScenariosRootDirectories(realPath, null, appConstants);
//
//		return (Map<String, String>) ((HashMap)TestPreparer.scenariosRootDirectories).clone();
//	}

//
//	public String testLarva() {
//		// Start testing larva
//		debug("***start larva***");
//		Result result;
//		// Get scenarios root directories
//		try {
//			result = runScenario(null, null, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//			result = null;
//		}
//
//		if (result == null) {
//			return error("First call to get scenarios failed");
//		} else {
//
//			Double countScenariosRootDirs = evaluateXPathNumber(
//					result.resultString,
//					"count(html/body//select[@name='scenariosrootdirectory']/option)");
//			if (countScenariosRootDirs == 0) {
//				return error("No scenarios root directories found");
//			}
//
//			Collection<String> scenariosRootDirsUnselected = evaluateXPath(
//					result.resultString,
//					"(html/body//select[@name='scenariosrootdirectory'])[1]/option[not(@selected)]/@value");
//
//			String runScenariosResult = runScenarios(result.resultString);
//			if (runScenariosResult!=null) {
//				return runScenariosResult;
//			}
//			if (scenariosRootDirsUnselected != null
//					&& scenariosRootDirsUnselected.size() > 0) {
//				for (String scenariosRootDirUnselected : scenariosRootDirsUnselected) {
//					try {
//						result = runScenario(scenariosRootDirUnselected, null,
//								null);
//					} catch (Exception e) {
//						e.printStackTrace();
//						result = null;
//					}
//
//					if (result == null) {
//						return error("Call to get scenarios from ["
//								+ scenariosRootDirUnselected + "] failed");
//					}
//
//					runScenariosResult = runScenarios(result.resultString);
//					if (runScenariosResult!=null) {
//						return runScenariosResult;
//					}
//				}
//			}
//		}
//		return null;
//	}
//
//	private String runScenarios(String xhtml) {
//		Collection<String> scenarios = evaluateXPath(
//				xhtml,
//				"(html/body//select[@name='execute'])[1]/option/@value[ends-with(.,'.properties')]");
//		if (scenarios == null || scenarios.size() == 0) {
//			return error("No scenarios found");
//		} else {
//			String scenariosRootDir = evaluateXPathFirst(
//					xhtml,
//					"(html/body//select[@name='scenariosrootdirectory'])[1]/option[@selected]/@value");
//			String scenariosRoot = evaluateXPathFirst(xhtml,
//					"(html/body//select[@name='scenariosrootdirectory'])[1]/option[@selected]");
//			debug("Found " + scenarios.size() + " scenario(s) in root ["
//					+ scenariosRoot + "]");
//			int scenariosTotal = scenarios.size();
//			int scenariosPassed = 0;
//			int scenariosCount = 0;
//			Result result;
//			for (String scenario : scenarios) {
//				scenariosCount++;
//
//				String scenarioShortName;
//				if (StringUtils.isNotEmpty(scenario)
//						&& StringUtils.isNotEmpty(scenariosRootDir)) {
//					if (scenario.startsWith(scenariosRootDir)) {
//						scenarioShortName = scenario.substring(scenariosRootDir
//								.length());
//					} else {
//						scenarioShortName = scenario;
//					}
//				} else {
//					scenarioShortName = scenario;
//				}
//				String scenarioInfo = "scenario [" + scenariosCount + "/"
//						+ scenariosTotal + "] [" + scenarioShortName + "]";
//
//				try {
//					result = runScenario(scenariosRootDir, scenario,
//							scenarioInfo);
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					result = null;
//				}
//
//				if (result == null) {
//					error(scenarioInfo + " failed");
//				} else {
//					if (result.resultString != null
//							&& result.resultString.contains("passed")
//					) {
//						debug(scenarioInfo + " passed in [" + result.duration
//								+ "] msec");
//						scenariosPassed++;
//					} else {
//						error(scenarioInfo + " failed in [" + result.duration
//								+ "] msec");
//						error(result.resultString);
//					}
//				}
//			}
//			String msg = "scenarios passed [" + scenariosPassed + "] from ["
//					+ scenariosCount + "]";
//
//			if (scenariosCount == scenariosPassed) {
//				debug(msg);
//			} else {
//				return error(msg);
//			}
//		}
//		return null;
//	}
//
//	private Result runScenario(String scenariosRootDir, String scenario, String scenarioInfo) {
//		int count = 2;
//		String resultString = null;
//		long startTime = 0;
//		while (count-- > 0 && resultString == null) {
//			startTime = System.currentTimeMillis();
//			ScenarioRunner scenarioRunner = new ScenarioRunner(
//					scenariosRootDir, scenario);
//			ExecutorService service = Executors.newSingleThreadExecutor();
//			Future future = service.submit(scenarioRunner);
//			long timeout = 60;
//			try {
//				try {
//					resultString = (String) future.get(timeout,
//							TimeUnit.SECONDS);
//				} catch (TimeoutException e) {
//					debug(scenarioInfo + " timed out, retries left [" + count
//							+ "]");
//				} catch (Exception e) {
//					debug(scenarioInfo + " got error, retries left [" + count
//							+ "]");
//				}
//			} finally {
//				service.shutdown();
//			}
//		}
//
//		long endTime = System.currentTimeMillis();
//		return new Result(resultString, endTime - startTime);
//	}
//
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

//	private static String evaluateXPathFirst(String xhtml, String xpath) {
//		try {
//			return XmlUtils
//					.evaluateXPathNodeSetFirstElement(xhtml, xpath);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	private static Collection<String> evaluateXPath(String xhtml, String xpath) {
//		try {
//			return XmlUtils.evaluateXPathNodeSet(xhtml, xpath);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	private static Double evaluateXPathNumber(String xhtml, String xpath) {
//		try {
//			return XmlUtils.evaluateXPathNumber(xhtml, xpath);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//	}

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
