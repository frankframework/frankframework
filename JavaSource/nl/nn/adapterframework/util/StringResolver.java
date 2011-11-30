/*
 * $Log: StringResolver.java,v $
 * Revision 1.15  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.13  2010/03/17 11:20:40  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added method needsResolution and extended method substVars with extra Properties object
 *
 * Revision 1.12  2010/03/10 13:59:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * committed to soon...
 *
 * Revision 1.10  2008/06/03 15:59:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE in solving keys to properties
 *
 * Revision 1.9  2008/03/28 14:24:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.8  2007/10/01 14:13:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error messages
 *
 * Revision 1.7  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.6  2006/11/14 16:39:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.5  2004/08/30 06:37:19  Johan Verrips <johan.verrips@ibissource.org>
 * Accepts map as parameter instead of Properties
 *
 * Revision 1.4  2004/03/26 10:42:37  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 17:05:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.util;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 * Provide functionality to resolve ${property.key} to the value of the property key, recursively.
 * @version Id
 * 
 * @author Johan Verrips 
 */
public class StringResolver {
	public static final String version="$RCSfile: StringResolver.java,v $ $Revision: 1.15 $ $Date: 2011-11-30 13:51:48 $";
	protected static Logger log = LogUtil.getLogger(StringResolver.class);
	
    static String DELIM_START = "${";
    static char DELIM_STOP = '}';
    static int DELIM_START_LEN = 2;
    static int DELIM_STOP_LEN = 1;

	public StringResolver() {
		super();
	}
    /**
       Very similar to <code>System.getProperty</code> except
       that the {@link SecurityException} is hidden.
    
       @param key The key to search for.
       @param def The default value to return.
       @return the string value of the system property, or the default
       value if there is no property with that key.
    
       @since 1.1 */
    public static String getSystemProperty(String key, String def) {
        try {
            return System.getProperty(key, def);
        } catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
            log.warn("Was not allowed to read system property [" + key + "]: "+ e.getMessage());
            return def;
        }
    }
    
     /**
	  * Do variable substitution on a string to resolve ${x2} to the value of the property x2.
	  * This is done recursive, so that <br><code><pre>
	  * Properties prop = new Properties();
	  * prop.put("test.name", "this is a name with ${test.xx}");
	  * prop.put("test.xx", "again");
	  * System.out.println(prop.get("test.name"));
	  * </pre></code>
	  * will print <code>this is a name with again</code>
	  * <p> First it looks in the System properties, if none is found and a <code>Properties</code>
	  * object is specified, it looks in the specified <code>Properties</code> object.
	  * If two <code>Properties</code> objects are specified, first it look in the first object. If
	  * none is found, it looks in the second object.
	  * 
	  */ 
	public static String substVars(String val, Map props1, Map props2)
        throws IllegalArgumentException {

        StringBuffer sbuf = new StringBuffer();

        int i = 0;
        int j, k;

        while (true) {
            j = val.indexOf(DELIM_START, i);
            if (j == -1) {
                // no more variables
                if (i == 0) { // this is a simple string
                    return val;
                } else { // add the tail string which contails no variables and return the result.
                    sbuf.append(val.substring(i, val.length()));
                    return sbuf.toString();
                }
            } else {
                sbuf.append(val.substring(i, j));
                k = val.indexOf(DELIM_STOP, j);
                if (k == -1) {
                    throw new IllegalArgumentException(
                        '[' + val + "] has no closing brace. Opening brace at position ["  + j + "]");
                } else {
                    j += DELIM_START_LEN;
                    String key = val.substring(j, k);
                    // first try in System properties
                    String replacement = getSystemProperty(key, null);
                    // then try props parameter
                    if (replacement == null && props1 != null) {
                    	if (props1 instanceof Properties){
                    		replacement=((Properties)props1).getProperty(key);
                    	} else {
                    		Object replacementSource = props1.get(key); 
                    		if (replacementSource!=null) {
								replacement = replacementSource.toString();
                    		}
						}
                    }
					if (replacement == null && props2 != null) {
						if (props2 instanceof Properties){
							replacement=((Properties)props2).getProperty(key);
						} else {
							Object replacementSource = props2.get(key); 
							if (replacementSource!=null) {
								replacement = replacementSource.toString();
							}
						}
					}

                    if (replacement != null) {
                        // Do variable substitution on the replacement string
                        // such that we can solve "Hello ${x1}" as "Hello p2" 
                        // the where the properties are
						// x1=${x2}
                        // x2=p2
                        String recursiveReplacement = substVars(replacement, props1, props2);
                        sbuf.append(recursiveReplacement);
                    }
                    i = k + DELIM_STOP_LEN;
                }
            }
        }
    }

	public static String substVars(String val, Map props)
		throws IllegalArgumentException {
		return substVars(val, props, null);
	}

	public static boolean needsResolution(String string) {
		int j = string.indexOf(DELIM_START);
		if (j == -1) {
			return false;
		} else {
			int k = string.indexOf(DELIM_STOP, j);
			if (k == -1) {
				return false;
			} else {
				return true;
			}			
		}
	}
}
