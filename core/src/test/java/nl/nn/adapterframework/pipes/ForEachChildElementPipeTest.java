package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.XsltSender;

public class ForEachChildElementPipeTest extends PipeTestBase<ForEachChildElementPipe> {

	private String messageBasic="<root><sub>abc</sub><sub>def</sub></root>";
	private String expectedBasic="<results count=\"2\">\n"+
			"<result item=\"1\">\n"+
			"abc\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"def\n"+
			"</result>\n</results>";

	private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public ForEachChildElementPipe createPipe() {
        return new ForEachChildElementPipe();
    }

    protected ISender getElementRenderer(final SwitchCounter sc) {
    	XsltSender sender = new XsltSender() {

			@Override
			public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
				if (sc!=null) sc.mark("out");
				return super.sendMessage(correlationID, message, prc);
			}
    		
    	};
    	sender.setXpathExpression("sub");
    	return sender;
    }

    
    @Test
    public void testBasic() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer(null));
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasic, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasic, actual);
    }

    @Test
    public void testXPath() throws PipeRunException, ConfigurationException, PipeStartException {
    	SwitchCounter sc = new SwitchCounter();
    	pipe.setElementXPathExpression("/root/sub");
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer(sc));
    	configurePipe();
    	pipe.start();

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasic.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasic, actual);
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

		ByteArrayInputStream bais = new ByteArrayInputStream(messageBasic.getBytes());    	
        PipeRunResult prr = pipe.doPipe(new LoggingInputStream(bais,sc), session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasic, actual);
		assertTrue("streaming failure: switch count ["+sc.count+"] should be larger than 2",sc.count>2);
    }
    
    
	private class SwitchCounter {
		public int count;
		String prevLabel;
		
		public void mark(String label) {
			if (prevLabel==null || !prevLabel.equals(label)) {
				prevLabel=label;
				count++;
			}
		}
	}

	private class SaxLogger extends XMLFilterImpl implements ContentHandler {
		
		private String prefix;
		private SwitchCounter sc;
		
		SaxLogger(String prefix, SwitchCounter sc) {
			this.prefix=prefix;
			this.sc=sc;
		}
		private void print(String string) {
			System.out.println(prefix+" "+string);
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

		int blocksize=10;
		private SwitchCounter sc;
		
		public LoggingInputStream(InputStream arg0, SwitchCounter sc) {
			super(arg0);
			this.sc=sc;
		}

		private void print(String string) {
			System.out.println("in-> "+string);
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
			if (len>blocksize) {
				len=blocksize;
			}
			int l=super.read(buf, off, len);
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