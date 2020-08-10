/*
   Copyright 2013, 2016, 2018-2019 Nationale-Nederlanden

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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.digester.substitution.VariableExpander;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
/**
 * Singleton class that has the constant values for this application. <br/>
 * <p>When an instance is created, it tries to load the properties file specified
 * by the <code>propertiesFileName</code> field</p>
 * <p>If a property exits with the name <code>ADDITIONAL.PROPERTIES.FILE</code>
 * that file is loaded also</p>

 * @author Niels Meijer
 * @version 2.0
 *
 */
public final class AppConstants extends Properties implements Serializable {
	private Logger log = LogUtil.getLogger(this);

	private final static String APP_CONSTANTS_PROPERTIES_FILE = "AppConstants.properties";
	private final static String ADDITIONAL_PROPERTIES_FILE_KEY = "ADDITIONAL.PROPERTIES.FILE";
	public static final String APPLICATION_SERVER_TYPE_PROPERTY = "application.server.type";

	private VariableExpander variableExpander;
	private static Properties additionalProperties = new Properties();

	private static ConcurrentHashMap<ClassLoader, AppConstants> appConstantsMap = new ConcurrentHashMap<ClassLoader, AppConstants>();

	private AppConstants(ClassLoader classLoader) {
		super();

		load(classLoader, APP_CONSTANTS_PROPERTIES_FILE, true);

		//TODO Make sure this to happens only once, and store all the properties in 'additionalProperties' to be loaded for each AppConstants instance
		//TODO JdbcUtil has static references to AppConstants causing it to load twice!
		if(classLoader instanceof IConfigurationClassLoader) {
			Properties databaseProperties = JdbcUtil.retrieveJdbcPropertiesFromDatabase();
			if (databaseProperties!=null) {
				putAll(databaseProperties);
			}
		}

		//Add all ibis properties
		putAll(additionalProperties);

		//Make sure to not call ClassUtils when using the root instance, as it has a static field referencing to AppConstants
		if(log.isInfoEnabled() && classLoader instanceof IConfigurationClassLoader) {
			log.info("created new AppConstants instance for classloader ["+ClassUtils.nameOf(classLoader)+"]");
		}
		else {
			log.info("created new AppConstants instance for root classloader");
		}
	}

	/**
	 * Return the AppConstants root instance
	 * @return AppConstants instance
	 */
	public static AppConstants getInstance() {
		return getInstance(AppConstants.class.getClassLoader());
	}

	/**
	 * Retrieve an instance based on a ClassLoader. This should be used by
	 * classes which are part of the Ibis configuration (like pipes and senders)
	 * because the configuration might be loaded from outside the webapp
	 * classpath. Hence the Thread.currentThread().getContextClassLoader() at
	 * the time the class was instantiated should be used.
	 * 
	 * @see IbisContext#init()
	 * @param cl ClassLoader to retrieve AppConstants from
	 * @return AppConstants instance
	 */
	public static synchronized AppConstants getInstance(final ClassLoader cl) {
		ClassLoader classLoader = cl;
		if(cl == null) {
			throw new IllegalStateException("calling AppConstants.getInstance without ClassLoader");
		}

		AppConstants instance = appConstantsMap.get(classLoader);
		if(instance == null) {
			instance = new AppConstants(classLoader);
			appConstantsMap.put(classLoader, instance);
		}
		return instance;
	}

	public static void removeInstance() {
		removeInstance(AppConstants.class.getClassLoader());
	}

	public static synchronized void removeInstance(final ClassLoader cl) {
		ClassLoader classLoader = cl;
		if(classLoader == null) {
			throw new IllegalStateException("calling AppConstants.removeInstance without ClassLoader");
		}
		AppConstants instance = appConstantsMap.get(classLoader);
		if (instance != null) {
			appConstantsMap.remove(classLoader);
		}
	}

