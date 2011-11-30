/*
 * $Log: Variant.java,v $
 * Revision 1.8  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2005/06/13 11:49:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use separate version of stringToSource, optimized for single use
 *
 * Revision 1.5  2005/06/13 10:10:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optimized transformation to XmlSource
 *
 * Revision 1.4  2005/05/31 09:39:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * using stringToSource to obtain a source for XSLT; cached this source
 *
 */
package nl.nn.adapterframework.util;

import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import java.io.Reader;
import java.io.StringReader;

/**
 * Class to handle all kinds of conversions.
 * @version Id
 *
 * @author Gerrit van Brakel IOS
 */
public class Variant {
	public static final String version = "$RCSfile: Variant.java,v $ $Revision: 1.8 $ $Date: 2011-11-30 13:51:48 $";

	private String data = null;
	private Source dataAsXmlSource = null;
	
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
		return asXmlSource(true);
	}

	public Source asXmlSource(boolean forMultipleUse) throws DomBuilderException {
		if (!forMultipleUse && dataAsXmlSource==null) {
			return XmlUtils.stringToSourceForSingleUse(data);
		}
		if (dataAsXmlSource==null) {
			dataAsXmlSource = XmlUtils.stringToSource(data);
		}
		return dataAsXmlSource;
	}
}
