package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.util.TransformerPool.OutputType;

public class XsltStreamingTest {

	private class SwitchCounter {
		public int count;
		private String prevLabel;

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


//	@Test()
//	public void testStreamingXsltViaTransformerHandlerAndEvents() throws SAXException, DomBuilderException, TransformerException, IOException {
//		/*
//		 * create transformer
//		 * feed SAX source events
//		 * receive SAX destination events
//		 * received events and source events should be mixed
//		 */
//
//		SwitchCounter sc = new SwitchCounter();
//
//		String xpath="/root/a";
//		TransformerPool tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(null, xpath,"xml", false, null, false, false, null, 1));
//
//		SAXResult result = new SAXResult();
//		SaxLogger resultfilter = new SaxLogger("out>",sc);
//		result.setHandler(resultfilter);
//
//		TransformerHandler handler = tp.getTransformerHandler();
//		handler.setResult(result);
//
//		SaxLogger inputfilter = new SaxLogger("in>",sc);
//
//		inputfilter.setContentHandler(handler);
//
////		tp.transform(source, result, null);
//		inputfilter.startDocument();
//		String uri="";
//		Attributes attributes=new AttributesImpl();
//		inputfilter.startElement(uri, "root", "root", attributes);
//		for(int i=0; i<10; i++) {
//			String localName="a";
//			String qName="a";
//			inputfilter.startElement(uri, localName, qName, attributes);
//			inputfilter.startElement(uri, "b", "b", attributes);
//			inputfilter.endElement(uri, "b", "b");
//			inputfilter.endElement(uri, localName, qName);
//			inputfilter.startElement(uri, "c", "c", attributes);
//			inputfilter.endElement(uri, "c", "c");
//		}
//		inputfilter.endElement(uri, "root", "root");
//		inputfilter.endDocument();
//
//		assertTrue("switch count ["+sc.count+"] should be larger than 2",sc.count>2);
//	}


	private class LoggingInputStream extends FilterInputStream {

		private int blocksize=10;
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

	@Test()
	public void testStreamingXsltViaStreamSource() throws TransformerException, IOException {
		/*
		 * create transformer
		 * feed SAX source events
		 * receive SAX destination events
		 * received events and source events should be mixed
		 */
		Assumptions.assumeTrue(AppConstants.getInstance().getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, true), "Streaming XSLT switched off");
		SwitchCounter sc = new SwitchCounter();

		String xpath="/root/a";
		TransformerPool tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(null, xpath,OutputType.XML, false, null, false, false, null, 1));

		SAXResult result = new SAXResult();
		SaxLogger resultfilter = new SaxLogger("out>", sc);
		result.setHandler(resultfilter);

		String input="<root>";
		for (int i=0;i<5;i++) {
			input+="<a>"+i+"</a>"+"<b>opvulling</b>";
		}
		input+="</root>";

		ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
		Source source = new StreamSource(new LoggingInputStream(bais,sc));

		tp.transform(source, result);
		assertTrue(sc.count>2, "switch count ["+sc.count+"] should be larger than 2");
	}


}
