package org.frankframework.management.web.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, TransactionalStorage.class})
public class TransactionalStorageTest extends FrankApiTestBase {

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
