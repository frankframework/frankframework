package org.frankframework.console.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.frankframework.management.gateway.events.ClusterMemberEvent;
import org.frankframework.management.gateway.events.ClusterMemberEvent.EventType;

@ContextConfiguration(classes = {WebTestConfiguration.class, ClusterMembers.class})
public class ClusterMembersTest extends FrankApiTestBase {

	@Autowired
	private ClusterMembers controller;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Test
	void getClusterMembersSelectsFirstWorkerWhenNoTarget() throws Exception {
		ClusterMember firstWorker = createMember("node-a", "worker");
		ClusterMember manager = createMember("manager-a", "manager");
		Mockito.doReturn(List.of(firstWorker, manager)).when(outputGateway).getMembers();

		mockMvc.perform(MockMvcRequestBuilders.get("/cluster/members"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("node-a"))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].selectedMember").value(true));
	}

	@Test
	void getClusterMembersWithNameSelectsMatchingWorker() throws Exception {
		ClusterMember firstWorker = createMember("node-a", "worker");
		ClusterMember secondWorker = createMember("node-b", "worker");
		Mockito.doReturn(List.of(firstWorker, secondWorker)).when(outputGateway).getMembers();

		mockMvc.perform(MockMvcRequestBuilders.get("/cluster/members").param("name", "node-b"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].selectedMember").value(false))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].selectedMember").value(true));
	}

	@Test
	void getClusterMembersWithTypeFilterReturnsOnlyRequestedType() throws Exception {
		ClusterMember worker = createMember("node-a", "worker");
		ClusterMember manager = createMember("manager-a", "manager");
		Mockito.doReturn(List.of(worker, manager)).when(outputGateway).getMembers();

		mockMvc.perform(MockMvcRequestBuilders.get("/cluster/members").param("type", "worker"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(1)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("worker"))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].selectedMember").value(true));
	}

	@Test
	void postClusterMemberByIdSetsSelectionForSubsequentRequest() throws Exception {
		ClusterMember firstWorker = createMember("node-a", "worker");
		ClusterMember secondWorker = createMember("node-b", "worker");
		Mockito.doReturn(List.of(firstWorker, secondWorker)).when(outputGateway).getMembers();
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(MockMvcRequestBuilders.post("/cluster/members")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":\"" + secondWorker.getId() + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isAccepted());

		mockMvc.perform(MockMvcRequestBuilders.get("/cluster/members").session(session))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].selectedMember").value(false))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].selectedMember").value(true));
	}

	@Test
	void onApplicationEventSendsClusterEventToWebsocketTopic() {
		ClusterMember member = createMember("node-a", "worker");
		ApplicationContext source = Mockito.mock(ApplicationContext.class);
		ClusterMemberEvent event = new ClusterMemberEvent(source, EventType.ADD_MEMBER, member);
		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

		controller.onApplicationEvent(event);

		Mockito.verify(messagingTemplate).convertAndSend(Mockito.eq("/event/cluster"), payloadCaptor.capture());
		String payload = payloadCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertTrue(payload.contains("\"type\":\"ADD_MEMBER\""));
		org.junit.jupiter.api.Assertions.assertTrue(payload.contains(member.getId().toString()));
	}

	@Test
	void getClusterMembersWithUnknownNameReturnsApiError() throws Exception {
		ClusterMember firstWorker = createMember("node-a", "worker");
		Mockito.doReturn(List.of(firstWorker)).when(outputGateway).getMembers();

		mockMvc.perform(MockMvcRequestBuilders.get("/cluster/members").param("name", "missing-node"))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string(containsString("member target with instance name [missing-node] not found")));
	}

	private ClusterMember createMember(String name, String type) {
		ClusterMember member = new ClusterMember();
		member.setId(UUID.randomUUID());
		member.setName(name);
		member.setType(type);
		return member;
	}
}
