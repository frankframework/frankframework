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

package org.frankframework.runner;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.wimdeblauwe.testcontainers.cypress.CypressContainer;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestResults;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestSuite;
import lombok.extern.log4j.Log4j2;

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
@Disabled // TODO enable this when the cypress tests are ported
public class RunCypressE2eTest {
	@SuppressWarnings("NullAway.Init")
	private static CypressContainer container;

	@SuppressWarnings("NullAway.Init")
	private static FrankApplication frankApplication;

	private static final String TEST_CONTAINER_BASE_URL = "http://host.testcontainers.internal:8080";
	private static final Path MOCHAWESOME_REPORTS_DIR = Paths.get("target/test-classes/e2e/cypress/test-results/reports/mochawesome");

	@BeforeAll
	static void setUp() throws IOException {
		// Pollers for WebSockets have an enormous delay for larger applications.
		System.setProperty("console.socket.poller.startDelay", "15");
		System.setProperty("console.socket.poller.warnings", "5");
		System.setProperty("console.socket.poller.adapters", "5");
		System.setProperty("console.socket.poller.messages", "5");

		startCypressInitializer();
		startTestContainer();
	}

	private static void startCypressInitializer() throws IOException {
		frankApplication = CypressInitializer.configureApplication();
		frankApplication.run();

		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(5))
				.until(() -> frankApplication.hasStarted());
	}

	public static void startTestContainer() {
		org.testcontainers.Testcontainers.exposeHostPorts(8080);

		container = new CypressContainer();

		// TODO figure out the correct url based on /e2e
		container.withBaseUrl(TEST_CONTAINER_BASE_URL + "/iaf-test/iaf/gui");
		container.withMochawesomeReportsAt(MOCHAWESOME_REPORTS_DIR);
		container.withClasspathResourcePath("e2e");
		container.withWorkingDirectory("/e2e/cypress");
		container.withLogConsumer(new Log4j2LogConsumer("CypressContainer", "CYPRESS"));

		container.start();

		Assertions.assertTrue(container.isRunning());
	}

	@AfterAll
	static void tearDown() {
		FrankApplication.exit(frankApplication);

		if (container != null) {
			container.stop();
			Assertions.assertFalse(container.isRunning());
		}

		Assertions.assertFalse(frankApplication.isRunning());
	}

	@TestFactory
	@NonNull Stream<DynamicContainer> runCypressTests() throws InterruptedException, IOException, TimeoutException {
		if (container == null) {
			return Stream.empty();
		}
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
								Assertions.assertTrue(frankApplication.hasStarted(), "!! application not reachable !!");
							}
							Assertions.assertTrue(test.isSuccess(), test::getErrorMessage);
						}
				));

		return DynamicContainer.dynamicContainer(suite.getTitle(), dynamicTests);
	}
}
