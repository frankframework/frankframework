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
package org.frankframework.management.web.socket;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.web.RequestMessageBuilder;
import org.frankframework.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.Json;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonValue;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FrankApiWebSocketBase {

	private static final String ROLE_PREFIX = "ROLE_"; //see AuthorityAuthorizationManager#ROLE_PREFIX
	private static final List<GrantedAuthority> READ_ONLY_AUTHORITY = Collections.singletonList(new SimpleGrantedAuthority(ROLE_PREFIX + "IbisObserver"));
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	@Autowired
	@Qualifier("outboundGateway")
	private OutboundGateway gateway;

	@Autowired
	protected SimpMessagingTemplate messagingTemplate;

	@Autowired
	private MessageCacheStore messageCacheStore;

	protected final void propagateAuthenticationContext(String name) {
		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
		Authentication wsAuthentication = new AnonymousAuthenticationToken(name, name, READ_ONLY_AUTHORITY);
		context.setAuthentication(wsAuthentication);
		this.securityContextHolderStrategy.setContext(context);
	}

	@Nullable
	protected String compareAndUpdateResponse(RequestMessageBuilder builder) {
		final Message<?> response;
		try {
			response = gateway.sendSyncMessage(builder.build());
		} catch (Exception e) { //BusException
			log.error("exception while sending synchronous bus request", e);
			return null;
		}

		String stringResponse = ResponseUtils.parseAsString(response);
		return convertMessageToDiff(builder.getTopic(), stringResponse);
	}

	/** we can assume that all messages stored in the cache are JSON messages */
	@Nullable
	private String convertMessageToDiff(BusTopic topic, @Nonnull String latestJsonMessage) {
		String cachedJsonMessage = messageCacheStore.getAndUpdate(topic, latestJsonMessage);
		return findJsonDiff(cachedJsonMessage, latestJsonMessage);
	}

	@Nullable
	private String findJsonDiff(@Nonnull String cachedJsonMessage, @Nonnull String latestJsonMessage) {
		try {
			JsonValue source = Json.createReader(new StringReader(cachedJsonMessage)).readValue();
			JsonValue target = Json.createReader(new StringReader(latestJsonMessage)).readValue();
			JsonMergePatch mergeDiff = Json.createMergeDiff(source, target);
			return mergeDiff.toJsonValue().toString();
		} catch (Exception e) {
			log.error("exception while performing json compare", e);
			return null;
		}
	}

}
