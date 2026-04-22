package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import org.frankframework.runner.FrankApplication;

public class TestAopConfiguration {

	@Test
	@DisplayName("Verify https://github.com/frankframework/frankframework/issues/10734 does not occur.")
	void validateThereAreNoAopErrors() throws Exception {
		FrankApplication frankApplication;
		try (TestAppender testAppender = TestAppender.newBuilder().minLogLevel(Level.DEBUG).build(AspectJExpressionPointcut.class)) {
			frankApplication = new FrankApplication();
			frankApplication.run();

			assertTrue(frankApplication.isRunning());

			assertTrue(testAppender.getLogEvents().isEmpty(), "FOUND AOP ERRORS! >> " + testAppender.getLogEvents());
		}

		FrankApplication.exit(frankApplication);
	}

}
