package org.frankframework.runner;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class DockerAvailableCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();

		return dockerAvailable
				? ConditionEvaluationResult.enabled("Docker is available")
				: ConditionEvaluationResult.disabled("Docker is not available");
	}
}
