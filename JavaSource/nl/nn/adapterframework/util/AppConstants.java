/*
 * $Log: AppConstants.java,v $
 * Revision 1.17  2010-03-10 13:57:50  m168309
 * committed to soon...
 *
 * Revision 1.15  2008/06/03 16:04:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * let custom properties override appConstants and DeploymentSpecifics
 *
 * Revision 1.14  2008/05/29 13:41:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reordered methods in file
 *
 * Revision 1.13  2007/10/10 07:27:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of VariableExpander
 *
 * Revision 1.12  2007/02/12 14:09:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.11  2006/03/15 14:01:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * resolving in toXml parameterized
 *
 * Revision 1.10  2006/03/08 13:57:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed resolving of properties in XML
 *
 * Revision 1.8  2006/01/19 12:17:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed warning for non found file to debug
 *
 * Revision 1.7  2005/09/20 13:29:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed setting of baseResourceURL
 *
 * Revision 1.6  2005/09/13 15:44:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added code to determine and set baseResourceURL
 *
 * Revision 1.5  2005/07/28 07:41:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added log-keyword
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.digester.substitution.VariableExpander;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
	public static final String version = "$RCSfile: AppConstants.java,v $ $Revision: 1.17 $ $Date: 2010-03-10 13:57:50 $";
	private Logger log = LogUtil.getLogger(this);
	
	public final static String propertiesFileName="AppConstants.properties";
	private static AppConstants self=null;
	private String additionalPropertiesFileKey="ADDITIONAL.PROPERTIES.FILE";
	
    private VariableExpander variableExpander;
    
	private AppConstants() {
		super();
		load();
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
	   Very similar to <code>System.getProperty</code> except
	   that the {@link SecurityException} is hidden.
    
	   @param key The key to search for.
	   @param def The default value to return.
	   @return the string value of the system property, or the default
	   value if there is no property with that key.
    
	   @since 1.1 */
	private String getSystemProperty(String key) {
		try {
			return System.getProperty(key);
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			log.warn("Was not allowed to read system property [" + key + "]: "+ e.getMessage());
			return null;
		}
	}

	/**
	 * the method is like the <code>getProperty</code>, but provides functionality to resolve <code>${variable}</code>
	 * syntaxis. It uses the AppConstants values and systemvalues to resolve the variables, and does this recursively.
	 * @see nl.nn.adapterframework.util.StringResolver
	 */
	public String getResolvedProperty(String key) {
        String value = null;
        value=getSystemProperty(key); // first try custom properties
        if (value==null) {
			value = getProperty(key); // then try DeploymentSpecifics and appConstants 
        }
		if (value != null) {
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
		} else {
            if (log.isDebugEnabled()) log.debug("getResolvedProperty: key ["+key+"] resolved to value ["+value+"]");
            return null;
        }
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
					log.debug("cannot find resource ["+theFilename+"] to load additional properties from, ignoring");
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
		return toXml(false);
	}

	public String toXml(boolean resolve) {
		Enumeration enum=this.keys();
		XmlBuilder xmlh=new XmlBuilder("applicationConstants");
		XmlBuilder xml=new XmlBuilder("properties");
		xmlh.addSubElement(xml);
		
		while (enum.hasMoreElements()){
			String propName=(String)enum.nextElement();
			
			XmlBuilder p=new XmlBuilder("property");
			p.addAttribute("name", propName);
			if (resolve) {
				p.setValue(this.getResolvedProperty(propName));
			} else {
				p.setValue(this.getProperty(propName));
			}
			xml.addSubElement(p);
		}
		return xmlh.toXML();
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

  
	/*
	 *	The variableExpander is set from the SpringContext. 
	 */
    public void setVariableExpander(VariableExpander expander) {
        variableExpander = expander;
    }
	public VariableExpander getVariableExpander() {
		return variableExpander;
	}

}
