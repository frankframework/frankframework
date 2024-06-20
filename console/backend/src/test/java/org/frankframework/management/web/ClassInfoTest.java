package org.frankframework.management.web;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, ClassInfo.class})
class ClassInfoTest extends FrankApiTestBase {

	@Test
	public void getClassInfoCorrectResponse() throws Exception {
		testBasicRequest("/classinfo/{className}", "DEBUG", "GET", "String.class");
	}

}
