package nl.nn.adapterframework.extensions.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessMetrics;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlUtils;

public class IbisTester {
	private AppConstants appConstants;
	String webAppPath;
	IbisContext ibisContext;
	MockServletContext application;

	private class Result {
		private String resultString;
		private long duration;

		public Result(String resultString, long duration) {
			this.resultString = resultString;
			this.duration = duration;
		}
	}

	private class ScenarioRunner implements Callable<String> {
		private String scenariosRootDir;
		private String scenario;

		public ScenarioRunner(String scenariosRootDir, String scenario) {
			this.scenariosRootDir = scenariosRootDir;
			this.scenario = scenario;
		}

		@Override
		public String call() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setServletPath("/larva/index.jsp");
			boolean silent;
			if (scenario == null) {
				String ibisContextKey = appConstants.getResolvedProperty(IbisApplicationServlet.KEY_CONTEXT);
				application = new MockServletContext("file:" + webAppPath, null);
				application.setAttribute(ibisContextKey, ibisContext);
				silent = false;
			} else {
				request.setParameter("loglevel", "scenario passed/failed");
				request.setParameter("execute", scenario);
				silent = true;
			}
			if (scenariosRootDir != null) {
				request.setParameter("scenariosrootdirectory", scenariosRootDir);
			}
			Writer writer = new StringWriter();
			runScenarios(application, request, writer, silent);
			if (scenario == null) {
				String htmlString = "<html><head/><body>" + writer.toString() + "</body></html>";
				return XmlUtils.toXhtml(htmlString);
			} else {
				return writer.toString();
			}
		}

