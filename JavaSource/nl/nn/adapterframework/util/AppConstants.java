package nl.nn.adapterframework.util;

import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.Binding;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;
/**
 * Singleton class that has the constant values for this application. <br/>
 * <p>When an instance is created, it tries to load the properties file specified
 * by the <code>propertiesFileName</code> field</p>
 * <p>If a property exits with the name <code>ADDITIONAL.PROPERTIES.FILE</code>
 * that file is loaded also</p>
 * <p>Properties may also be retrieved by retrieving the environment entries of the container
 * (since 4.0.1).  See table below for additional parameters. </p>
 *
 	<table>
	<tr><th>Property</th><th>Description</th><th>Type></th><th>Default Value</th></tr>
	<tr>
		<td>ADDITIONAL.PROPERTIES.FILE</TD>
		<td>Location of a file to load additionally.</td>
		<td>String
		<td>-not specified-</td>
	</tr>
	<tr>
		<td>environment.entries.load</td>
		<td>should environment entries be loaded from the container</td>
		<td>Boolean</td>
		<td>true</td>
	</tr>
	<tr>
		<td>environment.entries.bindingname</td>
		<td>Name where the bindings are stored</td>
		<td>String</td>
		<td>java:comp/env</td>
	</tr>
	<tr>
		<td>environment.entries.factory.initial</td>
		<td>Jndi setting: initial context factory</td>
		<td>String</td>
		<td>not specified</td>
		</tr>
	<tr>
		<td>environment.entries.provider.url</td>
		<td>Jndi setting: provider url</td>
		<td>String</td>
		<td>not specified</td>
		</tr>
	<tr>
		<td>environment.entries.security.principal</td>
		<td>Jndi setting: user id</td>
		<td>String</td>
		<td>not specified</td>
		</tr>
	<tr>
		<td>environment.entries.security.credentials</td>
		<td>Jndi setting: password</td>
		<td>String</td>
		<td>not specified</td>
	</tr>	
	<tr>
		<td>environment.entries.factory.url.pkgs</td>
		<td>Jndi setting: package prefixes to use when loading in URL context factories</td>
		<td>String</td>
		<td>not specified</td>
	</tr>	
	</table>
<p></p>
 * @version Id

 * @author Johan Verrips
 * 
 */
public final class AppConstants extends Properties implements Serializable{
	public static final String version="$Id: AppConstants.java,v 1.4 2005-02-17 09:48:04 L190409 Exp $";
	
	public final static String propertiesFileName="AppConstants.properties";
	private static AppConstants self=null;
	private Logger log = Logger.getLogger(this.getClass());
	private String additionalPropertiesFileKey="ADDITIONAL.PROPERTIES.FILE";
	
	private AppConstants() {
		super();
		load();
	
	}
	 /**
     * Gets a <code>boolean</code> value
     * Returns "true" if the retrieved value is "true", otherwise "false"
     * Uses the {@link #getResolvedProperty(String)} method.
     * @param key    the Key
     * @param dfault the default value
     * @return double
     */
     public boolean getBoolean(String key, boolean dfault) {
         String ob = this.getResolvedProperty(key);
         if (ob == null)return dfault;
         
         return ob.equalsIgnoreCase("true");
     }
    /**
     * Gets a <code>double</code> value
     * Uses the {@link #getResolvedProperty(String)} method.
     * @param key    the Key
     * @param dfault the default value
     * @return double
     */
     public double getDouble(String key, double dfault) {
         String ob = this.getResolvedProperty(key);
         if (ob == null)return dfault;
         return Double.parseDouble(ob);
     }
    /**
     * Retrieve an instance of this singleton
     * @return AppConstants instance
     */

	public static synchronized AppConstants getInstance() {
		if (self==null) {
			self=new AppConstants();

		}
		return self;
	}
    /**
     * Gets an <code>int</code> value
     * Uses the {@link #getResolvedProperty(String)} method.
     * @param key    the Key
     * @param dfault the default value
     * @return int
     */

    public int getInt(String key, int dfault) {
         String ob = this.getResolvedProperty(key);

         if (ob == null) return dfault;
         return Integer.parseInt(ob);
     }
    /**
     * Gets a <code>long</code> value
     * Uses the {@link #getResolvedProperty(String)} method.
     * @param key    the Key
     * @param dfault the default value
     * @return long
     */
     public long getLong(String key, long dfault) {
         String ob = this.getResolvedProperty(key);

         if (ob == null)return dfault;
         return Long.parseLong(ob);
     }
	/**
	 * the method is like the <code>getProperty</code>, but provides functionality to resolve <code>${variable}</code>
	 * syntaxis. It uses the AppConstants values and systemvalues to resolve the variables, and does this recursively.
	 * @see nl.nn.adapterframework.util.StringResolver
	 */
	public String getResolvedProperty(String key) {
        String value = this.getProperty(key);
        if (value == null)
            return null;

        try {
	        String result=StringResolver.substVars(value, this);
	        if (log.isDebugEnabled()) {
		        if (!value.equals(result)){
			        log.debug("resolved key ["+key+"], value ["+value+"] to ["+result+"]");
		        }
	        
	        }
	        return result;
        } catch (IllegalArgumentException e) {
            log.error("Bad option value [" + value + "].", e);
            return value;
        }
	}
    /**
     * Gets a <code>String</code> value
     * Uses the {@link #getResolvedProperty(String)} method.
     * @param key    the Key
     * @param dfault the default value
     * @return String
     */