	/**
	 * Very similar to <code>System.getProperty</code> except
	 * that the {@link SecurityException} is hidden.
	 * 
	 * @param key The key to search for.
	 * @return the string value of the system property, or NULL if there is no property with that key.
	 */
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
	 * Creates a tokenizer from the resolved value of this key. As a separator the "," is used.
	 * Uses the {@link #getResolvedProperty(String)} method.
	 * Can be used to process lists of values.
	 */
	public StringTokenizer getTokenizedProperty(String key) {
		return new StringTokenizer(getResolvedProperty(key), ",");
	}
	/**
	 * Creates a tokenizer from the resolved value of this key. As a separator the "," is used.
	 * Uses the {@link #getResolvedProperty(String)} method.
	 * Can be used to process lists of values.
	 */
	public StringTokenizer getTokenizedProperty(String key, String defaults) {
		String list = getResolvedProperty(key);
		if (list==null)
			list = defaults;
		return new StringTokenizer(list, ",");
	}

	/**
	 * Returns a list of {@link AppConstants#getInstance() AppConstants} which names begin with the keyBase
	 */
	public Properties getAppConstants(String keyBase) {
		return getAppConstants(keyBase, true, true);
	}
	

	/**
	 * Returns a list of {@link AppConstants#getInstance() AppConstants} which names begin with the keyBase
	 */
	public Properties getAppConstants(String keyBase, boolean useSystemProperties, boolean useEnvironmentVariables) {
		if(!keyBase.endsWith("."))
			keyBase +=".";

		AppConstants constants = getInstance();
		if(useSystemProperties)
			constants.putAll(System.getProperties());
		if(useEnvironmentVariables) {
			try {
				constants.putAll(Misc.getEnvironmentVariables());
			} catch (IOException e) {
				log.warn("unable to retrieve environment variables", e);
			}
		}

		Properties filteredProperties = new Properties();
		for(Object objKey: constants.keySet()) {
			String key = (String) objKey;
			if(key.startsWith(keyBase)) {
				filteredProperties.put(key, constants.getResolvedProperty(key));
			}
		}

		return filteredProperties;
	}

	/**
	 * Load the contents of a properties file.
	 * <p>Optionally, this may be a comma-separated list of files to load, e.g.
	 * <code><pre>log4j2.properties,deploymentspecifics.properties</pre></code>
	 * which will cause both files to be loaded in the listed order.
	 * </p>
	 */
	private synchronized void load(ClassLoader classLoader, String filename, boolean loadAdditionalPropertiesFiles) {
		load(classLoader, filename, null, loadAdditionalPropertiesFiles);
	}

	private synchronized void load(ClassLoader classLoader, String filename, String suffix, boolean loadAdditionalPropertiesFiles) {
		if(StringUtils.isEmpty(filename)) {
			throw new IllegalStateException("file to load properties from cannot be null");
		}

		StringTokenizer tokenizer = new StringTokenizer(filename, ",");
		while (tokenizer.hasMoreTokens()) {
			String theFilename = tokenizer.nextToken().trim();
			try {
				ClassLoader cl = classLoader;
				if(classLoader == null) {
					throw new IllegalStateException("no classloader found!");
				}
				List<URL> resources = Collections.list(cl.getResources(theFilename));
				if(resources.size() == 0) {
					if(APP_CONSTANTS_PROPERTIES_FILE.equals(theFilename)) { //The AppConstants.properties file cannot be found, abort!
						String msg = APP_CONSTANTS_PROPERTIES_FILE+ " file not found, unable to initalize AppConstants";
						log.error(msg);
						throw new MissingResourceException(msg, this.getClass().getSimpleName(), APP_CONSTANTS_PROPERTIES_FILE);
					}
					//An additional file to load properties from cannot be found
					if(log.isDebugEnabled()) {
						if(cl instanceof IConfigurationClassLoader) {
							log.debug("cannot find resource ["+theFilename+"] in classloader ["+cl+"] to load additional properties from, ignoring");
						} else {
							log.debug("cannot find resource ["+theFilename+"] in classloader ["+cl.getClass().getSimpleName()+"] to load additional properties from, ignoring");
						}
					}
				}

				//We need to reverse the loading order to make sure the parent files are loaded first
				Collections.reverse(resources);

				for (URL url : resources) {
					InputStream is = url.openStream();
					load(is);
					log.info("Application constants loaded from url [" + url.toString() + "]");
				}

				String loadFile = getProperty(ADDITIONAL_PROPERTIES_FILE_KEY); //Only load additional properties if it's defined...
				if (loadAdditionalPropertiesFiles && StringUtils.isNotEmpty(loadFile)) {
					// Add properties after load(is) to prevent load(is)
					// from overriding them
					String loadFileSuffix = getProperty(ADDITIONAL_PROPERTIES_FILE_KEY + ".SUFFIX");
					if (StringUtils.isNotEmpty(loadFileSuffix)){
						load(classLoader, loadFile, loadFileSuffix, false);
					} else {
						load(classLoader, loadFile, false);
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
					load(classLoader, suffixedFilename, false);
				}
			} catch (IOException e) {
				log.error("error reading [" + APP_CONSTANTS_PROPERTIES_FILE + "]", e);
			}
		}
	}

	/**
	 * Add property only in the local AppConstants!
	 * Try to avoid using this method and use {@link #setProperty(String, String)} if you want to set the property globally!
	 * 
	 * This method is used by {@link Properties#load(InputStream)} to add all properties found (in a file/stream) 
	 * to the {@link Hashtable}.
	 * @deprecated Use {@link #setProperty(String, String)} instead!
	 */
	@Deprecated
	@Override
	public synchronized Object put(Object key, Object value) {
		return super.put(key, value);
	}

	/**
	 * Add property to global AppConstants
	 */
	@Override
	public Object setProperty(String key, String value) {
		return setProperty(key, value, false);
	}

	public Object setProperty(String key, boolean value) {
		return setProperty(key, ""+value, false);
	}
	/**
	 * Add property to global (all) AppConstants
	 */
	public void put(String key, String value) {
		setProperty(key, value, false);
	}

	/**
	 * Add property to local (local=true) or global (local=false) AppConstants
	 */
	private synchronized Object setProperty(String key, String value, boolean local) {
		if(local) {
			return super.put(key, value);
		} else {
			for (java.util.Map.Entry<ClassLoader, AppConstants> mapElement : appConstantsMap.entrySet()) {
				mapElement.getValue().setProperty(key, value, true);
			}
			//Store in a map in case a new AppConstants instance is created after the property has already been set
			return additionalProperties.put(key, value);
		}
	}

	@Deprecated
	public String toXml() {
		return toXml(false);
	}

	@Deprecated
	public String toXml(boolean resolve) {
		Enumeration<Object> enumeration = this.keys();
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
