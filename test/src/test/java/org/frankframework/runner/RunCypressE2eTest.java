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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.wimdeblauwe.testcontainers.cypress.CypressContainer;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTest;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestResults;
import io.github.wimdeblauwe.testcontainers.cypress.CypressTestSuite;
import lombok.extern.log4j.Log4j2;

/**
 * Runs e2e tests with Cypress in a Testcontainer.
 *
 * @author Sergi Philipsen
 * @see "https://github.com/wimdeblauwe/testcontainers-cypress"
 */
@Log4j2
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class RunCypressE2eTest {
	private static CypressContainer container;
	private static ConfigurableApplicationContext run;

	@BeforeAll
	public static void setUp() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();
		Consumer<OutputFrame> logConsumer = outputFrame -> log.debug(outputFrame.getUtf8String());
		container = new CypressContainer();
		container.withBaseUrl("http://host.docker.internal:8080/iaf-test/iaf/gui");

		run = springApplication.run();
		container.withLogConsumer(logConsumer);
		container.start();

		Assertions.assertTrue(run.isRunning());
		Assertions.assertTrue(container.isRunning());
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
					log.error("{}: {}", test.getErrorMessage(), test.getStackTrace());
				}
				Assertions.assertTrue(test.isSuccess());
			}));
		}

		return DynamicContainer.dynamicContainer(suite.getTitle(), dynamicTests);
	}
}


