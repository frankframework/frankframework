/*
 * $Log: XmlExternalEntityResolver.java,v $
 * Revision 1.1  2012-09-24 18:16:04  m00f069
 * Don't resolve external entities in DOCTYPE
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/*
 * Entity resolver which resolves external entities to an empty string. This
 * will prevent the XML parser from downloading resources as specified by URI's
 * in a DOCTYPE.
 * 
 * @author Jaco de Groot
 */
public class XmlExternalEntityResolver implements EntityResolver2 {
	private Logger log = LogUtil.getLogger(this);

	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, java.io.IOException {
		return resolveEntity(null, publicId, null, systemId);
	}

	public InputSource getExternalSubset(String arg0, String arg1)
			throws SAXException, IOException {
		return null;
	}

	public InputSource resolveEntity(String name, String publicId,
			String baseURI, String systemId) throws SAXException, IOException {
		log.debug("Resolving entity with name '" + name + "', public id '"
				+ publicId + "', base uri '" + baseURI + "' and system id '"
				+ systemId + "' to an empty string");
		return new InputSource(new StringReader(""));
	}

}
