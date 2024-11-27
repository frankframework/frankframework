/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.console.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.log4j.Log4j2;

/**
 * This class is found by classpath scanning in the `FrankFrameworkApiContext.xml` file.
 * It is loaded after the xml has been loaded/wired.
 */
@Log4j2
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	@Value("${cors.origin:*}")
	private String allowedCorsOrigins; // Defaults to ALL allowed

	@Value("${cors.enforced:false}")
	private boolean enforceCORS;

	// localhost has been added to allowed origins so WebSockets can be used during local development on a `Node.js` server.
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		if(!enforceCORS && !"*".equals(allowedCorsOrigins)) {
			log.warn("CORS origin was set to [{}] but CORS is not enforced, disable CORS for WebSockets completely", allowedCorsOrigins);
			allowedCorsOrigins = "*";
		}

		registry.addEndpoint("/ws")
				.setAllowedOriginPatterns(allowedCorsOrigins);
		registry.addEndpoint("/stomp")
				.setAllowedOriginPatterns(allowedCorsOrigins)
				.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/event");
	}
}
