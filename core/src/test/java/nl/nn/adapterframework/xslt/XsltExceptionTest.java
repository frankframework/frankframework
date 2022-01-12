package nl.nn.adapterframework.xslt;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.FullXmlFilter;
import nl.nn.adapterframework.xml.SaxDocumentBuilder;
import nl.nn.adapterframework.xml.SaxException;
import nl.nn.adapterframework.xml.TransformerFilter;
import nl.nn.adapterframework.xml.XmlWriter;

public class XsltExceptionTest {

	public void testXsltException(boolean expectChildThreads, int tailCount) throws Exception {
		
		String xpathExpression="*/*";
		int xsltVersion = 1;
		TransformerPool tp = TransformerPool.configureTransformer0(null, null, null, xpathExpression, null, "xml", false, null, xsltVersion);
		
		XmlWriter writer = new XmlWriter();
		
		FullXmlFilter filter = new FullXmlFilter(writer) {
			
			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				if (localName.equals("error")) {
					throw new SaxException("Found error");
				}
				super.startElement(uri, localName, qName, atts);
			}
		};
		
		TransformerFilter transformer = tp.getTransformerFilter(null, null, null, expectChildThreads, filter);
		
		try {
			try (SaxDocumentBuilder seb = new SaxDocumentBuilder("root", transformer)) {
				seb.addElement("elem");
				seb.addElement("error");
				for(int i=0; i<tailCount; i++) {
					seb.addElement("elem");
				}
			}
			fail("Expected exception");
		} catch (Exception e) {
			System.out.println("Expected exception: "+e.getMessage());
		}
		System.out.println(writer);
	}
	
	@Test
	public void testXsltException2000() throws Exception {
		testXsltException(XmlUtils.isXsltStreamingByDefault(), 2000);
	}

	@Test
	public void testXsltException0() throws Exception {
		testXsltException(XmlUtils.isXsltStreamingByDefault(), 0);
	}
}
