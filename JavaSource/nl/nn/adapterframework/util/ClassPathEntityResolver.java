/*
 * $Log: ClassPathEntityResolver.java,v $
 * Revision 1.5  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2007/02/12 14:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2005/12/28 08:28:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	public static final String version="$RCSfile: ClassPathEntityResolver.java,v $ $Revision: 1.5 $ $Date: 2011-11-30 13:51:49 $";
	protected Logger log = LogUtil.getLogger(this);



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
