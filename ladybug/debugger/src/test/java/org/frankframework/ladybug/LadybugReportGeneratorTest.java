package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.wearefrank.ladybug.TestTool;

import org.frankframework.stream.Message;

public class LadybugReportGeneratorTest {

	private static final String CID = "abc-123";

	@Test
	void getInputFromFixedValue() {
		LadybugReportGenerator generator = new LadybugReportGenerator();
		TestTool testtool = mock(TestTool.class);

		generator.setTestTool(testtool);

		ArgumentCaptor<String> cidcapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);

		when(testtool.inputpoint(cidcapture.capture(), any(), nameCapture.capture(), valueCapture.capture(), anyMap())).thenAnswer(a -> a.getArguments()[3]);

		String result = generator.getInputFromFixedValue(CID, "String data");

		assertEquals("String data", result);
		assertEquals(CID, cidcapture.getValue());
		assertEquals("getInputFromFixedValue", nameCapture.getValue());
		assertEquals("String data", valueCapture.getValue());
	}

	@Test
	void getInputFromSessionKey() throws IOException {
		LadybugReportGenerator generator = new LadybugReportGenerator();
		TestTool testtool = mock(TestTool.class);

		generator.setTestTool(testtool);


		ArgumentCaptor<String> cidcapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);

		when(testtool.inputpoint(cidcapture.capture(), any(), nameCapture.capture(), valueCapture.capture(), anyMap())).thenAnswer(a -> a.getArguments()[3]);

		Message result = generator.getInputFromSessionKey(CID, "key-name", new Message("String data"));

		assertEquals("String data", result.asString());
		assertEquals(CID, cidcapture.getValue());
		assertEquals("getInputFromSessionKey key-name", nameCapture.getValue());
		assertEquals(result, valueCapture.getValue());
	}

	@Test
	void getDefaultValue() {
		LadybugReportGenerator generator = new LadybugReportGenerator();
		TestTool testtool = mock(TestTool.class);

		generator.setTestTool(testtool);

		ArgumentCaptor<String> cidcapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);

		when(testtool.inputpoint(cidcapture.capture(), any(), nameCapture.capture(), valueCapture.capture(), anyMap())).thenAnswer(a -> a.getArguments()[3]);

		String result = generator.getDefaultValue(CID, "String data");

		assertEquals("String data", result);
		assertEquals(CID, cidcapture.getValue());
		assertEquals("getDefaultValue", nameCapture.getValue());
		assertEquals("String data", valueCapture.getValue());
	}
}
