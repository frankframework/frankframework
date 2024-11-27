package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, Scheduler.class})
public class SchedulerTest extends FrankApiTestBase {

	@Test
	public void getSchedules() throws Exception {
		testActionAndTopicHeaders("/schedules", "SCHEDULER", "GET");
	}

	@Test
	public void getSchedule() throws Exception {
		testActionAndTopicHeaders("/schedules/groupName/jobs/jobName", "SCHEDULER", "FIND");
	}

	@Test
	public void updateScheduler() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("start", in.getHeaders().get("meta-operation"));
			assertEquals("SCHEDULER", in.getHeaders().get("topic"));
			assertEquals("MANAGE", in.getHeaders().get("action"));

			return in;
		});

		mockMvc.perform(put("/schedules")
						.content("{\"action\": \"start\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void trigger() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("start", in.getHeaders().get("meta-operation"));
			assertEquals("SCHEDULER", in.getHeaders().get("topic"));
			assertEquals("MANAGE", in.getHeaders().get("action"));

			return in;
		});

		mockMvc.perform(put("/schedules/groupName/jobs/jobName")
						.content("{\"action\": \"start\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void createSchedule() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("scheduleName", in.getHeaders().get("meta-job"));
			assertEquals("groupName", in.getHeaders().get("meta-group"));
			assertEquals("UPLOAD", in.getHeaders().get("action"));

			return in;
		});

		mockMvc.perform(multipart("/schedules")
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void createScheduleWithoutAllParts() throws Exception {
		mockMvc.perform(multipart("/schedules")
						.part(new MockPart("name", "scheduleName".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("Key [group] not defined"));
	}

	@Test
	public void updateSchedule() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("jobName", in.getHeaders().get("meta-job"));
			assertEquals("groupName", in.getHeaders().get("meta-group"));
			assertEquals("UPLOAD", in.getHeaders().get("action"));

			return in;
		});
		mockMvc.perform(multipart(HttpMethod.PUT, "/schedules/groupName/jobs/jobName")
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateScheduleWithoutAllParts() throws Exception {
		mockMvc.perform(multipart(HttpMethod.PUT, "/schedules/groupName/jobs/jobName")
						.part(new MockPart("name", "scheduleName".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("Key [adapter] not defined"));
	}

	@Test
	public void createScheduleInJobGroup() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("scheduleName", in.getHeaders().get("meta-job"));
			assertEquals("groupName", in.getHeaders().get("meta-group"));
			assertEquals("UPLOAD", in.getHeaders().get("action"));

			return in;
		});
		mockMvc.perform(multipart("/schedules/groupName/jobs")
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void createScheduleInJobGroupWithoutAllParts() throws Exception {
		mockMvc.perform(multipart("/schedules/groupName/jobs")
						.part(new MockPart("name", "scheduleName".getBytes())))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("Key [adapter] not defined"));
	}

	private static MockPart[] getMultiPartParts() {
		return new MockPart[]{
				new MockPart("name", "scheduleName".getBytes()),
				new MockPart("group", "groupName".getBytes()),
				new MockPart("cron", "* * * * *".getBytes()),
				new MockPart("interval", "0".getBytes()),
				new MockPart("adapter", "adapterName".getBytes()),
				new MockPart("receiver", "receiverName".getBytes()),
				new MockPart("configuration", "configurationName".getBytes()),
				new MockPart("listener", "listenenerName".getBytes()),
				new MockPart("lockkey", "".getBytes()),
				new MockPart("message", "".getBytes()),
				new MockPart("description", "".getBytes()),
				new MockPart("persistent", "false".getBytes()),
				new MockPart("locker", "false".getBytes()),
		};
	}
}
