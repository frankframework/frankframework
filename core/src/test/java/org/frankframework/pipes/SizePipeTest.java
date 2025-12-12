package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;

public class SizePipeTest extends PipeTestBase<SizePipe>{

	@Override
	public SizePipe createPipe() {
		return new SizePipe();
	}

	/**
	 *
	 * Method: doPipe(Object input, PipeLineSession session)
	 *
	 */
	@Test
	void testDoPipeSuccess() throws Exception {
		PipeRunResult res = doPipe("abcsd");
		assertEquals("5", res.getResult().asString());
	}

	@Test
	void testNullMessage() throws Exception {
		PipeRunResult res = doPipe(Message.nullMessage());
		assertEquals("0", res.getResult().asString());
	}

	@Test
	void testDomDocumentMessage() throws Exception {
		PipeRunResult res = doPipe(new Message(XmlUtils.buildDomDocument(new StringReader("<root/>"))));
		assertEquals("-1", res.getResult().asString());
	}
}
