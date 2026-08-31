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

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.configuration.ClientSession;
import org.frankframework.console.util.ResponseUtils;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.gateway.events.ClusterMemberEvent;
import org.frankframework.management.gateway.events.ClusterMemberEvent.EventType;
import org.frankframework.util.JacksonUtils;

/**
 * Cluster in this sense does not directly mean a Kubernetes or similar cluster, but a Hazelcast cluster.
 */
@RestController
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ClusterMembers implements ApplicationListener<ClusterMemberEvent> {

	private final ClientSession session;

	private final OutboundGateway outboundGateway;

	protected final SimpMessagingTemplate messagingTemplate;

	public ClusterMembers(ClientSession session, OutboundGateway outboundGateway, SimpMessagingTemplate messagingTemplate) {
		this.session = session;
		this.outboundGateway = outboundGateway;
		this.messagingTemplate = messagingTemplate;
	}

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("cluster")
	@Description("view all available cluster members")
	@GetMapping(value = "/cluster/members", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getClusterMembers(GetClusterMembersParams params) {
		String id = params.id;
		String name = params.name;
		String type = params.type;

		if (StringUtils.isNotEmpty(id)) {
			UUID uuid = UUID.fromString(id);
			setMemberTarget(uuid);
		} else if (StringUtils.isNotEmpty(name)) {
			setMemberTarget(name);
		}

		List<ClusterMember> members = type != null ?
				outboundGateway.getMembers().stream().filter(m -> type.equals(m.getType())).toList() :
				outboundGateway.getMembers();

		if (members.isEmpty()) {
			JsonMessage response = new JsonMessage(members);
			return ResponseUtils.convertToSpringResponse(response);
		}

		boolean hasSelectedMember = false;

		for (ClusterMember member : members) {
			if (member.getId().equals(session.getMemberTarget())) {
				hasSelectedMember = true;
				member.setSelectedMember(true);
			}
		}

		if (!hasSelectedMember) {
			ClusterMember firstWorker = members.stream().filter(m -> "worker".equals(m.getType())).findFirst().orElse(null);
			if (firstWorker != null) {
				setMemberTarget(firstWorker.getId());
				firstWorker.setSelectedMember(true);
			}
		}

		JsonMessage response = new JsonMessage(members);
		return ResponseUtils.convertToSpringResponse(response);
	}

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("cluster")
	@Description("select a specific cluster member to retrieve data from")
	@PostMapping(value = "/cluster/members", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> setClusterMemberTarget(@RequestBody ClusterMemberTargetModel model) {
		if (model.name != null)  setMemberTarget(model.name);
		if (model.id != null) {
			UUID uuid = UUID.fromString(model.id);
			setMemberTarget(uuid);
		}
		return ResponseEntity.accepted().build();
	}

	@Override
	public void onApplicationEvent(ClusterMemberEvent event) {
		String jsonResponse = JacksonUtils.convertToJson(new EventWrapper(event.getType(), event.getMember()));
		this.messagingTemplate.convertAndSend("/event/cluster", jsonResponse);
	}

	public record GetClusterMembersParams(String id, String name, String type) {}

	public record ClusterMemberTargetModel(
			String id,
			String name
	) {}

	private void setMemberTarget(UUID id) {
		List<ClusterMember> members = outboundGateway.getMembers();
		members.stream()
				.filter(member -> "worker".equals(member.getType()))
				.filter(member -> id.equals(member.getId()))
				.findAny()
				.orElseThrow(() -> new ApiException("member target with id ["+id.toString()+"] not found"));
		session.setMemberTarget(id);
	}

	private void setMemberTarget(String name) {
		List<ClusterMember> members = outboundGateway.getMembers();
		ClusterMember target = members.stream()
				.filter(member -> "worker".equals(member.getType()))
				.filter(member -> name.equals(member.getName()))
				.findFirst()
				.orElseThrow(() -> new ApiException("member target with instance name ["+name+"] not found"));
		session.setMemberTarget(target.getId());
	}

	private record EventWrapper(EventType type, ClusterMember member) {}
}
