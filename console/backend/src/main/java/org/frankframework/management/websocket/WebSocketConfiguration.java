package org.frankframework.management.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.standard.WebSocketContainerFactoryBean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/*@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/gs-guide-websocket");
		// TODO setup SockJS as fallback
	}
}*/


@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//		registry.addHandler(greetingHandler(), getUrlMapping("greeting")).setAllowedOrigins("*");
		registry.addHandler(greetingHandler(), "/greeting").setAllowedOrigins("*");
	}

	@Bean
	public WebSocketHandler greetingHandler() {
		return new GreetingHandler();
	}

	@Bean
	public WebSocketContainerFactoryBean createWebSocketContainer() {
		WebSocketContainerFactoryBean container = new WebSocketContainerFactoryBean();
		container.setMaxTextMessageBufferSize(8192);
		container.setMaxBinaryMessageBufferSize(8192);
		container.setMaxSessionIdleTimeout(30_000);
		container.setAsyncSendTimeout(30_000);
		return container;
	}

	private String getUrlMapping(String path) {
		return "/iaf/ws/" + path;
	}
}
