package org.frankframework.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

class FixedPositionRecordHandlerManagerTest {

	@Test
	public void testGetFirstPartOfNextRecord() throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader("eerste regel\ntweede regel\nderde regel"));
		reader.readLine();

		RecordHandlerManager manager = new FixedPositionRecordHandlerManager();

		assertEquals("tweede regel", manager.getFirstPartOfNextRecord(reader));
	}

	@Test
	public void testGetFullRecord() throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader("eerste regel\ntweede regel\nderde regel"));
		reader.readLine();
		String firstPart = reader.readLine();

		RecordHandlerManager manager = new FixedPositionRecordHandlerManager();

		assertEquals("tweede regel", manager.getFullRecord(reader, null, firstPart));
	}

	@Test
	public void testRawRecords() throws ConfigurationException, IOException {
		BufferedReader reader = new BufferedReader(new StringReader("abcd1234efgh3456pqrs6789"));

		FixedPositionRecordHandlerManager manager = new FixedPositionRecordHandlerManager();
		manager.setStartPosition(2);
		manager.setEndPosition(4);
		manager.setNewLineSeparated(false);

		RecordXmlTransformer handler = new RecordXmlTransformer();
		handler.setName("main");
		handler.setInputFields("2, 2, 4");
		handler.setOutputFields("pre,key,tail");

		RecordHandlingFlow flow = new RecordHandlingFlow();
		flow.setRecordKey("*");
		flow.setRecordHandler(handler);

		Map<String,IRecordHandlerManager> managerMap = new HashMap<>();
		managerMap.put("mgr", manager);

		Map<String,IRecordHandler> handlerMap = new HashMap<>();
		handlerMap.put("main", handler);

		Map<String,IResultHandler> resulthandlerMap = new HashMap<>();

		flow.configure(manager, managerMap, handlerMap, resulthandlerMap, null);

		// first record
		String firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("abcd", firstPart);
		assertEquals("abcd1234", manager.getFullRecord(reader, flow, firstPart));

		// second record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("efgh", firstPart);
		assertEquals("efgh3456", manager.getFullRecord(reader, flow, firstPart));

		// last record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("pqrs", firstPart);
		assertEquals("pqrs6789", manager.getFullRecord(reader, flow, firstPart));

		// after last record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertNull(firstPart);
	}

	@Test
	public void testRawRecordsIncompleteLast() throws ConfigurationException, IOException {
		BufferedReader reader = new BufferedReader(new StringReader("abcd1234efgh3456pqrs67"));

		FixedPositionRecordHandlerManager manager = new FixedPositionRecordHandlerManager();
		manager.setStartPosition(2);
		manager.setEndPosition(4);
		manager.setNewLineSeparated(false);

		RecordXmlTransformer handler = new RecordXmlTransformer();
		handler.setName("main");
		handler.setInputFields("2, 2, 4");
		handler.setOutputFields("pre,key,tail");

		RecordHandlingFlow flow = new RecordHandlingFlow();
		flow.setRecordKey("*");
		flow.setRecordHandler(handler);

		Map<String,IRecordHandlerManager> managerMap = new HashMap<>();
		managerMap.put("mgr", manager);

		Map<String,IRecordHandler> handlerMap = new HashMap<>();
		handlerMap.put("main", handler);

		Map<String,IResultHandler> resulthandlerMap = new HashMap<>();

		flow.configure(manager, managerMap, handlerMap, resulthandlerMap, null);

		// first record
		String firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("abcd", firstPart);
		assertEquals("abcd1234", manager.getFullRecord(reader, flow, firstPart));

		// second record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("efgh", firstPart);
		assertEquals("efgh3456", manager.getFullRecord(reader, flow, firstPart));

		// last record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("pqrs", firstPart);
		assertEquals("pqrs67", manager.getFullRecord(reader, flow, firstPart));

		// after last record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertNull(firstPart);
	}

	@Test
	public void testRawRecordsFewFields() throws ConfigurationException, IOException {
		BufferedReader reader = new BufferedReader(new StringReader("abcd1234efgh3456pqrs6789"));

		FixedPositionRecordHandlerManager manager = new FixedPositionRecordHandlerManager();
		manager.setStartPosition(2);
		manager.setEndPosition(4);
		manager.setNewLineSeparated(false);

		RecordXmlTransformer handler = new RecordXmlTransformer();
		handler.setName("main");
		handler.setInputFields("2");
		handler.setOutputFields("pre");

		RecordHandlingFlow flow = new RecordHandlingFlow();
		flow.setRecordKey("*");
		flow.setRecordHandler(handler);

		Map<String,IRecordHandlerManager> managerMap = new HashMap<>();
		managerMap.put("mgr", manager);

		Map<String,IRecordHandler> handlerMap = new HashMap<>();
		handlerMap.put("main", handler);

		Map<String,IResultHandler> resulthandlerMap = new HashMap<>();

		flow.configure(manager, managerMap, handlerMap, resulthandlerMap, null);

		String firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("abcd", firstPart);
		assertEquals("abcd", manager.getFullRecord(reader, flow, firstPart));

		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("1234", firstPart);
		assertEquals("1234", manager.getFullRecord(reader, flow, firstPart));
	}

	@Test
	public void testRawRecordsNoKey() throws ConfigurationException, IOException {
		BufferedReader reader = new BufferedReader(new StringReader("abcd1234efgh3456pqrs6789"));

		FixedPositionRecordHandlerManager manager = new FixedPositionRecordHandlerManager();
		manager.setNewLineSeparated(false);

		RecordXmlTransformer handler = new RecordXmlTransformer();
		handler.setName("main");
		handler.setInputFields("2, 2, 4");
		handler.setOutputFields("pre,key,tail");

		RecordHandlingFlow flow = new RecordHandlingFlow();
		flow.setRecordKey("*");
		flow.setRecordHandler(handler);

		Map<String,IRecordHandlerManager> managerMap = new HashMap<>();
		managerMap.put("mgr", manager);

		Map<String,IRecordHandler> handlerMap = new HashMap<>();
		handlerMap.put("main", handler);

		Map<String,IResultHandler> resulthandlerMap = new HashMap<>();

		flow.configure(manager, managerMap, handlerMap, resulthandlerMap, null);

		// first record
		String firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("a", firstPart);
		assertEquals("abcd1234", manager.getFullRecord(reader, flow, firstPart));

		// second record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("e", firstPart);
		assertEquals("efgh3456", manager.getFullRecord(reader, flow, firstPart));

		// last record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertEquals("p", firstPart);
		assertEquals("pqrs6789", manager.getFullRecord(reader, flow, firstPart));

		// after last record
		firstPart = manager.getFirstPartOfNextRecord(reader);
		assertNull(firstPart);
	}

}
