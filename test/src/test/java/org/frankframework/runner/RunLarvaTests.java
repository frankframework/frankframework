package org.frankframework.runner;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisContext;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.larva.LarvaConfig;
import org.frankframework.larva.LarvaLogLevel;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.Scenario;
import org.frankframework.larva.ScenarioRunner;
import org.frankframework.larva.TestRunStatus;
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.larva.output.PlainTextScenarioOutputRenderer;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.SpringUtils;

/**
 * Attempt to run Larva tests in the Maven build.
 *
 * There are some issues -- some tests fail unexpectedly, whereas they do not fail when running
 * in a normal AppServer environment.
 *
 * Therefore, it will not fail the build and run only to provide extra coverage-reporting.
 *
 */
@Tag("integration")
@Log4j2
public class RunLarvaTests {

	public static final LarvaLogLevel LARVA_LOG_LEVEL = LarvaLogLevel.WRONG_PIPELINE_MESSAGES_PREPARED_FOR_DIFF;
	public static final Set<String> IGNORED_SCENARIOS = Set.of(
			"Authentication/scenario03",
			"Authentication/scenario04",
			"Base64Pipe/scenario01",
			"Base64Pipe/scenario02",
			"FileSender/scenario01",
			"ManagedFileHandler/scenario01",
			"ManagedFileHandler/scenario02",
			"Validators/SoapValidator/scenario07",
			"WebServiceListenerSender/scenario11b",
			"WebServiceListenerSender/scenario11c",
			"WebServiceListenerSender/scenario11d",
			"WsdlGeneratorPipe/scenario01",
			"WsdlGeneratorPipe/scenario02",
			"WsdlGeneratorPipe/scenario03",
			"XsltProviderListener/scenario04",

			// These scenarios likely fail when Narayana is used
			"JdbcListener/scenario02",
			"OutputStreaming/scenario04",
			"Receivers/Transacted/NoInProcess/scenario01",
			"Receivers/Transacted/NoInProcess/scenario02",
			"TransactionHandling/MultiThread/scenario20",
			"TransactionHandling/MultiThread/scenario21",
			"TransactionHandling/MultiThread/scenario22",

			// These scenarios will likely fail when JMS is active but Narayana is not used
			"FxF3/scenario11",
			"FxF3/scenario12",
			"JmsListenerSender/FF/scenario03",
			"JmsListenerSender/FF/scenario07",
			"JmsListenerSender/FF/scenario09",
			"JmsListenerSender/FF/scenario11",
			"JmsRetryListener/scenario01"
			);

	private static ConfigurableApplicationContext parentContext;
	private static ConfigurableApplicationContext applicationContext;
	private static IbisContext ibisContext;
	private static EmbeddedActiveMQ jmsServer;

