package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.xml.FullXmlFilter;

public class ForEachChildElementPipeTest extends StreamingPipeTestBase<ForEachChildElementPipe> {

	private boolean TEST_CDATA=false;
	private String CDATA_START=TEST_CDATA?"<![CDATA[":"";
	private String CDATA_END=TEST_CDATA?"]]>":"";

	private String messageBasicNoNS="<root><sub>A &amp; B</sub><sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub><sub name=\"r\">R</sub></root>";
	private String messageBasicNoNSLong="<root><sub>A &amp; B</sub><sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub><sub>data</sub><sub>data</sub><sub>data</sub><sub>data</sub></root>";
	private String messageBasicNS1="<root xmlns=\"urn:test\"><sub>A &amp; B</sub><sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub><sub name=\"r\">R</sub></root>";
	private String messageBasicNS2="<ns:root xmlns:ns=\"urn:test\"><ns:sub>A &amp; B</ns:sub><ns:sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</ns:sub><ns:sub name=\"r\">R</ns:sub></ns:root>";
	private String messageError="<root><sub name=\"a\">B</sub><sub>error</sub><sub>tail</sub></root>";
	private String messageDuplNamespace1="<root xmlns=\"urn:test\"><header xmlns=\"urn:header\">x</header><sub xmlns=\"urn:test\">A &amp; B</sub><sub xmlns=\"urn:test\" name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub><sub xmlns=\"urn:test\" name=\"r\">R</sub></root>";
	private String messageDuplNamespace2="<ns:root xmlns:ns=\"urn:test\"><header xmlns=\"urn:header\">x</header><ns:sub xmlns:ns=\"urn:test\">A &amp; B</ns:sub><ns:sub xmlns:ns=\"urn:test\" name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</ns:sub><ns:sub xmlns:ns=\"urn:test\" name=\"r\">R</ns:sub></ns:root>";

	private String expectedBasicNoNS="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub>A &amp; B</sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub>\n"+
			"</result>\n"+
			"<result item=\"3\">\n"+
			"<sub name=\"r\">R</sub>\n"+
			"</result>\n</results>";

	private String expectedBasicNoNSBlock="<results>\n"+
			"<result item=\"1\">\n"+
			"<block><sub>A &amp; B</sub><sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub></block>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<block><sub name=\"r\">R</sub></block>\n"+
			"</result>\n</results>";


	private String expectedBasicNoNSFirstElement="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub>A &amp; B</sub>\n"+
			"</result>\n</results>";

	private String expectedBasicNoNSFirstTwoElements="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub>A &amp; B</sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<sub name=\"p &amp; Q\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub>\n"+
			"</result>\n</results>";

	private String expectedBasicNS1="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub xmlns=\"urn:test\">A &amp; B</sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<sub name=\"p &amp; Q\" xmlns=\"urn:test\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub>\n"+
			"</result>\n"+
			"<result item=\"3\">\n"+
			"<sub name=\"r\" xmlns=\"urn:test\">R</sub>\n"+
			"</result>\n</results>";
	private String expectedBasicNS2="<results>\n"+
			"<result item=\"1\">\n"+
			"<ns:sub xmlns:ns=\"urn:test\">A &amp; B</ns:sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<ns:sub name=\"p &amp; Q\" xmlns:ns=\"urn:test\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</ns:sub>\n"+
			"</result>\n"+
			"<result item=\"3\">\n"+
			"<ns:sub name=\"r\" xmlns:ns=\"urn:test\">R</ns:sub>\n"+
			"</result>\n</results>";

	private IPipeLineSession session = new PipeLineSessionBase();

	@Override
	public ForEachChildElementPipe createPipe() {
		return new ForEachChildElementPipe();
	}

	protected ISender getElementRenderer() {
		return getElementRenderer(null, null);
	}

	protected ISender getElementRenderer(final Exception e) {
		return getElementRenderer(null, e);
	}

	protected ISender getElementRenderer(final SwitchCounter sc) {
		return getElementRenderer(sc, null);
	}

