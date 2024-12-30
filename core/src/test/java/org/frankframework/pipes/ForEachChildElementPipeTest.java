package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.IteratingPipe.StopReason;
import org.frankframework.senders.EchoSender;
import org.frankframework.senders.XsltSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.AppConstants;
import org.frankframework.util.XmlUtils;

public class ForEachChildElementPipeTest extends PipeTestBase<ForEachChildElementPipe> {

	private final boolean TEST_CDATA = false;
	private final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private final String CDATA_END = TEST_CDATA ? "]]>" : "";

	private final String messageBasicNoNS = "<root><sub>A &amp; B</sub><sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub><sub name=\"r\">R</sub></root>";
	private final String messageBasicNoNSLong = "<root><sub>A &amp; B</sub><sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub><sub>data</sub><sub>data</sub><sub>data</sub><sub>data</sub></root>";
	private final String messageBasicNS1 = "<root xmlns=\"urn:test\"><sub>A &amp; B</sub><sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub><sub name=\"r\">R</sub></root>";
	private final String messageBasicNS2 = "<ns:root xmlns:ns=\"urn:test\"><ns:sub>A &amp; B</ns:sub><ns:sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</ns:sub><ns:sub name=\"r\">R</ns:sub></ns:root>";
	private final String messageError = "<root><sub name=\"a\">B</sub><sub>error</sub><sub>tail</sub></root>";

	private final String expectedBasicNoNS = "<results>\n" +
		"<result item=\"1\">\n" +
		"<sub>A &amp; B</sub>\n" +
		"</result>\n" +
		"<result item=\"2\">\n" +
		"<sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub>\n" +
		"</result>\n" +
		"<result item=\"3\">\n" +
		"<sub name=\"r\">R</sub>\n" +
		"</result>\n</results>";

	private final String expectedBasicNoNSFirstElement = """
		<results>
		<result item="1">
		<sub>A &amp; B</sub>
		</result>
		</results>\
		""";

	private final String expectedBasicNoNSFirstTwoElements = "<results>\n" +
		"<result item=\"1\">\n" +
		"<sub>A &amp; B</sub>\n" +
		"</result>\n" +
		"<result item=\"2\">\n" +
		"<sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub>\n" +
		"</result>\n</results>";

	private final String expectedBasicNS1 = "<results>\n" +
		"<result item=\"1\">\n" +
		"<sub xmlns=\"urn:test\">A &amp; B</sub>\n" +
		"</result>\n" +
		"<result item=\"2\">\n" +
		"<sub name=\"p &amp; Q\" xmlns=\"urn:test\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub>\n" +
		"</result>\n" +
		"<result item=\"3\">\n" +
		"<sub name=\"r\" xmlns=\"urn:test\">R</sub>\n" +
		"</result>\n</results>";

	private final String expectedBasicNS2 = "<results>\n" +
		"<result item=\"1\">\n" +
		"<ns:sub xmlns:ns=\"urn:test\">A &amp; B</ns:sub>\n" +
		"</result>\n" +
		"<result item=\"2\">\n" +
		"<ns:sub name=\"p &amp; Q\" xmlns:ns=\"urn:test\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</ns:sub>\n" +
		"</result>\n" +
		"<result item=\"3\">\n" +
		"<ns:sub name=\"r\" xmlns:ns=\"urn:test\">R</ns:sub>\n" +
		"</result>\n</results>";

	private final PipeLineSession session = new PipeLineSession();

	@Override
	public ForEachChildElementPipe createPipe() {
		return new ForEachChildElementPipe();
	}

	protected ElementRenderer getElementRenderer() {
		return new ElementRenderer(null, null);
	}

	protected ElementRenderer getElementRenderer(final Exception e) {
		return new ElementRenderer(null, e);
	}

	protected ElementRenderer getElementRenderer(final SwitchCounter sc) {
		return new ElementRenderer(sc, null);
	}

	protected ElementRenderer getElementRenderer(final SwitchCounter sc, final Exception e) {
		return new ElementRenderer(sc, e);
	}

	private class ElementRenderer extends EchoSender {

		public SwitchCounter sc;
		public Exception e;
		public int callCounter;

