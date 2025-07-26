/*
   Copyright 2025 WeAreFrank!

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

package org.frankframework.runner;

import static org.awaitility.Awaitility.await;
import static org.frankframework.testutil.TestAssertions.isTestRunningOnGitHub;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.wimdeblauwe.testcontainers.cypress.CypressContainer;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestResults;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestSuite;
import lombok.extern.log4j.Log4j2;

import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

/**
 * Runs e2e tests with Cypress in a Testcontainer.
 * Requires Docker, else the test will be skipped.
 * <p>
 * Exclude with '-DexcludedGroups=integration'
 *
 * @author Sergi Philipsen
 * @see "https://github.com/wimdeblauwe/testcontainers-cypress"
 */
@Log4j2
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
public class RunCypressE2eTest {
	private static CypressContainer container;
	private static ConfigurableApplicationContext run;
	private static final Logger CYPRESS_LOG = LogUtil.getLogger("cypress");

	@BeforeAll
	public static void setUp() throws IOException {
		startIafTestInitializer();
		startTestContainer();
	}

	private static void startIafTestInitializer() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();

		run = springApplication.run();
		OutboundGateway gateway = SpringUtils.createBean(run, LocalGateway.class);

		assertTrue(run.isRunning());
		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(5))
				.until(() -> verifyAppIsHealthy(gateway));
	}

	private static boolean verifyAppIsHealthy(OutboundGateway gateway) {
		try {
			Message<Object> response = gateway.sendSyncMessage(RequestMessageBuilder.create(BusTopic.HEALTH).build(null));
			return "200".equals(response.getHeaders().get(BusMessageUtils.HEADER_PREFIX+MessageBase.STATUS_KEY));
		} catch (Exception e) {
			CYPRESS_LOG.error("error while checking health of application", e);
			return false;
		}
	}

	public static void startTestContainer() {
		org.testcontainers.Testcontainers.exposeHostPorts(8080);

		container = new CypressContainer();
		container.withBaseUrl("http://host.testcontainers.internal:8080/iaf-test/iaf/gui");
		container.withLogConsumer(frame -> CYPRESS_LOG.info(frame.getUtf8StringWithoutLineEnding()));

		container.start();

		assertTrue(container.isRunning());
	}

	@AfterAll
	public static void tearDown() {
		if (run == null) return;

		run.stop();
		container.stop();

		assertFalse(run.isRunning());
		assertFalse(container.isRunning());

		run.close();

		// Make sure to clear the app constants as well
		AppConstants.removeInstance();
	}

	@TestFactory
	@Nonnull Stream<DynamicContainer> runCypressTests() throws InterruptedException, IOException, TimeoutException {
		CypressTestResults testResults = container.getTestResults();

		return testResults.getSuites()
				.stream()
				.map(this::createContainerFromSuite);
	}

	private DynamicContainer createContainerFromSuite(CypressTestSuite suite) {
		Stream<DynamicTest> dynamicTests = suite.getTests().stream()
				.map(test -> DynamicTest.dynamicTest(
						test.getDescription(), () -> {
							if (!test.isSuccess()) {
								log.error("{}:\n{}", test.getErrorMessage(), test.getStackTrace());
							}
							assertTrue(test.isSuccess(), test::getErrorMessage);
						}
				));

		return DynamicContainer.dynamicContainer(suite.getTitle(), dynamicTests);
	}
}
