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
package org.frankframework.console.controllers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
public class BrowseQueue {

	private final FrankApiService frankApiService;

	public BrowseQueue(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@GetMapping(value = "/jms", produces = MediaType.APPLICATION_JSON_VALUE)
	@Relation("queuebrowser")
	@Description("view a list of all JMS QueueConnectionFactories")
	public ResponseEntity<?> getQueueConnectionFactories() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.QUEUE, BusAction.GET));
	}

	@AllowAllIbisUserRoles
	@PostMapping(value = "/jms/browse", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Relation("queuebrowser")
	@Description("view a list of messages on a specific JMS queue")
	public ResponseEntity<?> browseQueue(@RequestBody BrowseQueueModel model) {
		if (StringUtils.isEmpty(model.destination))
			throw new ApiException("No destination provided");
		if (StringUtils.isEmpty(model.type))
			throw new ApiException("No type provided");

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.QUEUE, BusAction.FIND);
		builder.addHeader(BusMessageUtils.HEADER_CONNECTION_FACTORY_NAME_KEY, model.connectionFactory);
		builder.addHeader("destination", model.destination);
		builder.addHeader("type", model.type);
		if (model.rowNumbersOnly != null) {
			builder.addHeader("rowNumbersOnly", model.rowNumbersOnly);
		}
		if (model.payload != null) {
			builder.addHeader("showPayload", model.payload);
		}
		if (model.lookupDestination != null) {
			builder.addHeader("lookupDestination", model.lookupDestination);
		}
		return frankApiService.callSyncGateway(builder);
	}

	public record BrowseQueueModel(
			String connectionFactory,
			String destination,
			String type,
			Boolean rowNumbersOnly,
			Boolean payload,
			Boolean lookupDestination
	) { }
}
