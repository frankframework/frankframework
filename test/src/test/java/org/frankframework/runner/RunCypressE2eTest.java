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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.wimdeblauwe.testcontainers.cypress.CypressContainer;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTest;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestResults;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestSuite;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.LogUtil;

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
	private static final Logger LOGGER = LogUtil.getLogger("cypress");

	@BeforeAll
	public static void setUp() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();

		org.testcontainers.Testcontainers.exposeHostPorts(8080);

		container = new CypressContainer();
		container.withBaseUrl("http://host.testcontainers.internal:8080/iaf-test/iaf/gui");
		container.withLogConsumer(frame -> LOGGER.info(frame.getUtf8StringWithoutLineEnding()));

		run = springApplication.run();
		container.start();

		assertTrue(run.isRunning());
		assertTrue(container.isRunning());
	}

	@AfterAll
	public static void tearDown() {
		run.stop();
		container.stop();

		Assertions.assertFalse(run.isRunning());
		Assertions.assertFalse(container.isRunning());
	}

	@TestFactory
	List<DynamicContainer> runCypressTests() throws InterruptedException, IOException, TimeoutException {
		CypressTestResults testResults = container.getTestResults();

		return convertToJUnitDynamicTests(testResults);
	}

	@Nonnull
	private List<DynamicContainer> convertToJUnitDynamicTests(CypressTestResults testResults) {
		return testResults.getSuites()
				.stream()
				.map(this::createContainerFromSuite)
				.toList();
	}

	private DynamicContainer createContainerFromSuite(CypressTestSuite suite) {
		List<DynamicTest> dynamicTests = new ArrayList<>();
		for (CypressTest test : suite.getTests()) {
			dynamicTests.add(DynamicTest.dynamicTest(test.getDescription(), () -> {
				if (!test.isSuccess()) {
					log.error("{}:\n{}", test.getErrorMessage(), test.getStackTrace());
				}
				assertTrue(test.isSuccess());
			}));
		}

		return DynamicContainer.dynamicContainer(suite.getTitle(), dynamicTests);
	}
}
