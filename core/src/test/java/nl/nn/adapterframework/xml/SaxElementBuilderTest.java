package nl.nn.adapterframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class SaxElementBuilderTest {

	@Test
	public void buildDocument() throws SAXException {
		String expected = "<root attr1=\"1234\" attr2=\"a b  c d e f   g\" attr3=\"abc\" attr4=\"def\"><sub1>sub1Value</sub1><sub2 attr=\"a b  c d e f   g\">sub2value</sub2></root>";
		SaxElementBuilder root = new SaxElementBuilder("root");
		try {
			root.addAttribute("attr1", 1234);
			root.addAttribute("attr2", "a b  c\td\re\nf\r\n\t\ng");

			Map<String,String> attrs = new LinkedHashMap<>();
			attrs.put("attr3", "abc");
			attrs.put("attr4", "def");
			root.addAttributes(attrs);

			root.addElement("sub1", "sub1Value");
			root.addElement("sub2", "attr", "a b  c\td\re\nf\r\n\t\ng", "sub2value");
		} finally {
			root.close();
		}
		assertEquals(expected, root.toString());
	}
}
