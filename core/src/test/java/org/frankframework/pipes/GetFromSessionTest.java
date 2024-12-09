package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterType;
import org.frankframework.util.StreamUtil;

public class GetFromSessionTest extends PipeTestBase<GetFromSession> {

	private PipeLineSession session;

	private final String DUMMY_DATA = "dummy data";

	@BeforeEach
	public void populateSession() {
		session = new PipeLineSession();
		session.put("dummyString", DUMMY_DATA);
		session.put("dummyByteArray", DUMMY_DATA.getBytes());
		session.put("dummyStream", new ByteArrayInputStream(DUMMY_DATA.getBytes()));

		session.put("emptyMap", new HashMap<String, String>());
		Map<String, String> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		session.put("map", map);
	}

	@Override
	public GetFromSession createPipe() {
		return new GetFromSession();
	}

	@Test
	public void sessionKeyIsEmpty() throws Exception {
		pipe.setSessionKey("");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "dummyString", session);
		String result=prr.getResult().asString();
		assertEquals(DUMMY_DATA, result.trim());
	}

	@Test
	public void unknownSessionKey() throws ConfigurationException, PipeRunException {
		pipe.setSessionKey("unknown");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "ingored", session);
		assertTrue(prr.getResult().isNull());
	}

	@Test
	public void retrieveStringFromSession() throws Exception {
		pipe.setSessionKey("dummyString");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "ingored", session);
		String result = prr.getResult().asString();
		assertEquals(DUMMY_DATA, result);
	}

	@Test
	public void retrieveByteArrayFromSession() throws Exception {
		pipe.setSessionKey("dummyByteArray");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "ingored", session);
		byte[] result = prr.getResult().asByteArray();
		assertEquals(DUMMY_DATA, new String(result));
	}

	@Test
	public void retrieveInputStreamFromSession() throws ConfigurationException, PipeRunException, IOException {
		pipe.setSessionKey("dummyStream");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "ingored", session);
		InputStream result = prr.getResult().asInputStream();
		assertEquals(DUMMY_DATA, StreamUtil.streamToString(result));
	}

	@Test
	public void retrieveEmptyMapFromSession() throws Exception {
		pipe.setType(ParameterType.MAP);
		pipe.setSessionKey("emptyMap");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "ingored", session);
		String result = prr.getResult().asString();
		assertEquals("<items/>", result.trim());
	}

	@Test
	public void retrieveMapFromSession() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(ParameterType.MAP);
		pipe.setSessionKey("map");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "ingored", session);
		String result = prr.getResult().asString();

		assertNotNull(result);
		String key1 = null;
		String key2 = null;

		//It's a bit random how the map is sorted.. sometimes key2 appears before key1
		BufferedReader bufReader = new BufferedReader(new StringReader(result));
		String line;
		while( (line=bufReader.readLine()) != null ) {
			if(line.indexOf("key1") > 0)
				key1 = line.trim();
			if(line.indexOf("key2") > 0)
				key2 = line.trim();
		}

		assertEquals("<item name=\"key1\">value1</item>", key1);
		assertEquals("<item name=\"key2\">value2</item>", key2);
	}
}