		public void runScenarios(ServletContext application,
				HttpServletRequest request, Writer out, boolean silent)
				throws IllegalArgumentException, SecurityException,
				IllegalAccessException, InvocationTargetException,
				NoSuchMethodException, ClassNotFoundException {

			Class<?>[] args_types = new Class<?>[4];
			args_types[0] = ServletContext.class;
			args_types[1] = HttpServletRequest.class;
			args_types[2] = Writer.class;
			args_types[3] = boolean.class;
			Object[] args = new Object[4];
			args[0] = application;
			args[1] = request;
			args[2] = out;
			args[3] = silent;
			Class.forName("nl.nn.adapterframework.testtool.TestTool").getMethod("runScenarios", args_types).invoke(null, args);
		}
	}

	public String doTest() {
		initTest();
		try {
			String result = testStartAdapters();
			if (result==null) {
				result = testLarva();
			}
			return result;
		} finally {
			closeTest();
		}
	}

	// all called methods in doTest must be public so they can also be called
	// from outside

	public void initTest() {
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
		System.setProperty("log.level", "INFO");
		System.setProperty("dtap.stage", "LOC");
		System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, "IBISTEST");
		System.setProperty("flow.create.url", "");
		debug("***start***");
		ibisContext = null;
	}

	public void closeTest() {
		if (ibisContext != null) {
			ibisContext.destroy();
		}
		debug("***end***");
	}

	public String testStartAdapters() {
		// Log4J2 will automatically create a console appender and basic pattern layout.
		Configurator.setLevel(LogUtil.getRootLogger().getName(), Level.INFO);
		// remove AppConstants because it can be present from another JUnit test
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
		webAppPath = getWebContentDirectory();
		String projectBaseDir = Misc.getProjectBaseDir();
		appConstants.put("project.basedir", projectBaseDir);
		debug("***set property with name [project.basedir] and value [" + projectBaseDir + "]***");

		System.setProperty("jdbc.migrator.active", "true");
		// appConstants.put("validators.disabled", "true");
		// appConstants.put("xmlValidator.lazyInit", "true");
		// appConstants.put("xmlValidator.maxInitialised", "200");

		ibisContext = new IbisContext();
		long configLoadStartTime = System.currentTimeMillis();
		ibisContext.init(false);
		long configLoadEndTime = System.currentTimeMillis();
		debug("***configuration loaded in ["+ (configLoadEndTime - configLoadStartTime) + "] msec***");

		int adaptersStarted = 0;
		int adaptersCount = 0;
		List<IAdapter> registeredAdapters = ibisContext.getIbisManager().getRegisteredAdapters();
		for (IAdapter adapter : registeredAdapters) {
			adaptersCount++;
			RunStateEnum runState = adapter.getRunState();
			if (!(RunStateEnum.STARTED).equals(runState)) {
				debug("adapter [" + adapter.getName() + "] has state [" + runState + "], will retry...");
				int count = 30;
				while (count-- > 0 && !(RunStateEnum.STARTED).equals(runState)) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					runState = adapter.getRunState();
					if (!(RunStateEnum.STARTED).equals(runState)) {
						debug("adapter [" + adapter.getName() + "] has state [" + runState + "], retries left [" + count + "]");
					} else {
						debug("adapter [" + adapter.getName() + "] has state [" + runState + "]");
					}
				}
			} else {
				debug("adapter [" + adapter.getName() + "] has state [" + runState + "]");
			}
			if ((RunStateEnum.STARTED).equals(runState)) {
				adaptersStarted++;
			} else {
				error("adapter [" + adapter.getName() + "] has state [" + runState + "]");
			}
		}
		String msg = "adapters started [" + adaptersStarted + "] from [" + adaptersCount + "]";
		if (adaptersCount == adaptersStarted) {
			debug(msg);
			return null;
		} else {
			return error(msg);
		}
	}

	public String testLarva() {
		debug("***start larva***");
		Result result;
		try {
			result = runScenario(null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
		}

		if (result == null) {
			return error("First call to get scenarios failed");
		} else {
			Double countScenariosRootDirs = evaluateXPathNumber(result.resultString, "count(html/body//select[@name='scenariosrootdirectory']/option)");
			if (countScenariosRootDirs == 0) {
				return error("No scenarios root directories found");
			}

			Collection<String> scenariosRootDirsUnselected = evaluateXPath(result.resultString, "(html/body//select[@name='scenariosrootdirectory'])[1]/option[not(@selected)]/@value");

			String runScenariosResult = runScenarios(result.resultString);
			if (runScenariosResult!=null) {
				return runScenariosResult;
			}
			if (scenariosRootDirsUnselected != null
					&& scenariosRootDirsUnselected.size() > 0) {
				for (String scenariosRootDirUnselected : scenariosRootDirsUnselected) {
					try {
						result = runScenario(scenariosRootDirUnselected, null,
								null);
					} catch (Exception e) {
						e.printStackTrace();
						result = null;
					}

					if (result == null) {
						return error("Call to get scenarios from [" + scenariosRootDirUnselected + "] failed");
					}

					runScenariosResult = runScenarios(result.resultString);
					if (runScenariosResult!=null) {
						return runScenariosResult;
					}
				}
			}
		}
		return null;
	}

	private String runScenarios(String xhtml) {
		Collection<String> scenarios = evaluateXPath(
				xhtml,
				"(html/body//select[@name='execute'])[1]/option/@value[ends-with(.,'.properties')]");
		if (scenarios == null || scenarios.size() == 0) {
			return error("No scenarios found");
		} else {
			String scenariosRootDir = evaluateXPathFirst(
					xhtml,
					"(html/body//select[@name='scenariosrootdirectory'])[1]/option[@selected]/@value");
			String scenariosRoot = evaluateXPathFirst(xhtml,
					"(html/body//select[@name='scenariosrootdirectory'])[1]/option[@selected]");
			debug("Found " + scenarios.size() + " scenario(s) in root ["
					+ scenariosRoot + "]");
			int scenariosTotal = scenarios.size();
			int scenariosPassed = 0;
			int scenariosCount = 0;
			Result result;
			for (String scenario : scenarios) {
				scenariosCount++;

				String scenarioShortName;
				if (StringUtils.isNotEmpty(scenario)
						&& StringUtils.isNotEmpty(scenariosRootDir)) {
					if (scenario.startsWith(scenariosRootDir)) {
						scenarioShortName = scenario.substring(scenariosRootDir
								.length());
					} else {
						scenarioShortName = scenario;
					}
				} else {
					scenarioShortName = scenario;
				}
				String scenarioInfo = "scenario [" + scenariosCount + "/"
						+ scenariosTotal + "] [" + scenarioShortName + "]";

				try {
					result = runScenario(scenariosRootDir, scenario,
							scenarioInfo);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					result = null;
				}

				if (result == null) {
					error(scenarioInfo + " failed");
				} else {
					if (result.resultString != null
						&& result.resultString.contains("passed")
					) {
						debug(scenarioInfo + " passed in [" + result.duration + "] msec");
						scenariosPassed++;
					} else {
						error(scenarioInfo + " failed in [" + result.duration + "] msec");
						error(result.resultString);
					}
				}
			}
			String msg = "scenarios passed [" + scenariosPassed + "] from [" + scenariosCount + "]";

			if (scenariosCount == scenariosPassed) {
				debug(msg);
			} else {
				return error(msg);
			}
		}
		return null;
	}

	private Result runScenario(String scenariosRootDir, String scenario,
			String scenarioInfo) {
		int count = 2;
		String resultString = null;
		long startTime = 0;
		while (count-- > 0 && resultString == null) {
			startTime = System.currentTimeMillis();
			ScenarioRunner scenarioRunner = new ScenarioRunner(
					scenariosRootDir, scenario);
			ExecutorService service = Executors.newSingleThreadExecutor();
			Future<String> future = service.submit(scenarioRunner);
			long timeout = 60;
			try {
				try {
					resultString = future.get(timeout, TimeUnit.SECONDS);
				} catch (TimeoutException e) {
					debug(scenarioInfo + " timed out, retries left [" + count + "]");
				} catch (Exception e) {
					e.printStackTrace();
					debug(scenarioInfo + " got error, retries left [" + count + "]");
				}
			} finally {
				service.shutdown();
			}
		}

		long endTime = System.currentTimeMillis();
		return new Result(resultString, endTime - startTime);
	}

	private static void debug(String string) {
		System.out.println(getIsoTimeStamp() + " " + getMemoryInfo() + " " + string);
	}

	private static String error(String string) {
		System.err.println(getIsoTimeStamp() + " " + getMemoryInfo() + " " + string);
		return string;
	}

	private static String getIsoTimeStamp() {
		return DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
	}

	private static String getMemoryInfo() {
		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		return "[" + ProcessMetrics.normalizedNotation(totalMem - freeMem) + "/" + ProcessMetrics.normalizedNotation(totalMem) + "]";
	}

	private static String evaluateXPathFirst(String xhtml, String xpath) {
		try {
			return XmlUtils.evaluateXPathNodeSetFirstElement(xhtml, xpath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private static Collection<String> evaluateXPath(String xhtml, String xpath) {
		try {
			return XmlUtils.evaluateXPathNodeSet(xhtml, xpath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private static Double evaluateXPathNumber(String xhtml, String xpath) {
		try {
			return XmlUtils.evaluateXPathNumber(xhtml, xpath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private static String getWebContentDirectory() {
		String buildOutputDirectory = Misc.getBuildOutputDirectory();
		if (buildOutputDirectory != null && buildOutputDirectory.endsWith("classes")) {
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
					error(e.getMessage());
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
