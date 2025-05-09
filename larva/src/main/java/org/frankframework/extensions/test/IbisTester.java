/*
   Copyright 2018 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.extensions.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisContext;
import org.frankframework.core.Adapter;
import org.frankframework.larva.LarvaHtmlConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.output.HtmlScenarioOutputRenderer;
import org.frankframework.larva.output.LarvaHtmlWriter;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.Misc;
import org.frankframework.util.RunState;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;

@Log4j2
public class IbisTester {
	String webAppPath;
	@Getter IbisContext ibisContext;

	private record Result(String resultString, long duration) {
	}

	private class ScenarioRunner implements Callable<String> {
		private final String scenariosRootDir;
		private final String scenario;

		public ScenarioRunner(String scenariosRootDir, String scenario) {
			this.scenariosRootDir = scenariosRootDir;
			this.scenario = scenario;
		}

		@Override
		public String call() throws Exception {
			LarvaHtmlConfig larvaConfig = new LarvaHtmlConfig();
			larvaConfig.setLogLevel(LarvaLogLevel.SCENARIO_PASSED_FAILED);
			larvaConfig.setExecute(scenario);
			if (scenariosRootDir != null) {
				larvaConfig.setActiveScenariosDirectory(scenariosRootDir);
			}
			Writer writer = new StringWriter();
			LarvaHtmlWriter larvaWriter = new LarvaHtmlWriter(larvaConfig, writer);
			TestExecutionObserver testExecutionObserver = new HtmlScenarioOutputRenderer(larvaConfig, larvaWriter);
			LarvaTool.runScenarios(ibisContext.getApplicationContext(), larvaConfig, larvaWriter, testExecutionObserver, scenario);
			if (scenario == null) {
				String htmlString = "<html><head/><body>" + writer + "</body></html>";
				try (Message message = new Message(htmlString)) {
					return XmlUtils.toXhtml(message).asString();
				}
			} else {
				return writer.toString();
			}
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

	// all called methods in doTest must be public, so they can also be called from outside

	public void initTest() {
		try {
			// fix for GitLab Runner
			File file = new File("target/log");
			String canonicalPath = file.getCanonicalPath();
			canonicalPath = canonicalPath.replace("\\", "/");
			System.setProperty("log.dir", canonicalPath);
		} catch (IOException e) {
			log.warn("Could not set log.dir property", e);
			System.setProperty("log.dir", "target/log");
		}
		System.setProperty("log.level", "INFO");
		System.setProperty("dtap.stage", "LOC");

		/*
		 * By default, the ladybug AOP config is included. This gives the following error:
		 * LinkageError: loader 'app' attempted duplicate class definition for XYZ
		 *
		 * Current default SPRING.CONFIG.LOCATIONS = spring${application.server.type}${application.server.type.custom}.xml,springIbisDebuggerAdvice.xml,springCustom.xml
		 * Overwrite so only IBISTEST is used.
		 */
		System.setProperty("SPRING.CONFIG.LOCATIONS", "springIBISTEST.xml");
		System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, "IBISTEST");
		System.setProperty("flow.create.url", "");
		debug("***start***");
		ibisContext = null;
	}

	public void closeTest() {
		if (ibisContext != null) {
			ibisContext.close();
		}
		debug("***end***");
	}

	/**
	 * returns a string containing the error, if any
	 */
	public String testStartAdapters() {
		// Log4J2 will automatically create a console appender and basic pattern layout.
		Configurator.setLevel(LogUtil.getRootLogger().getName(), Level.INFO);
		// remove AppConstants because it can be present from another JUnit test
		AppConstants.removeInstance();
		webAppPath = getWebContentDirectory();

		System.setProperty("jdbc.migrator.active", "true");
		// appConstants.put("validators.disabled", "true");
		// appConstants.put("xmlValidator.lazyInit", "true");
		// appConstants.put("xmlValidator.maxInitialised", "200");

		ibisContext = new IbisContext();
		long configLoadStartTime = System.currentTimeMillis();
		ibisContext.init(false); // Creates, configures and starts the necessary configurations
		long configLoadEndTime = System.currentTimeMillis();
		debug("***configuration loaded in ["+ (configLoadEndTime - configLoadStartTime) + "] msec***");

		List<Configuration> configurations = ibisContext.getIbisManager().getConfigurations();
		List<Adapter> adapters = new ArrayList<>();
		for(Configuration configuration : configurations) {
			if(configuration.getConfigurationException() != null) {
				error("error loading configuration ["+configuration.getName()+"]: "+ configuration.getConfigurationException().getMessage());
			} else {
				adapters.addAll(configuration.getRegisteredAdapters());
				debug("loading configuration ["+configuration.getName()+"] with ["+configuration.getRegisteredAdapters().size()+"] adapters");
			}
		}

		debug("***starting adapters***");
		int adaptersStarted = 0;
		int adaptersCount = 0;
		for (Adapter adapter: adapters) {
			adaptersCount++;
			RunState runState = adapter.getRunState();
			if ((RunState.STARTED) != runState) {
				debug("adapter [" + adapter.getName() + "] has state [" + runState + "], will retry...");
				int count = 30;
				while (count-- > 0 && (RunState.STARTED) != runState) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					runState = adapter.getRunState();
					if ((RunState.STARTED) != runState) {
						debug("adapter [" + adapter.getName() + "] has state [" + runState + "], retries left [" + count + "]");
					} else {
						debug("adapter [" + adapter.getName() + "] has state [" + runState + "]");
					}
				}
			} else {
				debug("adapter [" + adapter.getName() + "] has state [" + runState + "]");
			}
			if ((RunState.STARTED) == runState) {
				adaptersStarted++;
			} else {
				error("adapter [" + adapter.getName() + "] has state [" + runState + "]");
			}
		}

		String msg = "adapters started [" + adaptersStarted + "] from [" + adaptersCount + "]";

		if (adaptersCount == adaptersStarted) {
			debug(msg);
			return null; // null == good
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
			log.warn("Error running larva", e);
			result = null;
		}

		if (result == null) {
			return error("First call to get scenarios failed");
		} else {
			Double countScenariosRootDirs = evaluateXPathNumber(result.resultString, "count(html/body//select[@name='scenariosrootdirectory']/option)");
			if (countScenariosRootDirs == null || countScenariosRootDirs == 0) {
				return error("No scenarios root directories found");
			}

			Collection<String> scenariosRootDirsUnselected = evaluateXPath(result.resultString, "(html/body//select[@name='scenariosrootdirectory'])[1]/option[not(@selected)]/@value");

			String runScenariosResult = runScenarios(result.resultString);
			if (runScenariosResult!=null) {
				return runScenariosResult;
			}
			if (!scenariosRootDirsUnselected.isEmpty()) {
				for (String scenariosRootDirUnselected : scenariosRootDirsUnselected) {
					try {
						result = runScenario(scenariosRootDirUnselected, null,
								null);
					} catch (Exception e) {
						log.warn("Error running larva", e);
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
		if (scenarios.isEmpty()) {
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
					result = runScenario(scenariosRootDir, scenario, scenarioInfo);
				} catch (Exception e) {
					log.warn("Error running scenario {}", scenarioShortName, e);
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

	private static @Nonnull String getIsoTimeStamp() {
		return DateFormatUtils.now();
	}

	private static @Nonnull String getMemoryInfo() {
		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		return "[" + Misc.toFileSize(totalMem - freeMem) + "/" + Misc.toFileSize(totalMem) + "]";
	}

	private static @Nullable String evaluateXPathFirst(String xhtml, String xpath) {
		try {
			return XmlUtils.evaluateXPathNodeSetFirstElement(xhtml, xpath);
		} catch (Exception e) {
			log.warn("Error evaluating xpath [{}]", xpath, e);
			return null;
		}
	}

	private static @Nonnull Collection<String> evaluateXPath(String xhtml, String xpath) {
		try {
			return XmlUtils.evaluateXPathNodeSet(xhtml, xpath);
		} catch (Exception e) {
			log.warn("Error evaluating xpath [{}]", xpath, e);
			return Collections.emptyList();
		}
	}

	private static @Nullable Double evaluateXPathNumber(String xhtml, String xpath) {
		try {
			return XmlUtils.evaluateXPathNumber(xhtml, xpath);
		} catch (Exception e) {
			log.warn("Error evaluating xpath [{}]", xpath, e);
			return null;
		}
	}

	private static String getWebContentDirectory() {
		String buildOutputDirectory = getBuildOutputDirectory();
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
				} catch (SecurityException e) {
					error(e.getMessage());
					return null;
				}
			}
			return wcDirectory;
		} else {
			return null;
		}
	}

	private static String getBuildOutputDirectory() {
		// TODO: Warning from Sonarlint of Potential NPE?
		String path = new File(AppConstants.class.getClassLoader().getResource("").getPath()).getPath();

		try {
			return URLDecoder.decode(path, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		} catch (UnsupportedEncodingException e) {
			log.warn("unable to parse build-output-directory using charset [{}]", StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, e);
			return null;
		}
	}

}
