package org.frankframework.management.web.spring;

public record TestGatewayMessageResponse(MessageResult result,
		String state,
		String message) {

	public record MessageResult(String topic, String action) {
	}
}
