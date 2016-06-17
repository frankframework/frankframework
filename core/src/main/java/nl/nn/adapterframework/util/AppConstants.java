/*
   Copyright 2013, 2016 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.IbisContext;

import org.apache.commons.digester.substitution.VariableExpander;
import org.apache.log4j.Logger;
/**
 * Singleton class that has the constant values for this application. <br/>
 * <p>When an instance is created, it tries to load the properties file specified
 * by the <code>propertiesFileName</code> field</p>
 * <p>If a property exits with the name <code>ADDITIONAL.PROPERTIES.FILE</code>
 * that file is loaded also</p>

 * @author Johan Verrips
 *
 */
public final class AppConstants extends Properties implements Serializable{
	private Logger log = LogUtil.getLogger(this);

	public final static String propertiesFileName="AppConstants.properties";
	private static AppConstants self=null;
	private String additionalPropertiesFileKey="ADDITIONAL.PROPERTIES.FILE";
	private VariableExpander variableExpander;
	private static Properties propertyPlaceholderConfigurerProperties = new Properties();

//	private final static Properties baseProperties = new Properties();
//	static {
//		baseProperties.put("hostname", Misc.getHostname());
//	}
	
	private AppConstants() {
		super();
//		putAll(baseProperties);
		load(null, null, propertiesFileName);
	}

	private AppConstants(ClassLoader classLoader) {
		super();
//		putAll(baseProperties);
		load(classLoader, null, propertiesFileName);
		putAll(propertyPlaceholderConfigurerProperties);
	}

	private AppConstants(String directory) {
		super();
		load(null, directory, propertiesFileName);
		putAll(propertyPlaceholderConfigurerProperties);
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
	 * Retrieve an instance based on a ClassLoader. This should be used by
	 * classes which are part of the Ibis configuration (like pipes and senders)
	 * because the configuration might be loaded from outside the webapp
	 * classpath. Hence the Thread.currentThread().getContextClassLoader() at
	 * the time the class was instantiated should be used.
	 * 
	 * @see IbisContext#init()
	 * @return AppConstants instance
	 */
	public static synchronized AppConstants getInstance(ClassLoader classLoader) {
		return new AppConstants(classLoader);
	}

	/**
	 * Retrieve an instance based on a directory (not a singleton)
	 * @return AppConstants instance
	 */
	public static synchronized AppConstants getInstance(String directory) {
		return new AppConstants(directory);
	}

	/**
	   Very similar to <code>System.getProperty</code> except
	   that the {@link SecurityException} is hidden.

	   @param key The key to search for.
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
	 * Load the contents of a propertiesfile.
	 * <p>Optionally, this may be a comma-seperated list of files to load, e.g.
	 * <code><pre>log4j.properties,deploymentspecifics.properties</pre></code>
	 * which will cause both files to be loaded. Trimming of the filename will take place,
	 * so you may also specify <code><pre>log4j.properties, deploymentspecifics.properties</pre></code>
	 * </p>
	 */
	private synchronized void load(ClassLoader classLoader, String directory, String filename) {
		StringTokenizer tokenizer = new StringTokenizer(filename, ",");
		while (tokenizer.hasMoreTokens()) {
			String theFilename= tokenizer.nextToken().trim();
			try {
				if (StringResolver.needsResolution(theFilename)) {
					Properties props = Misc.getEnvironmentVariables();
					props.putAll(System.getProperties());
					theFilename = StringResolver.substVars(theFilename, props);
				}
				InputStream is = null;
				if (directory != null) {
					File file = new File(directory + "/" + theFilename);
					if (file.exists()) {
						is = new FileInputStream(file);
					} else {
						log.debug("cannot find file ["+theFilename+"] to load additional properties from, ignoring");
					}
				}
				URL url = null;
				if (is == null) {
					url = ClassUtils.getResourceURL(classLoader, theFilename);
					if (url == null) {
						log.debug("cannot find resource ["+theFilename+"] to load additional properties from, ignoring");
					} else {
						is = url.openStream();
					}
				}
				if (is != null) {
					load(is);
					if (url != null) {
						log.info("Application constants loaded from url [" + url.toString() + "]");
					} else {
						log.info("Application constants loaded from file [" + theFilename + "]");
					}
					if (getProperty(additionalPropertiesFileKey) != null) {
						// prevent reloading of the same file over and over again
						String loadFile = getProperty(additionalPropertiesFileKey);
						this.remove(additionalPropertiesFileKey);
						load(classLoader, directory, loadFile);
					}
				}
			} catch (IOException e) {
				log.error("error reading [" + propertiesFileName + "]", e);
			}
		}
	}

	public void setPropertyPlaceholderConfigurerProperty(String name, String value) {
		self.put(name, value);
		propertyPlaceholderConfigurerProperties.put(name, value);
	}

	public String toXml() {
		return toXml(false);
	}

	public String toXml(boolean resolve) {
		Enumeration enumeration=this.keys();
		XmlBuilder xmlh=new XmlBuilder("applicationConstants");
		XmlBuilder xml=new XmlBuilder("properties");
		xmlh.addSubElement(xml);

		while (enumeration.hasMoreElements()){
			String propName=(String)enumeration.nextElement();

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
