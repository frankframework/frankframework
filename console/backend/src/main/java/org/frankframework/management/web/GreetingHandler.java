package org.frankframework.management.web;

import org.frankframework.util.JacksonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class GreetingHandler extends AbstractWebSocketHandler {

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		GreetingDTO greeting = JacksonUtils.convertToDTO(message.getPayload(), GreetingDTO.class);
		TextMessage reply = new TextMessage("Hello " + greeting.name() + "!");
		session.sendMessage(reply);
	}

	protected record GreetingDTO(String name) {}

}