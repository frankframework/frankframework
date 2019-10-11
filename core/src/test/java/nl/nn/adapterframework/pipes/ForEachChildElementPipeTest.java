package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.xml.FullXmlFilter;

public class ForEachChildElementPipeTest extends PipeTestBase<ForEachChildElementPipe> {

	private boolean TEST_CDATA=false;
	private String CDATA_START=TEST_CDATA?"<![CDATA[":"";
	private String CDATA_END=TEST_CDATA?"]]>":"";

	private String messageBasicNoNS="<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub></root>";
	private String messageBasicNoNSLong="<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub><sub>data</sub><sub>data</sub><sub>data</sub><sub>data</sub></root>";
	private String messageBasicNS1="<root xmlns=\"urn:test\"><sub name=\"p &amp; Q\">A &amp; B</sub><sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub></root>";
	private String messageBasicNS2="<ns:root xmlns:ns=\"urn:test\"><ns:sub name=\"p &amp; Q\">A &amp; B</ns:sub><ns:sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</ns:sub></ns:root>";
	private String messageError="<root><sub name=\"a\">B</sub><sub>error</sub><sub>tail</sub></root>";

	private String expectedBasicNoNS="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub name=\"p &amp; Q\">A &amp; B</sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub>\n"+
			"</result>\n</results>";

	private String expectedBasicNoNSFirstElement="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub name=\"p &amp; Q\">A &amp; B</sub>\n"+
			"</result>\n</results>";

	private String expectedBasicNS1="<results>\n"+
			"<result item=\"1\">\n"+
			"<sub name=\"p &amp; Q\" xmlns=\"urn:test\">A &amp; B</sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<sub xmlns=\"urn:test\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub>\n"+
			"</result>\n</results>";
	private String expectedBasicNS2="<results>\n"+
			"<result item=\"1\">\n"+
			"<ns:sub name=\"p &amp; Q\" xmlns:ns=\"urn:test\">A &amp; B</ns:sub>\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"<ns:sub xmlns:ns=\"urn:test\">"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</ns:sub>\n"+
			"</result>\n</results>";

	private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public ForEachChildElementPipe createPipe() {
        return new ForEachChildElementPipe();
    }

    protected ISender getElementRenderer(final SwitchCounter sc) {
    	EchoSender sender = new EchoSender() {

			@Override
			public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
				if (sc!=null) sc.mark("out");
				if (message.contains("error")) {
					throw new SenderException("Exception triggered");
				}
				return super.sendMessage(correlationID, message, prc);
			}
    		
    	};
    	return sender;
    }

    
    @Test
    public void testBasic() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasicNoNS, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
    }

    @Test
    public void testError() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	configurePipe();
    	pipe.start();

    	exception.expectMessage("Exception triggered");
        PipeRunResult prr = pipe.doPipe(messageError, session);
    }

    @Test
    public void testErrorXpath() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	pipe.setElementXPathExpression("/root/sub");
    	configurePipe();
    	pipe.start();

    	exception.expectMessage("Exception triggered");
        PipeRunResult prr = pipe.doPipe(messageError, session);
    }

    @Test
    public void testBasicRemoveNamespacesNonPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	pipe.setRemoveNamespaces(true);
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasicNS1, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
    }

    @Test
    public void testBasicRemoveNamespacesPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	pipe.setRemoveNamespaces(true);
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasicNS2, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
    }

    @Test
    public void testBasicNoRemoveNamespacesNonPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	pipe.setRemoveNamespaces(false);
    	pipe.setNamespaceDefs("ns=urn:test");
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasicNS1, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNS1, actual);
    }

    @Test
    public void testBasicNoRemoveNamespacesPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	pipe.setRemoveNamespaces(false);
    	pipe.setNamespaceDefs("ns=urn:test");
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasicNS2, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNS2, actual);
    }

    @Test
    public void testXPath() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/root/sub");
    	//pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }

    @Test
    public void testXPathRemoveNamespacesNonPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/ns:root/ns:sub");
    	pipe.setNamespaceDefs("ns=urn:test");
    	pipe.setRemoveNamespaces(true);
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }

    @Test
    public void testXPathRemoveNamespacesPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/ns:root/ns:sub");
    	pipe.setNamespaceDefs("ns=urn:test");
    	pipe.setRemoveNamespaces(true);
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }

    @Test
    public void testXPathNoRemoveNamespacesNonPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/*[local-name()='root']/*[local-name()='sub']");
    	pipe.setNamespaceAware(false);
    	pipe.setRemoveNamespaces(false);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNS1, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }
    
    @Test
    public void testXPathNoRemoveNamespacesPrefixed() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/*[local-name()='root']/*[local-name()='sub']");
    	pipe.setNamespaceAware(false);
    	pipe.setRemoveNamespaces(false);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS2.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNS2, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }
    
    @Test
    public void testXPathNoRemoveNamespacesWithNamespaceDefs() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/nstest:root/nstest:sub");
    	pipe.setNamespaceDefs("nstest=urn:test");
    	pipe.setRemoveNamespaces(false);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNS1.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNS1, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }
    
    @Test
    public void testXPathWithSpecialChars() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/root/sub[position()<3]");
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasicNoNS, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }
 
    @Test
    public void testBasicWithStopExpression() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setSender(getElementRenderer(null));
    	pipe.setStopConditionXPathExpression("position()=1");
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNS.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
		//System.out.println("num reads="+sc.hitCount.get("in"));
        assertThat(sc.hitCount.get("in"), Matchers.lessThan(10));
        assertEquals(expectedBasicNoNSFirstElement, actual);
    }

    @Test
    public void testXPathStopExpression() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/root/sub");
    	//pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer(sc));
       	pipe.setStopConditionXPathExpression("position()=1");
        configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasicNoNSLong.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
        assertThat(sc.hitCount.get("in"), Matchers.lessThan(11));
        assertEquals(expectedBasicNoNSFirstElement, actual);
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
		
		SaxLogger(String prefix, SwitchCounter sc) {
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