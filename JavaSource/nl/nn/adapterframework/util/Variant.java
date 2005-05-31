/*
 * $Log: Variant.java,v $
 * Revision 1.4  2005-05-31 09:39:09  europe\L190409
 * using stringToSource to obtain a source for XSLT; cached this source
 *
 */
package nl.nn.adapterframework.util;

import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.Reader;
import java.io.StringReader;

/**
 * Class to handle all kinds of conversions.
 * @version Id
 *
 * @author Gerrit van Brakel IOS
 */
public class Variant {
	public static final String version = "$RCSfile: Variant.java,v $ $Revision: 1.4 $ $Date: 2005-05-31 09:39:09 $";

	private String data = null;
	private DOMSource dataAsDOMSource = null;
	
	public Variant(Object obj) {
		this(obj.toString());
	}
	public Variant(String obj) {
		super();
		data = obj;
	}
	public Reader asReader() {
		return new StringReader(data);
	}
	public String asString() {
		return data;
	}
	
	/**
	 * Renders an InputSource for SAX parsing
	 */
	public InputSource asXmlInputSource() {

		StringReader sr;

		sr = new StringReader(data);

		return new InputSource(sr);
	}

	/**
	 * Renders a Source for XSLT-transformation
	 */
	public Source asXmlSource() throws DomBuilderException {
		if (dataAsDOMSource==null) {
			dataAsDOMSource = (DOMSource)XmlUtils.stringToSource(data,true);
		}
		return dataAsDOMSource;
	}
}
