package nl.nn.adapterframework.util;

import org.apache.log4j.Logger;

import java.util.Properties;
/**
 * Provide functionality to resolve ${property.key} to the value of the property key, recursively.
 * 
 * <p>Creation date: (15-08-2003 13:47:20)</p>
 * @author Johan Verrips 
 */
public class StringResolver {
	public static final String version="$Id: StringResolver.java,v 1.1 2004-02-04 08:36:06 a1909356#db2admin Exp $";
	
    static String DELIM_START = "${";
    static char DELIM_STOP = '}';
    static int DELIM_START_LEN = 2;
    static int DELIM_STOP_LEN = 1;
    static Logger log = Logger.getLogger("StringResolver");
/**
 * StringResolver constructor comment.
 */
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
            log.warn("Was not allowed to read system property \"" + key + "\".");
            return def;
        }
    }
    /**
 * Starts the application.
 * @param args an array of command-line arguments
 */
public static void main(java.lang.String[] args) {
	// Insert code to start the application here.
	StringResolver sr=new StringResolver();
	Properties prop=new Properties();
	prop.put("test.name", "dit is de naam met ${test.xx}");
	prop.put("test.xx", "hier nog eens");
	System.out.println(prop.get("test.name"));
	System.out.println(sr.substVars("dit is ${test.name}", prop));

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
 public static String substVars(String val, Properties props)
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
                        '"' + val + "\" has no closing brace. Opening brace at position " + j + '.');
                } else {
                    j += DELIM_START_LEN;
                    String key = val.substring(j, k);
                    // first try in System properties
                    String replacement = getSystemProperty(key, null);
                    // then try props parameter
                    if (replacement == null && props != null) {
                        replacement = props.getProperty(key);
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
}
