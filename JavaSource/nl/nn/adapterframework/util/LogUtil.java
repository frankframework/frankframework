/*
 * $Log: LogUtil.java,v $
 * Revision 1.1  2007-02-12 14:10:36  europe\L190409
 * introduction of LogUtil
 *
 */
package nl.nn.adapterframework.util;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.RootCategory;

/**
 * Convenience functions for logging.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class LogUtil {
	public static final String version="$RCSfile: LogUtil.java,v $  $Revision: 1.1 $ $Date: 2007-02-12 14:10:36 $";

	private static Hierarchy hierarchy = new Hierarchy(new RootCategory(Level.DEBUG));
	
	
	public static Logger getLogger(String name) { 
		return getHierarchy().getLogger(name);
	}

	public static Logger getLogger(Class clazz) { 
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(Object owner) { 
		return getLogger(owner.getClass());
	}


	public static Hierarchy getHierarchy() {
		return hierarchy;
	}

}
