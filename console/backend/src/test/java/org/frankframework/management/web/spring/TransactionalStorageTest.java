package org.frankframework.management.web.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, TransactionalStorage.class})
public class TransactionalStorageTest extends FrankApiTestBase {

	@Test
	public void testBrowseMessages() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/configurations/test/adapters/test/PIPES/test/stores/test/messages/test"))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("{\"topic\":\"MESSAGE_BROWSER\",\"action\":\"GET\"}"));
	}

	@Test
	public void testStorageSourceParsing() {
		Assertions.assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("receivers"));
		Assertions.assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("Receivers"));
		Assertions.assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("RECEIVERS"));
		IllegalArgumentException i = assertThrows(IllegalArgumentException.class, () -> org.frankframework.management.web.TransactionalStorage.StorageSource.fromString(""));
		assertEquals("no StorageSource option supplied", i.getMessage());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TransactionalStorage.StorageSource.fromString("dummy"));
		assertEquals("invalid StorageSource option", e.getMessage());
	}
}
