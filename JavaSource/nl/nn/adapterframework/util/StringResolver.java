/*
 * $Log: StringResolver.java,v $
 * Revision 1.7.4.1  2007-10-04 13:29:31  europe\L190409
 * synchronize with HEAD (4.7.0)
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
	public static final String version="$RCSfile: StringResolver.java,v $ $Revision: 1.7.4.1 $ $Date: 2007-10-04 13:29:31 $";
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
	  */ 
	public static String substVars(String val, Map props)
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
                    if (replacement == null && props != null) {
                    	if (props instanceof Properties){
                    		replacement=((Properties)props).getProperty(key);
                    	} else {
                    	
	                        replacement = props.get(key).toString();
						}

                    }

                    if (replacement != null) {
                        // Do variable substitution on the replacement string
                        // such that we can solve "Hello ${x2}" as "Hello p1" 
                        // the where the properties are
                        // x1=p1
                        // x2=${x1}
                        String recursiveReplacement = substVars(replacement, props);
                        sbuf.append(recursiveReplacement);
                    }
                    i = k + DELIM_STOP_LEN;
                }
            }
        }
    }
 
    
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		// Insert code to start the application here.
	//	StringResolver sr=new StringResolver();
		Properties prop=new Properties();
		prop.put("test.name", "dit is de naam met ${test.xx}");
		prop.put("test.xx", "hier nog eens");
		System.out.println(prop.get("test.name"));
		System.out.println(StringResolver.substVars("dit is ${test.name}", prop));
	
	}
    
}
