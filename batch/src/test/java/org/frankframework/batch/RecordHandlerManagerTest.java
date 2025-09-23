package org.frankframework.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

class RecordHandlerManagerTest {

	@Test
	public void testGetFirstPartOfNextRecord() throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader("eerste regel\ntweede regel\nderde regel"));
		reader.readLine();
		
		RecordHandlerManager manager = new RecordHandlerManager();
		
		assertEquals("tweede regel", manager.getFirstPartOfNextRecord(reader));
	}

	@Test
	public void testGetFullRecord() throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader("eerste regel\ntweede regel\nderde regel"));
		reader.readLine();
		String firstPart = reader.readLine();
		
		RecordHandlerManager manager = new RecordHandlerManager();
		
		assertEquals("tweede regel", manager.getFullRecord(reader, null, firstPart));
	}

}
