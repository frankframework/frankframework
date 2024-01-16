package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTransactionalStorage {

	@Test
	public void testStorageSourceParsing() {
		Assertions.assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("receivers"));
		Assertions.assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("Receivers"));
		Assertions.assertEquals(TransactionalStorage.StorageSource.RECEIVERS, TransactionalStorage.StorageSource.fromString("RECEIVERS"));
		IllegalArgumentException i = assertThrows(IllegalArgumentException.class, () -> TransactionalStorage.StorageSource.fromString(""));
		assertEquals("no StorageSource option supplied", i.getMessage());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TransactionalStorage.StorageSource.fromString("dummy"));
		assertEquals("invalid StorageSource option", e.getMessage());
	}
}
