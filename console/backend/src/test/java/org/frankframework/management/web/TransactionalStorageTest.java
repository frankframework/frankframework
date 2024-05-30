package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, TransactionalStorage.class})
public class TransactionalStorageTest extends FrankApiTestBase {

	@Test
	public void testStorageSourceParsing() {
		assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("receivers"));
		assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("Receivers"));
		assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("RECEIVERS"));
		IllegalArgumentException i = assertThrows(IllegalArgumentException.class, () -> TransactionalStorage.StorageSource.fromString(""));
		assertEquals("no StorageSource option supplied", i.getMessage());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TransactionalStorage.StorageSource.fromString("dummy"));
		assertEquals("invalid StorageSource option", e.getMessage());
	}
}