    public String getString(String key, String dfault){
        String ob = this.getResolvedProperty(key);

        if (ob == null)return dfault;
        return ob;

    }
/**
 * Creates a tokenizer from the values of this key.  As a sepearator the "," is used.
 * Uses the {@link #getResolvedProperty(String)} method.
 * Can be used to process lists of values.
 */
public StringTokenizer getTokenizer(String key) {
    return new StringTokenizer(getResolvedProperty(key), ",");
}
/**
 * Creates a tokenizer from the values of this key.
 * Uses the {@link #getResolvedProperty(String)} method.
 * Can be used to process lists of values.
 */
public StringTokenizer getTokenizer(String key, String defaults) {
	String list = getResolvedProperty(key);
	if (list==null)
	  list = defaults;
    return new StringTokenizer(list, ",");
}
/**
 * Load loads first the properties from the file specified by propertiesFileName and any propertiesfile specified
 * there-in. After that (if the property environment.entries.load is TRUE), it reads the environment entries in het
 * jndi.  Each new property load overrides existing properties, so the first file may contain application defaults,
 * then per deployment and at last environment entries in the JNDI.
 */
public void load() {
    load(propertiesFileName);
    if (getBoolean("environment.entries.load", true)) {
        try {
            String factory = getString("environment.entries.factory.initial", null);
            String providerUrl = getString("environment.entries.provider.url", null);
            String principal = getString("environment.entries.security.principal", null);
            String credentials =   getString("environment.entries.security.credentials", null);
            String urlPkgPrefixes =   getString("environment.entries.factory.url.pkgs", null);

            InitialContext context;

            Hashtable env = new Hashtable();
            if (StringUtils.isNotEmpty(factory))
                env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
            if (StringUtils.isNotEmpty(urlPkgPrefixes))
                env.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
            if (StringUtils.isNotEmpty(providerUrl))
                env.put(Context.PROVIDER_URL, providerUrl);
            if (StringUtils.isNotEmpty(principal))
                env.put(Context.SECURITY_PRINCIPAL, principal);
            if (StringUtils.isNotEmpty(credentials))
                env.put(Context.SECURITY_CREDENTIALS, credentials);
                
            if (env.size() > 0)
                context = new InitialContext(env);
            else
                context = new InitialContext();
 
             NamingEnumeration nEnumeration =
                context.listBindings(
                    getString("environment.entries.bindingname", "java:comp/env"));
            while (nEnumeration.hasMore()) {
                Binding binding = (Binding) nEnumeration.next();
                this.put(binding.getName(), binding.getObject());
            }
            nEnumeration.close();
            context.close();
        } catch (NamingException ne) {
            log.error("Error occured retrieving environment entries ", ne);
        }
    }
}
/**
 * Load the contents of a propertiesfile. 
 * <p>Optionally, this may be a comma-seperated list of files to load, e.g. 
 * <code><pre>log4j.properties,deploymentspecifics.properties</pre></code>
 * which will cause both files to be loaded. Trimming of the filename will take place,
 * so you may also specify <code><pre>log4j.properties, deploymentspecifics.properties</pre></code>
 * </p>
 */
public synchronized void load(String filename) {

    StringTokenizer tokenizer = new StringTokenizer(filename, ",");
    
     
     
    while (tokenizer.hasMoreTokens()) {
        String theFilename=((String) (tokenizer.nextToken())).trim();
        try {
            URL url = ClassUtils.getResourceURL(this, theFilename);

			if (url==null) {
				log.warn("cannot find resource ["+theFilename+"] to load additional properties from, ignoring");
			} else {
	            InputStream is = url.openStream();
	            load(is);
	            log.info("Application constants loaded from [" + url.toString() + "]");
	            if (getProperty(additionalPropertiesFileKey) != null) {
	                // prevent reloading of the same file over and over again
	                String loadFile = getProperty(additionalPropertiesFileKey);
	                this.remove(additionalPropertiesFileKey);
	                load(loadFile);
	            }
			}
        } catch (IOException e) {
            log.error("error reading [" + propertiesFileName + "]", e);
        }
    }
}
	public void setDefaults(Properties defaults) {
		super.putAll(defaults);
	}
	public String toXml() {
		Enumeration enum=this.keys();
		XmlBuilder xmlh=new XmlBuilder("applicationConstants");
		XmlBuilder xml=new XmlBuilder("properties");
		xmlh.addSubElement(xml);
		
		while (enum.hasMoreElements()){
			String propName=(String)enum.nextElement();
			
			XmlBuilder p=new XmlBuilder("property");
			p.addAttribute("name", propName);
			p.setValue(this.getProperty(propName));
			xml.addSubElement(p);
		}
		return xmlh.toXML();
	}
}
