package org.frankframework.management.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, Scheduler.class})
public class SchedulerTest extends FrankApiTestBase {

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
		mockMvc.perform(put("/schedules")
						.content("{\"action\": \"start\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void trigger() throws Exception {
		mockMvc.perform(put("/schedules/groupName/jobs/jobName")
						.content("{\"action\": \"start\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void createSchedule() throws Exception {
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
}
