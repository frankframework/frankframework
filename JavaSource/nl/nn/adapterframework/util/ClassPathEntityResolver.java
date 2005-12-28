/*
 * $Log: ClassPathEntityResolver.java,v $
 * Revision 1.2  2005-12-28 08:28:45  europe\L190409
 * improved handling of malformed URLs
 *
 * Revision 1.1  2004/06/16 06:57:44  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version
 *
 *
 */
package nl.nn.adapterframework.util;
import java.io.IOException;
import java.net.URL;

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
	public static final String version="$RCSfile: ClassPathEntityResolver.java,v $ $Revision: 1.2 $ $Date: 2005-12-28 08:28:45 $";
	protected Logger log = Logger.getLogger(this.getClass());



	/**
	 * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
	 */
	public InputSource resolveEntity(String publicId, String systemId)
		throws SAXException, IOException {
		InputSource inputSource = null;

		String classPathEntityUrl = systemId;
	
		// strip any file info from systemId 
		int idx = classPathEntityUrl.lastIndexOf("/");
		if (idx >= 0) {
			classPathEntityUrl = classPathEntityUrl.substring(idx + 1);
		}
		classPathEntityUrl = "/" + classPathEntityUrl;
		try {
			log.debug("Resolving [" + systemId + "] to [" + classPathEntityUrl+"]");
			URL url = ClassUtils.getResourceURL(this,classPathEntityUrl);
			if (url==null) {
				log.error("cannot find resource for ClassPathEntity [" + systemId + "] from Url [" + classPathEntityUrl+"]");
				return null;
			}
			inputSource = new InputSource(url.openStream());
		} catch (Exception e) {
			log.error("Exception resolving ClassPathEntity [" + systemId + "] to [" + classPathEntityUrl+"]",e);
			// No action; just let the null InputSource pass through
		}

		// If nothing found, null is returned, for normal processing
		return inputSource;
	}

}
