/*
 * $Log: LogUtil.java,v $
 * Revision 1.3  2007-08-29 15:13:04  europe\L190409
 * enables use of log4j4ibis.properties
 *
 * Revision 1.2  2007/02/12 15:56:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configure logging hierarchy from log4j.properties
 *
 * Revision 1.1  2007/02/12 14:10:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of LogUtil
 *
 */
package nl.nn.adapterframework.util;

import java.net.URL;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.RootCategory;

/**
 * Convenience functions for logging.
 * Enables a separate log4j configuartion for each Ibis-instance.
 * Searches first for log4j4ibis.properties on the classpath. If not found, then searches for log4j.properties.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class LogUtil {
	public static final String version="$RCSfile: LogUtil.java,v $  $Revision: 1.3 $ $Date: 2007-08-29 15:13:04 $";

	private static Hierarchy hierarchy=null;
	
	
	public static Logger getLogger(String name) { 
		return getHierarchy().getLogger(name);
	}

	public static Logger getLogger(Class clazz) { 
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(Object owner) { 
		return getLogger(owner.getClass());
	}


	public static synchronized Hierarchy getHierarchy() {
		if (hierarchy==null) {
			hierarchy = new Hierarchy(new RootCategory(Level.DEBUG));
			URL url = ClassUtils.getResourceURL(LogUtil.class, "log4j4ibis.properties");
			if (url==null) {
				url = ClassUtils.getResourceURL(LogUtil.class, "log4j.properties");
			}
			if (url==null) {
				System.err.println("Did not find log4j4ibis.properties or log4j.properties on classpath. Cannot configure log4j properly.");
			}
			new PropertyConfigurator().doConfigure(url, hierarchy);
		}
		return hierarchy;
	}

}
