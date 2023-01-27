package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.management.web.TransactionalStorage.StorageSource;

public class TestTransactionalStorage {

	@Test
	public void testStorageSourceParsing() {
		assertEquals(StorageSource.RECEIVERS, StorageSource.fromString("receivers"));
		assertEquals(StorageSource.RECEIVERS, StorageSource.fromString("Receivers"));
		assertEquals(StorageSource.RECEIVERS, StorageSource.fromString("RECEIVERS"));
		IllegalArgumentException i = assertThrows(IllegalArgumentException.class, ()-> StorageSource.fromString(""));
		assertEquals("no StorageSource option supplied", i.getMessage());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()-> StorageSource.fromString("dummy"));
		assertEquals("invalid StorageSource option", e.getMessage());
	}
}