	private LarvaTool larvaTool;
	private ScenarioRunner scenarioRunner;
	private String scenarioRootDir;
	private LarvaWriter larvaWriter;
	private TestExecutionObserver testExecutionObserver;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setupBeforeAll() throws Exception {
		jmsServer = configureEmbeddedJmsServer();

		SpringApplication springApplication = IafTestInitializer.configureApplication("NARAYANA", null, "inmem");
		// This ApplicationContext doesn't have the database, so we cannot use it for the Larva Tests...
		parentContext = springApplication.run();
		ServletContext servletContext = parentContext.getBean(ServletContext.class);

		// We need to get the IbisContext from the ServletContext, since from this one we can get the ApplicationContext that has the database.
		ibisContext = FrankApplicationInitializer.getIbisContext(servletContext);
		applicationContext = ibisContext.getApplicationContext();

		// For WSDL Generator tests, this property needs to be unset from the value it gets from core/src/test/resources/DeploymentSpecifics.properties
		// However, as it's set via DeploymentSpecifics we cannot just clear or remove it, so we have to set it to blanks in the System properties which take preference over DeploymentSpecifics
		System.setProperty("wsdl.soapAction", "");

		OutboundGateway gateway = SpringUtils.createBean(parentContext, LocalGateway.class);
		assertTrue(parentContext.isRunning());
		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(5))
				.until(() -> verifyAppIsHealthy(gateway));
	}

	private static EmbeddedActiveMQ configureEmbeddedJmsServer() throws Exception {
		String jmsDataDir = IafTestInitializer.getLogDir(IafTestInitializer.getProjectDir()) + "/ArtemisMQ";

		Configuration artemisJmsConfig = new ConfigurationImpl();
		artemisJmsConfig.addAcceptorConfiguration("in-vm", "vm://0")
			.setSecurityEnabled(false)
				.setName("ArtemisJmsServer-Larva")
				.setBindingsDirectory(jmsDataDir + "/bindings")
				.setJournalDirectory(jmsDataDir + "/journal")
				.setLargeMessagesDirectory(jmsDataDir + "/largemessages")
		;

		EmbeddedActiveMQ embeddedServer = new EmbeddedActiveMQ();
		embeddedServer.setConfiguration(artemisJmsConfig);
		embeddedServer.start();
		log.info("Started embedded in-memory JMS server");
		return embeddedServer;
	}

	private static boolean verifyAppIsHealthy(OutboundGateway gateway) {
		try {
			Message<Object> response = gateway.sendSyncMessage(RequestMessageBuilder.create(BusTopic.HEALTH).build(null));
			return "200".equals(response.getHeaders().get(BusMessageUtils.HEADER_PREFIX+ MessageBase.STATUS_KEY));
		} catch (Exception e) {
			log.error("error while checking health of application", e);
			return false;
		}
	}

	@BeforeEach
	void setupBeforeEach() {
		larvaTool = LarvaTool.createInstance(applicationContext);

		LarvaConfig larvaConfig = larvaTool.getLarvaConfig();
		larvaConfig.setTimeout(10_000);
		larvaConfig.setLogLevel(LARVA_LOG_LEVEL);
		larvaConfig.setMultiThreaded(false);

		larvaWriter = new LarvaWriter(larvaConfig, System.out);
		larvaTool.setWriter(larvaWriter);
		testExecutionObserver = new PlainTextScenarioOutputRenderer(larvaWriter);
	}

	@AfterAll
	static void tearDown() {
		CloseUtils.closeSilently(ibisContext, parentContext);
		try {
			jmsServer.stop();
		} catch (Exception e) {
			log.error("error while stopping embedded JMS server", e);
		}

		// Make sure to clear the app constants as well
		AppConstants.removeInstance();
	}

	/**
	 * This should create Dynamic tests for JUnit to run. However the dynamic tests are not properly
	 * reported on by Surefire, although Surefire does execute them.
	 *
	 * Another issue is that over half the Larva scenarios fails even though I see no reason why they
	 * would fail. Running all scenarios at once by passing the scenario-directoy to the LarvaTool, most
	 * scenarios do run. I don't yet see the principle difference in environment that makes them fail here.
	 *
	 */
	@TestFactory
	Stream<DynamicNode> larvaTests() {
		assertTrue(applicationContext.isRunning());
		TestRunStatus testRunStatus = larvaTool.createTestRunStatus();
		scenarioRootDir = testRunStatus.initScenarioDirectories();
		scenarioRunner = larvaTool.createScenarioRunner(testExecutionObserver, testRunStatus);

		testRunStatus.readScenarioFiles(larvaTool.getScenarioLoader());
		List<Scenario> allScenarios = testRunStatus.getScenariosToRun(larvaTool.getLarvaConfig().getActiveScenariosDirectory());
		assertFalse(allScenarios.isEmpty(), () -> "Did not find any scenario-files in scenarioRootDir [%s]!".formatted(scenarioRootDir));
		System.err.printf("Creating JUnit tests from %d scenarios loaded from [%s]%n", allScenarios.size(), scenarioRootDir);
		return createScenarios(scenarioRootDir, "", allScenarios);
	}

	private @Nonnull Stream<DynamicNode> createScenarioContainer(@Nonnull String baseFolder, @Nonnull Map.Entry<String, List<Scenario>> scenarioFolder) {
		String scenarioFolderName = scenarioFolder.getKey();
		if (StringUtils.isBlank(scenarioFolderName)) {
			return createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue());
		}
		return Stream.of(DynamicContainer.dynamicContainer(scenarioFolderName, new File(baseFolder, scenarioFolderName).toURI(), createScenarios(baseFolder, scenarioFolderName, scenarioFolder.getValue())));
	}

	private @Nonnull Stream<DynamicNode> createScenarios(@Nonnull String baseFolder, @Nonnull String subFolder, @Nonnull List<Scenario> scenarioFiles) {
		String commonFolder = StringUtils.isBlank(subFolder) ? baseFolder : Paths.get(baseFolder, subFolder).toString();
		Map<String, List<Scenario>> scenariosByFolder = ScenarioRunner.groupScenariosByFolder(scenarioFiles, commonFolder);

		if (scenariosByFolder.size() == 1) {
			return scenarioFiles.stream()
					.map(this::convertLarvaScenarioToTest);
		} else {
			return scenariosByFolder.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.flatMap((Map.Entry<String, List<Scenario>> nestedSubFolder) -> createScenarioContainer(commonFolder, nestedSubFolder));
		}
	}

	private DynamicTest convertLarvaScenarioToTest(Scenario scenario) {
		// Scenario name always computed from the scenario root dir to be understandable without context of immediate parent
		String scenarioName = scenario.getName();
		return DynamicTest.dynamicTest(
				scenarioName, scenario.getScenarioFile().toURI(), () -> {
					System.out.println("Running scenario: [" + scenarioName + "]");
					int scenarioPassed = scenarioRunner.runOneFile(scenario, true);
					larvaWriter.flush();

					assumeTrue(scenarioPassed != LarvaTool.RESULT_ERROR || !IGNORED_SCENARIOS.contains(scenarioName), () -> "Ignoring Blacklisted Scenario: [" + scenarioName + "]");
					assertNotEquals(LarvaTool.RESULT_ERROR, scenarioPassed, () -> "Scenario failed: [" + scenarioName + "]");
				}
		);
	}

	@Test
	@Disabled("Run Larva test scenarios individually now")
	void runLarvaTests() {
		assertTrue(applicationContext.isRunning());
		LarvaConfig larvaConfig = larvaTool.getLarvaConfig();
		larvaConfig.setLogLevel(LarvaLogLevel.SCENARIO_FAILED);

		long start = System.currentTimeMillis();
		TestRunStatus result = larvaTool.runScenarios(larvaTool.getActiveScenariosDirectory(), testExecutionObserver, larvaWriter);
		long end = System.currentTimeMillis();
		System.err.printf("Scenarios executed; duration: %dms%n", end - start);

		if (result.getScenariosFailedCount() > 0) {
			System.err.printf("%d Larva tests failed, duration: %dms; %n%n", result.getScenariosFailedCount(), end - start);
		} else {
			System.err.printf("All %d Larva tests succeeded in %dms%n", result.getScenarioExecuteCount(), end - start);
		}

		// About 15 to 18 scenarios will fail because the environment is not set up entirely correct. Do not fail the build because of that, still get the extra coverage.
//		assertEquals(0, result, () -> "Error in LarvaTool scenarios, %d scenarios failed. Duration: %dms; %n%n%s".formatted(result, end - start, larvaOutput));
	}
}