		ElementRenderer(SwitchCounter sc, Exception e) {
			super();
			this.sc=sc;
			this.e=e;
		}

		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
			callCounter++;
			if (sc!=null) sc.mark("out");
			try {
				if (message.asString().contains("error")) {
					if (e!=null) {
						if (e instanceof SenderException exception) {
							throw exception;
						}
						if (e instanceof TimeoutException exception) {
							throw exception;
						}
						if (e instanceof RuntimeException exception) {
							throw exception;
						}
					}
					throw new SenderException("Exception triggered", e);
				}
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(),e);
			}
			return super.sendMessage(message, session);
		}

	}

	@Test
	public void testBasic() throws Exception {
		pipe.setSender(getElementRenderer());
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNoNS, session);
		String actual = prr.getResult().asString();
		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testJsonInputWithXsltV3() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setXsltVersion(3);
		pipe.setStyleSheetName("/ForEachChildElementPipe/xslt3.0_test.xsl");
		configurePipe();
		pipe.start();

		String messageBasicJson = "<root><sub><data>{\"children\":{\"child\":\"1\"}}</data></sub><sub><data>{\"children\":{\"child\":\"2\"}}</data></sub><sub><data>{\"children\":{\"child\":\"3\"}}</data></sub></root>";
		PipeRunResult prr = doPipe(pipe, messageBasicJson, session);
		String actual = prr.getResult().asString();

		String expectedBasicJsonConversion = """
				<results>
				<result item="1">
				<data><children><child>1</child></children></data>
				</result>
				<result item="2">
				<data><children><child>2</child></children></data>
				</result>
				<result item="3">
				<data><children><child>3</child></children></data>
				</result>
				</results>\
				""";
		assertEquals(expectedBasicJsonConversion, actual);
	}

	@Test
	public void testBlockSize() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setBlockSize(2);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNoNS, session);
		String actual = prr.getResult().asString();

		String expectedBasicNoNSBlock = "<results>\n" +
				"<result item=\"1\">\n" +
				"<block><sub>A &amp; B</sub><sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub></block>\n" +
				"</result>\n" +
				"<result item=\"2\">\n" +
				"<block><sub name=\"r\">R</sub></block>\n" +
				"</result>\n</results>";
		assertEquals(expectedBasicNoNSBlock, actual);
	}

	@Test
	public void testBlockSize1() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setBlockSize(1);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNoNS, session);
		String actual = prr.getResult().asString();

		String expectedBasicNoNSBlockSize1 = "<results>\n" +
				"<result item=\"1\">\n" +
				"<block><sub>A &amp; B</sub></block>\n" +
				"</result>\n" +
				"<result item=\"2\">\n" +
				"<block><sub name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub></block>\n" +
				"</result>\n" +
				"<result item=\"3\">\n" +
				"<block><sub name=\"r\">R</sub></block>\n" +
				"</result>\n</results>";
		assertEquals(expectedBasicNoNSBlockSize1, actual);
	}

	@Test
	public void testErrorBlock() throws Exception {
		Exception targetException = new NullPointerException("FakeException");
		ElementRenderer er = getElementRenderer(targetException) ;
		pipe.setSender(er);
		pipe.setBlockSize(10);
		configurePipe();
		pipe.start();

		try {
			doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("(NullPointerException) FakeException"));
			assertCauseChainEndsAtOriginalException(targetException,e);
			assertEquals(1, er.callCounter);
		}
	}

	@Test
	public void testError() throws Exception {
		Exception targetException = new NullPointerException("FakeException");
		pipe.setSender(getElementRenderer(targetException));
		configurePipe();
		pipe.start();

		try {
			doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("(NullPointerException) FakeException"));
			assertCauseChainEndsAtOriginalException(targetException,e);
		}
	}

	@Test
	public void testErrorXpath() throws Exception {
		Exception targetException = new NullPointerException("FakeException");
		pipe.setSender(getElementRenderer(targetException));
		pipe.setElementXPathExpression("/root/sub");
		configurePipe();
		pipe.start();

		try {
			doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("(NullPointerException) FakeException"));
			assertCauseChainEndsAtOriginalException(targetException,e);
		}
	}

	@Test
	public void testTimeout() throws Exception {
		Exception targetException = new TimeoutException("FakeTimeout");
		pipe.setSender(getElementRenderer(targetException));
		configurePipe();
		pipe.start();

		try {
			doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("FakeTimeout"));
			assertCauseChainEndsAtOriginalException(targetException,e);
		}
	}

	@Test
	public void testTimeoutXpath() throws Exception {
		Exception targetException = new TimeoutException("FakeTimeout");
		pipe.setSender(getElementRenderer(targetException));
		pipe.setElementXPathExpression("/root/sub");
		configurePipe();
		pipe.start();

		try {
			doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("FakeTimeout"));
			assertCauseChainEndsAtOriginalException(targetException,e);
		}
	}

	private void assertCauseChainEndsAtOriginalException(Exception expectedCause,Exception actual) {
		//actual.printStackTrace();
		Throwable cause=actual;
		while (cause.getCause()!=null) {
			cause=cause.getCause();
		}
		//cause.printStackTrace();
		assertEquals(expectedCause, cause, "cause chain should continue up to original exception");
	}

	@Test
	public void testBasicRemoveNamespacesNonPrefixed() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setRemoveNamespaces(true);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNS1, session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testBasicRemoveNamespacesPrefixed() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setRemoveNamespaces(true);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNS2, session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testBasicNoRemoveNamespacesNonPrefixed() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setRemoveNamespaces(false);
		pipe.setNamespaceDefs("ns=urn:test");
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNS1, session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS1, actual);
	}

	@Test
	public void testBasicNoRemoveNamespacesPrefixed() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setRemoveNamespaces(false);
		pipe.setNamespaceDefs("ns=urn:test");
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNS2, session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS2, actual);
	}


	@Test
	public void testXPath() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/root/sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testXPathWithParameter() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/root/sub[@name=$param]");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.addParameter(new Parameter("param","r"));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		String expectedBasicOnlyR = """
				<results>
				<result item="1">
				<sub name="r">R</sub>
				</result>
				</results>\
				""";
		assertEquals(expectedBasicOnlyR, actual);
	}

	@Test
	public void testXPathRemoveNamespacesNonPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/ns:root/ns:sub");
		pipe.setNamespaceDefs("ns=urn:test");
		pipe.setRemoveNamespaces(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testXPathRemoveNamespacesPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/ns:root/ns:sub");
		pipe.setNamespaceDefs("ns=urn:test");
		pipe.setRemoveNamespaces(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testXPathNoRemoveNamespacesNonPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/*[local-name()='root']/*[local-name()='sub']");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS1, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testXPathNoRemoveNamespacesPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/*[local-name()='root']/*[local-name()='sub']");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS2, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testXPathNoRemoveNamespacesWithNamespaceDefs() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/nstest:root/nstest:sub");
		pipe.setNamespaceDefs("nstest=urn:test");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS1, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testXPathWithSpecialChars() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/root/sub[position()<3]");
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testContainerElement() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setContainerElement("root");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		String wrappedMessage = "<envelope><x>" + messageBasicNoNS + "</x></envelope>";

		ByteArrayInputStream bais = new ByteArrayInputStream(wrappedMessage.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testContaineElementRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setContainerElement("root");
		pipe.setRemoveNamespaces(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		String wrappedMessage = "<envelope><x>" + messageBasicNS1 + "</x></envelope>";

		ByteArrayInputStream bais = new ByteArrayInputStream(wrappedMessage.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testContaineElementNoRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setContainerElement("root");
		pipe.setNamespaceDefs("urn:test");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		String wrappedMessage = "<envelope><x>" + messageBasicNS1 + "</x></envelope>";

		ByteArrayInputStream bais = new ByteArrayInputStream(wrappedMessage.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS1, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}


	@Test
	public void testTargetElement() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testTargetElementRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNS, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testTargetElementNoRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS1, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testTargetElementNoRemoveNamespacesDuplicateNamespaceDefsDefaultNamespace() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();


		String messageDuplNamespace1 = "<root xmlns=\"urn:test\"><header xmlns=\"urn:header\">x</header><sub xmlns=\"urn:test\">A &amp; B</sub><sub xmlns=\"urn:test\" name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub><sub xmlns=\"urn:test\" name=\"r\">R</sub></root>";
		ByteArrayInputStream bais = new ByteArrayInputStream(messageDuplNamespace1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS1, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testTargetElementNoRemoveNamespacesDuplicateNamespaceDefsPrefixedNamespace() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();


		String messageDuplNamespace2 = "<ns:root xmlns:ns=\"urn:test\"><header xmlns=\"urn:header\">x</header><ns:sub xmlns:ns=\"urn:test\">A &amp; B</ns:sub><ns:sub xmlns:ns=\"urn:test\" name=\"p &amp; Q\">" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</ns:sub><ns:sub xmlns:ns=\"urn:test\" name=\"r\">R</ns:sub></ns:root>";
		ByteArrayInputStream bais = new ByteArrayInputStream(messageDuplNamespace2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNS2, actual);
		assertTrue(sc.count > 2, "streaming failure: switch count [" + sc.count + "] should be larger than 2");
	}

	@Test
	public void testBasicWithStopExpression() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setStopConditionXPathExpression("*[@name='p & Q']");
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(17));
		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
	}

	@Test
	public void testBasicWithStopExpressionAndForwardName() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setStopConditionXPathExpression("*[@name='p & Q']");
		pipe.addForward(new PipeForward(StopReason.STOP_CONDITION_MET.getForwardName(), "dummy"));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(17));
		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		assertEquals(StopReason.STOP_CONDITION_MET.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void testBasicWithStopExpressionAndNotRegistered() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setStopConditionXPathExpression("*[@name='p & Q']");
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(17));
		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void testBasicMaxItems1() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setMaxItems(1);
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNSFirstElement, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(10));
	}

	@Test
	public void testBasicMaxItems1AndForwardName() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setMaxItems(1);
		pipe.addForward(new PipeForward(StopReason.MAX_ITEMS_REACHED.getForwardName(), "dummy"));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNSFirstElement, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(10));
		assertEquals(StopReason.MAX_ITEMS_REACHED.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void testBasicMaxItems2() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setMaxItems(2);
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNSLong.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(15));
	}

	@Test
	public void testBasicMaxItems2AndForwardName() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setMaxItems(2);
		pipe.addForward(new PipeForward(StopReason.MAX_ITEMS_REACHED.getForwardName(), "dummy"));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNSLong.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(15));
		assertEquals(StopReason.MAX_ITEMS_REACHED.getForwardName(), prr.getPipeForward().getName());
	}

	@Test
	public void testBasicMaxItems2AndNotRegisteredForward() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setSender(getElementRenderer());
		pipe.setMaxItems(2);
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNSLong.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(15));
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void testTargetElementMaxItems1() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.setMaxItems(1);
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNSLong.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

//		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(11));
		assertEquals(expectedBasicNoNSFirstElement, actual);
	}

	@Test
	public void testTargetElementMaxItems2() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.setMaxItems(2);
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNSLong.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

//		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(20));
		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
	}

	@Test
	public void testNamespacedTargetElement1() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.setNamespaceDefs("urn:test");
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

//		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
//        assertThat(sc.hitCount.get("in"), Matchers.lessThan(11));
		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testNamespacedTargetElement2() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.setNamespaceDefs("urn:test");
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

//		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
//        assertThat(sc.hitCount.get("in"), Matchers.lessThan(11));
		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testPrefixedNamespacedTargetElement1() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("x:sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.setNamespaceDefs("x=urn:test");
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

//		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
//        assertThat(sc.hitCount.get("in"), Matchers.lessThan(11));
		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testPrefixedNamespacedTargetElement2() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("x:sub");
		// pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		pipe.setNamespaceDefs("x=urn:test");
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = prr.getResult().asString();

//		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
//        assertThat(sc.hitCount.get("in"), Matchers.lessThan(11));
		assertEquals(expectedBasicNoNS, actual);
	}


	@Test
	public void testNoDuplicateNamespaces() throws Exception, IOException {
		pipe.setSender(getElementRenderer());
		pipe.setTargetElement("XDOC");
		pipe.setRemoveNamespaces(false);
		configurePipe();
		pipe.start();

		String input = TestFileUtils.getTestFile("/ForEachChildElementPipe/xdocs.xml");
		String expected = TestFileUtils.getTestFile("/ForEachChildElementPipe/ForEachChildElementPipe-Result.txt");
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	public void testBulk2() throws Exception, IOException {
		pipe.setSender(getElementRenderer());
		pipe.setTargetElement("XDOC");
		pipe.setBlockSize(4);
		pipe.setRemoveNamespaces(false);
		configurePipe();
		pipe.start();

		String input = TestFileUtils.getTestFile("/ForEachChildElementPipe/bulk2.xml");
		String expected = TestFileUtils.getTestFile("/ForEachChildElementPipe/bulk2out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	public void testBulk2Parallel() throws Exception, IOException {
		pipe.setSender(getElementRenderer());
		pipe.setTargetElement("XDOC");
		pipe.setBlockSize(4);
		pipe.setParallel(true);
		pipe.setTaskExecutor(new ConcurrentTaskExecutor());
		pipe.setMaxChildThreads(2);
		pipe.setRemoveNamespaces(false);
		configurePipe();
		pipe.start();

		String input = TestFileUtils.getTestFile("/ForEachChildElementPipe/bulk2.xml");
		String expected = TestFileUtils.getTestFile("/ForEachChildElementPipe/bulk2out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}


	@Test
	public void testRemoveNamespacesInAttributes() throws Exception, IOException {
		pipe.setSender(getElementRenderer());
		pipe.setTargetElement("XDOC");
		configurePipe();
		pipe.start();

		String input = TestFileUtils.getTestFile("/ForEachChildElementPipe/NamespaceCaseIn.xml");
		String expected = TestFileUtils.getTestFile("/ForEachChildElementPipe/NamespaceCaseOut.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	@Test
	public void testNamespacedXPath() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setElementXPathExpression("//x:directoryUrl");
		pipe.setNamespaceDefs("x=http://studieData.nl/schema/edudex/directory");
		configurePipe();
		pipe.start();

		String input = TestFileUtils.getTestFile("/ForEachChildElementPipe/NamespacedXPath/input.xml");
		String expected = TestFileUtils.getTestFile("/ForEachChildElementPipe/NamespacedXPath/expected.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String actual = prr.getResult().asString();

		assertEquals(expected, actual);
	}

	//The XmlFileElementIteratorPipe has been deprecated, elementName has been replaced by targetElement.
	//This test proves that the old method works with the ForEachChildElemenPipe
	@Test
	public void testElementName() throws Exception {
		XsltSender sender = new XsltSender();
		sender.setXpathExpression("concat(Person/PersonName/Id,'_',Person/Demographics/Gender)");
		pipe.setSender(sender);
		pipe.setTargetElement("Person");
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/XmlFileElementIteratorPipe/input.xml");
		String expected = TestFileUtils.getTestFile("/XmlFileElementIteratorPipe/ElementNameOutput.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	// The XmlFileElementIteratorPipe has been deprecated, elementChain has been replaced with targetElement + containerElement.
	// This test proves that the old method works with the ForEachChildElemenPipe.
	// The xPath in the sender was adjusted to match the targetElement
	@Test
	public void testElementChain() throws Exception {
		XsltSender sender = new XsltSender();
		sender.setXpathExpression("concat(Party/Person/PersonName/Id,'_',Party/Person/Demographics/Gender)");
		pipe.setSender(sender);
		pipe.setContainerElement("PartyInternalAgreementRole");
		pipe.setTargetElement("Party");
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/XmlFileElementIteratorPipe/input.xml");
		String expected = TestFileUtils.getTestFile("/XmlFileElementIteratorPipe/ElementChainOutput.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	private class SwitchCounter {
		public int count;
		private String prevLabel;
		public Map<String,Integer> hitCount = new HashMap<>();

		public void mark(String label) {
			if (prevLabel==null || !prevLabel.equals(label)) {
				prevLabel=label;
				count++;
			}
			Integer hits=hitCount.get(label);
			if (hits == null) {
				hitCount.put(label,1);
			} else {
				hitCount.put(label,hits+1);
			}
		}
	}


	private class LoggingInputStream extends FilterInputStream {

		private final int blockSize = 10;
		private final SwitchCounter sc;

		public LoggingInputStream(InputStream arg0, SwitchCounter sc) {
			super(arg0);
			this.sc=sc;
		}

		private void print(String string) {
			log.debug("in["+sc.hitCount.get("in")+"]-> "+string);
			sc.mark("in");
		}

		@Override
		public int read() throws IOException {
			int c=super.read();
			print("in-> ["+((char)c)+"]");
			return c;
		}

		@Override
		public int read(byte[] buf, int off, int len) throws IOException {
			int l=super.read(buf, off, Math.min(len, blockSize));
			if (l<0) {
				print("{EOF}");
			} else {
				print(new String(buf,off,l));
			}
			return l;
		}

		@Override
		public int read(byte[] buf) throws IOException {
			if (buf.length> blockSize) {
				return read(buf,0, blockSize);
			}
			int l=super.read(buf);
			if (l<0) {
				print("{EOF}");
			} else {
				print(new String(buf,0,l));
			}
			return l;
		}
	}
}
