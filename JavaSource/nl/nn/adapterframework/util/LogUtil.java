/*
 * $Log: LogUtil.java,v $
 * Revision 1.4  2007-08-30 15:11:46  europe\L190409
 * use only hierarchy if log4j4ibis.properties is present
 *
 * Revision 1.3  2007/08/29 15:13:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @author  Jaco de Groot (***@dynasol.nl)
 * @version Id
 */
public class LogUtil {
	public static final String version="$RCSfile: LogUtil.java,v $  $Revision: 1.4 $ $Date: 2007-08-30 15:11:46 $";

	private static Hierarchy hierarchy=null;
	
	static {
		URL url = LogUtil.class.getClassLoader().getResource("log4j4ibis.properties");
		if (url==null) {
			System.err.println("Did not find log4j4ibis.properties, leaving it up to log4j's default initialization procedure: http://logging.apache.org/log4j/docs/manual.html#defaultInit");
		} else {
			hierarchy = new Hierarchy(new RootCategory(Level.DEBUG));
			new PropertyConfigurator().doConfigure(url, hierarchy);
		}
	}

	public static Logger getRootLogger() { 
		if (hierarchy == null) {
			return Logger.getRootLogger();
		} else {
			return hierarchy.getRootLogger();
		}
	}
	
	public static Logger getLogger(String name) { 
		Logger logger = null;
		if (hierarchy == null) {
			logger = Logger.getLogger(name);
		} else {
			logger = hierarchy.getLogger(name);
		}
		return logger;
	}

	public static Logger getLogger(Class clazz) { 
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(Object owner) { 
		return getLogger(owner.getClass());
	}

}
