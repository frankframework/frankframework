package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.jdbc.datasource.TestBlobs;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.xml.XmlWriter;

class RestoreMovedElementsHandlerTest {

	private final XmlWriter xmlWriter = new XmlWriter();
	private final RestoreMovedElementsHandler handler = new RestoreMovedElementsHandler(xmlWriter);
	private final PipeLineSession pipeLineSession = new PipeLineSession();

	@AfterEach
	void tearDown() {
		pipeLineSession.close();
	}

	@Test
	void testBasicRestoreFlow() throws IOException, SAXException {
		String inputString = "<xml>{sessionKey:1}abc{sessionKey:2}def{sessionKey:3}</xml>";
		pipeLineSession.put("1", "123");
		pipeLineSession.put("2", "456");
		pipeLineSession.put("3", "789");
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals("<xml>123abc456def789</xml>", xmlWriter.toString());
	}

	@Test
	void testRealXMLInputFile() throws IOException, SAXException {
		String inputString = TestFileUtils.getTestFile("/Util/CompactSaxHandler/input-chaintest.xml");
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals(inputString, xmlWriter.toString());
	}

	@Test
	void testBigSessionData() throws IOException, SAXException {
		String inputString = TestFileUtils.getTestFile("/Util/RestoreMovedElementsHandler/input.xml");
		handler.setSession(pipeLineSession);
		String textBlob = TestBlobs.getBigString(1024, 1000);
		String replacedContent = "Brief";
		pipeLineSession.put(replacedContent, textBlob);

		// Act
		XmlUtils.parseXml(inputString.replace(replacedContent, "{sessionKey:" + replacedContent + "}"), handler);

		assertEquals(xmlWriter.toString(), inputString.replace("Brief", textBlob));
		assertEquals(xmlWriter.toString().length(), inputString.length() + textBlob.length() - replacedContent.length());
	}

	@Test
	void testCDATA() throws IOException, SAXException {
		String inputString = TestFileUtils.getTestFile("/Util/RestoreMovedElementsHandler/input.xml");
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals(inputString, xmlWriter.toString());
	}

	@Test
	void testCorruptedInputFlow1() throws IOException, SAXException {
		String inputString = "<xml>xxxxxxx{session Key:1}abc{sessionKeydef{sessionKey:3{sessionKey:2}{sessionKey:1}}}}}}{sessionKey:notThere}</xml>";
		pipeLineSession.put("1", "123");
		pipeLineSession.put("2", "456");
		pipeLineSession.put("3", "789");
		pipeLineSession.put("33", "123");
		pipeLineSession.put("not There", "123");
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals("<xml>xxxxxxx{session Key:1}abc{sessionKeydef{sessionKey:3456123}}}}}{sessionKey:notThere}</xml>", xmlWriter.toString());
	}

	@Test
	void testCorruptedInputFlow2() throws IOException, SAXException {
		String inputString = "<xml>{sessionKey:{sessionKey:{sessionKey:{sessionKey:}{{{{{{}}}{{{sessionKey:</xml>";
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals("<xml>{sessionKey:{sessionKey:{sessionKey:{sessionKey:}{{{{{{}}}{{{sessionKey:</xml>", xmlWriter.toString());
	}

	@Test
	void testCorruptedInputFlow3() throws IOException, SAXException {
		String inputString = "<xml>{sessionKey: $$$$%%§§§§§§§§§§``````````%% }</xml>";
		pipeLineSession.put(" $$$$%%", Integer.MAX_VALUE);
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals("<xml>{sessionKey: $$$$%%§§§§§§§§§§``````````%% }</xml>", xmlWriter.toString());
	}

	@Test
	void testCorruptedInputFlow4() throws IOException, SAXException {
		String inputString = "<xml>{sessionKey:1}abc{sessionKey:2}def{sessionKey:3}</xml>";
		pipeLineSession.put("1", "{sessionKey:2}");
		pipeLineSession.put("-2", "{sessionKey:3}");
		pipeLineSession.put("abc", "efg");
		handler.setSession(pipeLineSession);
		XmlUtils.parseXml(inputString, handler);
		assertEquals("<xml>{sessionKey:2}abc{sessionKey:2}def{sessionKey:3}</xml>", xmlWriter.toString());
	}

}
