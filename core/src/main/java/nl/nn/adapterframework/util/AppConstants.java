/*
   Copyright 2013, 2016, 2018 Nationale-Nederlanden

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.digester.substitution.VariableExpander;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
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

	private final static String APP_CONSTANTS_PROPERTIES_FILE = "AppConstants.properties";
	private final static String ADDITIONAL_PROPERTIES_FILE_KEY = "ADDITIONAL.PROPERTIES.FILE";
	private static AppConstants self = null;
	private VariableExpander variableExpander;
	private static Properties additionalPropertiesFilesSubstVarsProperties = new Properties();
	private static Properties propertyPlaceholderConfigurerProperties = new Properties();
	
	private AppConstants() {
		super();
		load(null, null, APP_CONSTANTS_PROPERTIES_FILE, true);
		if (JdbcUtil.retrieveJdbcPropertiesFromDatabase()!=null) {
			putAll(JdbcUtil.retrieveJdbcPropertiesFromDatabase());
		}
	}

	private AppConstants(ClassLoader classLoader) {
		super();
		load(classLoader, null, APP_CONSTANTS_PROPERTIES_FILE, true);
		putAll(propertyPlaceholderConfigurerProperties);
		if (JdbcUtil.retrieveJdbcPropertiesFromDatabase()!=null) {
			putAll(JdbcUtil.retrieveJdbcPropertiesFromDatabase());
		}
	}

	private AppConstants(String directory) {
		super();
		load(null, directory, APP_CONSTANTS_PROPERTIES_FILE, true);
		putAll(propertyPlaceholderConfigurerProperties);
		if (JdbcUtil.retrieveJdbcPropertiesFromDatabase()!=null) {
			putAll(JdbcUtil.retrieveJdbcPropertiesFromDatabase());
		}
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

	public static synchronized void removeInstance() {
		if (self!=null) {
			self.clear();
			self=null;
		}
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

	@Override
	public String get(Object key) {
		return getResolvedProperty((String)key);
	}

	@Override
	public String getProperty(String key) {
		return getResolvedProperty(key);
	}

	public String getUnresolvedProperty(String key) {
		return super.getProperty(key);
	}

	/**
	 * the method is like the <code>Properties.getProperty</code>, but provides functionality to resolve <code>${variable}</code>
	 * syntaxis. It uses the AppConstants values and systemvalues to resolve the variables, and does this recursively.
	 * @see nl.nn.adapterframework.util.StringResolver
	 */
	public String getResolvedProperty(String key) {
		String value = null;
        value=getSystemProperty(key); // first try custom properties
        if (value==null) {
			value = super.getProperty(key); // then try DeploymentSpecifics and appConstants
        }
		if (value != null) {
			try {
				String result=StringResolver.substVars(value, this);
				if (log.isTraceEnabled()) {
					if (!value.equals(result)){
						log.trace("resolved key ["+key+"], value ["+value+"] to ["+result+"]");
					}

				}
				return result;
			} catch (IllegalArgumentException e) {
				log.error("Bad option value [" + value + "].", e);
				return value;
			}
		} else {
            if (log.isTraceEnabled()) log.trace("getResolvedProperty: key ["+key+"] resolved to value ["+value+"]");
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
	 * Returns a list of {@link AppConstants#getInstance() AppConstants} which names begin with the keyBase
	 */
	public Properties getAppConstants(String keyBase) {
		if(!keyBase.endsWith("."))
			keyBase +=".";

		Properties properties = new Properties();
		for(Object objKey: getInstance().keySet()) {
			String key = (String) objKey;
			if(key.startsWith(keyBase)) {
				properties.put(key, getInstance().getResolvedProperty(key));
			}
		}

		return properties;
	}

	/**
	 * Load the contents of a propertiesfile.
	 * <p>Optionally, this may be a comma-seperated list of files to load, e.g.
	 * <code><pre>log4j.properties,deploymentspecifics.properties</pre></code>
	 * which will cause both files to be loaded. Trimming of the filename will take place,
	 * so you may also specify <code><pre>log4j.properties, deploymentspecifics.properties</pre></code>
	 * </p>
	 */
	private synchronized void load(ClassLoader classLoader, String directory,
			String filename, boolean loadAdditionalPropertiesFiles) {
		load(classLoader, directory, filename, null, loadAdditionalPropertiesFiles);
	}
	private synchronized void load(ClassLoader classLoader, String directory,
			String filename, String suffix,
			boolean loadAdditionalPropertiesFiles) {
		StringTokenizer tokenizer = new StringTokenizer(filename, ",");
		while (tokenizer.hasMoreTokens()) {
			String theFilename= tokenizer.nextToken().trim();
			try {
				if (directory != null) {
					File file = new File(directory + "/" + theFilename);
					if (file.exists()) {
						InputStream is = new FileInputStream(file);
						load(is);
						log.info("Application constants loaded from file [" + theFilename + "]");
					} else {
						log.debug("cannot find file ["+theFilename+"] to load additional properties from, ignoring");
					}
				}
				else {
					ClassLoader cl = classLoader;
					if(classLoader == null) {
						cl = AppConstants.class.getClassLoader();
					}
					List<URL> resources = Collections.list(cl.getResources(theFilename));
					if(resources.size() == 0)
						log.debug("cannot find resource ["+theFilename+"] to load additional properties from, ignoring");

					//We need to reverse the loading order to make sure the parent files are loaded first
					Collections.reverse(resources);

					for (URL url : resources) {
						InputStream is = url.openStream();
						load(is);
						log.info("Application constants loaded from url [" + url.toString() + "]");
					}

					if (loadAdditionalPropertiesFiles) {
						// Add properties after load(is) to prevent load(is)
						// from overriding them
						putAll(additionalPropertiesFilesSubstVarsProperties);
						String loadFile = getProperty(ADDITIONAL_PROPERTIES_FILE_KEY);
						String loadFileSuffix = getProperty(ADDITIONAL_PROPERTIES_FILE_KEY + ".SUFFIX");
						if (StringUtils.isNotEmpty(loadFileSuffix)){
							load(classLoader, directory, loadFile, loadFileSuffix, false);
						} else {
							load(classLoader, directory, loadFile, false);
						}
					}
				}
				if (suffix != null) {
					String baseName = FilenameUtils.getBaseName(theFilename);
					String extension = FilenameUtils.getExtension(theFilename);
					String suffixedFilename = baseName
							+ "_"
							+ suffix
							+ (StringUtils.isEmpty(extension) ? "" : "."
									+ extension);
					load(classLoader, directory, suffixedFilename, false);
				}
			} catch (IOException e) {
				log.error("error reading [" + APP_CONSTANTS_PROPERTIES_FILE + "]", e);
			}
		}
	}

	/**
	 * Add property which can be used to substitute variables in the value of
	 * the property ADDITIONAL.PROPERTIES.FILE.
	 */
	public void putAdditionalPropertiesFilesSubstVarsProperty(String name, String value) {
		additionalPropertiesFilesSubstVarsProperties.put(name, value);
	}

	/**
	 * Add additional property to AppConstants which is added to Spring by a
	 * PropertyPlaceholderConfigurer and also needs to to be added to
	 * AppConstants to keep properties available to Spring and properties in
	 * AppConstants in sync. AppConstants properties are initially added to
	 * Spring as PropertiesPropertySource in the createApplicationContext method
	 * of IbisContext.
	 */
	public void putPropertyPlaceholderConfigurerProperty(String name, String value) {
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
