package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, ClassInfo.class})
class ClassInfoTest extends FrankApiTestBase {

	@Test
	public void getClassInfoCorrectResponse() throws Exception {
		testActionAndTopicHeaders("/classinfo/className", "DEBUG", "GET");
	}
}
