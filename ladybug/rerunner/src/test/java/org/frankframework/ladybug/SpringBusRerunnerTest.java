package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SpringBusRerunnerTest {

	@Test
	public void test() {
		SpringBusRerunner rerunner = new SpringBusRerunner();
		Map<String, String> threadContext = new HashMap<>();
		threadContext.put("mid", "123456");
		threadContext.put("cid", "654321");

		String json = rerunner.toJson(threadContext);
		String expected = "[{\"key\":\"mid\",\"value\":\"123456\"},{\"key\":\"cid\",\"value\":\"654321\"}]";
		assertEquals(expected, json);
	}
}
