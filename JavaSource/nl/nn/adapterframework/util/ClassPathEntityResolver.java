/*
 * $Log: ClassPathEntityResolver.java,v $
 * Revision 1.1  2004-06-16 06:57:44  NNVZNL01#L180564
 * Initial version
 *
 *
 */
package nl.nn.adapterframework.util;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

	/**
	 * @author Frits Berger RSD SF OJM
	 * @see org.xml.sax.EntityResolver
	 * This class offers the resolveEntity method to resolve a systemId to
	 * a resource on the classpath.
	 * @since 4.1.1
	 * @version Id
	 */

public class ClassPathEntityResolver implements EntityResolver {

	public ClassPathEntityResolver() {
		super();
	}

	public static final String version = "$Id: ClassPathEntityResolver.java,v 1.1 2004-06-16 06:57:44 NNVZNL01#L180564 Exp $";

	protected Logger log = Logger.getLogger(this.getClass());

	/**
	 * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
	 */
	public InputSource resolveEntity(String publicId, String systemId)
		throws SAXException, IOException {
		InputSource inputSource = null;

		try {
			// strip any file info from systemId 
			int idx = systemId.lastIndexOf("/");
			if (idx >= 0)
				systemId = systemId.substring(idx + 1);
			InputStream inputStream =
				this.getClass().getResourceAsStream("/" + systemId);
			inputSource = new InputSource(inputStream);
			log.debug("Resolving " + systemId + " to /" + systemId);
		} catch (Exception e) {
			log.error(e);
			// No action; just let the null InputSource pass through
		}

		// If nothing found, null is returned, for normal processing
		return inputSource;
	}

}
