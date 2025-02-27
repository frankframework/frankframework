package org.frankframework.xslt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.xml.FullXmlFilter;
import org.frankframework.xml.SaxDocumentBuilder;
import org.frankframework.xml.SaxException;
import org.frankframework.xml.TransformerFilter;
import org.frankframework.xml.XmlWriter;

@Log4j2
public class XsltExceptionTest {

	public void testXsltException(int tailCount) throws Exception {

		String xpathExpression="*/*";
		int xsltVersion = 1;
		TransformerPool tp = TransformerPool.configureTransformer0(null, null, xpathExpression, null, OutputType.XML, false, null, xsltVersion);

		XmlWriter writer = new XmlWriter();

		FullXmlFilter filter = new FullXmlFilter(writer) {

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				if ("error".equals(localName)) {
					throw new SaxException("Found error");
				}
				super.startElement(uri, localName, qName, atts);
			}
		};
		try {
			TransformerFilter transformer = tp.getTransformerFilter(filter);

			try (SaxDocumentBuilder seb = new SaxDocumentBuilder("root", transformer, false)) {
				seb.addElement("elem");
				seb.addElement("error");
				for(int i=0; i<tailCount; i++) {
					seb.addElement("elem");
				}
			}
		} catch (Exception e) {
			log.debug(writer);
			assertEquals("<elem/>", writer.toString());
			throw e;
		}
	}

	@Test
	void testXsltException2000() {
		assertThrows(SaxException.class, () -> testXsltException(2000));
	}

	@Test
	void testXsltException0() {
		assertThrows(SaxException.class, () -> testXsltException(0));
	}
}