	protected ISender getElementRenderer(final SwitchCounter sc, final Exception e) {
		EchoSender sender = new EchoSender() {

			@Override
			public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
				if (sc!=null) sc.mark("out");
				try {
					if (message.asString().contains("error")) {
						if (e!=null) {
							if (e instanceof SenderException) {
								throw (SenderException)e;
							}
							if (e instanceof TimeOutException) {
								throw (TimeOutException)e;
							}
							if (e instanceof RuntimeException) {
								throw (RuntimeException)e;
							}
						}
						throw new SenderException("Exception triggered", e);
					}
				} catch (IOException e) {
					throw new SenderException(getLogPrefix(),e);
				}
				return super.sendMessage(message, session);
			}

		};
		return sender;
	}

	@Override
	public void setup() throws ConfigurationException {
		assumeFalse(provideStreamForInput);
		super.setup();
	}
	
	@Test
	public void testBasic() throws Exception {
		pipe.setSender(getElementRenderer());
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNoNS, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testBlockSize() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setBlockSize(2);
		pipe.setBlockPrefix("<block>");
		pipe.setBlockSuffix("</block>");
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNoNS, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNSBlock, actual);
	}

	@Test
	public void testError() throws Exception {
		Exception targetException = new NullPointerException("FakeException");
		pipe.setSender(getElementRenderer(targetException));
		configurePipe();
		pipe.start();

		try {
			PipeRunResult prr = doPipe(pipe, messageError, session);
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
			PipeRunResult prr = doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("(NullPointerException) FakeException"));
			assertCauseChainEndsAtOriginalException(targetException,e);
		}
	}

	@Test
	public void testTimeout() throws Exception {
		Exception targetException = new TimeOutException("FakeTimeout");
		pipe.setSender(getElementRenderer(targetException));
		configurePipe();
		pipe.start();

		try {
			PipeRunResult prr = doPipe(pipe, messageError, session);
			fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("FakeTimeout"));
			assertCauseChainEndsAtOriginalException(targetException,e);
		}
	}

	@Test
	public void testTimeoutXpath() throws Exception {
		Exception targetException = new TimeOutException("FakeTimeout");
		pipe.setSender(getElementRenderer(targetException));
		pipe.setElementXPathExpression("/root/sub");
		configurePipe();
		pipe.start();

		try {
			PipeRunResult prr = doPipe(pipe, messageError, session);
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
		assertEquals("cause chain should continue up to original exception",expectedCause, cause);
	}

	@Test
	public void testBasicRemoveNamespacesNonPrefixed() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setRemoveNamespaces(true);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNS1, session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
	}

	@Test
	public void testBasicRemoveNamespacesPrefixed() throws Exception {
		pipe.setSender(getElementRenderer());
		pipe.setRemoveNamespaces(true);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, messageBasicNS2, session);
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testXPathRemoveNamespacesNonPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/ns:root/ns:sub");
		pipe.setNamespaceDefs("ns=urn:test");
		pipe.setRemoveNamespaces(true);
		pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testXPathRemoveNamespacesPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/ns:root/ns:sub");
		pipe.setNamespaceDefs("ns=urn:test");
		pipe.setRemoveNamespaces(true);
		pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testXPathNoRemoveNamespacesNonPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/*[local-name()='root']/*[local-name()='sub']");
		pipe.setNamespaceAware(false);
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS1, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testXPathNoRemoveNamespacesPrefixed() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/*[local-name()='root']/*[local-name()='sub']");
		pipe.setNamespaceAware(false);
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS2, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS1, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testXPathWithSpecialChars() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setElementXPathExpression("/root/sub[position()<3]");
		pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		assumeTrue("Streaming XSLT switched off", AppConstants.getInstance().getBoolean("xslt.streaming.default", true));
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testContaineElementRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setContainerElement("root");
		pipe.setRemoveNamespaces(true);
		pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		String wrappedMessage = "<envelope><x>" + messageBasicNS1 + "</x></envelope>";

		ByteArrayInputStream bais = new ByteArrayInputStream(wrappedMessage.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testContaineElementNoRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setContainerElement("root");
		pipe.setNamespaceDefs("urn:test");
		pipe.setNamespaceAware(false);
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		String wrappedMessage = "<envelope><x>" + messageBasicNS1 + "</x></envelope>";

		ByteArrayInputStream bais = new ByteArrayInputStream(wrappedMessage.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS1, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testTargetElementRemoveNamespaces() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(true);
		pipe.setNamespaceAware(true);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS1, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testTargetElementNoRemoveNamespacesDuplicateNamespaceDefsDefaultNamespace() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		
		ByteArrayInputStream bais = new ByteArrayInputStream(messageDuplNamespace1.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS1, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
	}

	@Test
	public void testTargetElementNoRemoveNamespacesDuplicateNamespaceDefsPrefixedNamespace() throws Exception {
		SwitchCounter sc = new SwitchCounter();
		pipe.setTargetElement("sub");
		pipe.setRemoveNamespaces(false);
		pipe.setSender(getElementRenderer(sc));
		configurePipe();
		pipe.start();

		
		ByteArrayInputStream bais = new ByteArrayInputStream(messageDuplNamespace2.getBytes());
		PipeRunResult prr = doPipe(pipe, new LoggingInputStream(bais, sc), session);
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNS2, actual);
		assertTrue("streaming failure: switch count [" + sc.count + "] should be larger than 2", sc.count > 2);
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
		String actual = Message.asString(prr.getResult());

		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(17));
		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNSFirstElement, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(10));
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
		String actual = Message.asString(prr.getResult());

		assertEquals(expectedBasicNoNSFirstTwoElements, actual);
		// System.out.println("num reads="+sc.hitCount.get("in"));
		assertThat(sc.hitCount.get("in"), Matchers.lessThan(15));
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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

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
		String actual = Message.asString(prr.getResult());

		assertEquals(expected, actual);
	}

	
	private class SwitchCounter {
		public int count;
		private String prevLabel;
		public Map<String,Integer> hitCount = new HashMap<String,Integer>();
		
		public void mark(String label) {
			if (prevLabel==null || !prevLabel.equals(label)) {
				prevLabel=label;
				count++;
			}
			Integer hits=hitCount.get(label);
			if (hits==null) {
				hitCount.put(label,1);
			} else {
				hitCount.put(label,hits+1);
			}
		}
	}

	private class SaxLogger extends FullXmlFilter implements ContentHandler {
		
		private String prefix;
		private SwitchCounter sc;
		
		SaxLogger(String prefix, SwitchCounter sc, ContentHandler handler) {
			super(handler);
			this.prefix=prefix;
			this.sc=sc;
		}
		private void print(String string) {
			log.debug(prefix+" "+string);
			sc.mark(prefix);
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			print(new String(ch,start,length));
			super.characters(ch, start, length);
		}

		@Override
		public void startDocument() throws SAXException {
			print("startDocument");
			super.startDocument();
		}

		@Override
		public void endDocument() throws SAXException {
			print("endDocument");
			super.endDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			print("startElement "+localName);
			super.startElement(uri, localName, qName, attributes);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			print("endElement "+localName);
			super.endElement(uri, localName, qName);
		}

		
	}

	private class LoggingInputStream extends FilterInputStream {

		private int blocksize=10;
		private SwitchCounter sc;
		
		public LoggingInputStream(InputStream arg0, SwitchCounter sc) {
			super(arg0);
			this.sc=sc;
		}

		private void print(String string) {
			log.debug("in-> "+string);
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
			int l=super.read(buf, off, len<blocksize?len:blocksize);
			if (l<0) {
				print("{EOF}");
			} else {
				print(new String(buf,off,l));
			}
			return l;
		}

		@Override
		public int read(byte[] buf) throws IOException {
			if (buf.length>blocksize) {
				return read(buf,0,blocksize);
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