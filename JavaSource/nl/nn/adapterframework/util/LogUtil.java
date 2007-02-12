/*
 * $Log: LogUtil.java,v $
 * Revision 1.2  2007-02-12 15:56:27  europe\L190409
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
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class LogUtil {
	public static final String version="$RCSfile: LogUtil.java,v $  $Revision: 1.2 $ $Date: 2007-02-12 15:56:27 $";

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
			URL url = ClassUtils.getResourceURL(LogUtil.class, "log4j.properties");
			new PropertyConfigurator().doConfigure(url, hierarchy);
		}
		return hierarchy;
	}

}
