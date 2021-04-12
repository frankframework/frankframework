/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Functions to manipulate the configuration. 
 *
 * @author  Peter Leeuwenburgh
 * @author  Jaco de Groot
 */
public class ConfigurationUtils {
	private static Logger log = LogUtil.getLogger(ConfigurationUtils.class);

	private static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	private static final String STUB4TESTTOOL_VALIDATORS_DISABLED_KEY = "validators.disabled";
	private static final String STUB4TESTTOOL_XSLT = "/xml/xsl/stub4testtool.xsl";
	private static final String ACTIVE_XSLT = "/xml/xsl/active.xsl";
	private static final String CANONICALIZE_XSLT = "/xml/xsl/canonicalize.xsl";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.autoDatabaseClassLoader", false);
	private static final boolean CONFIG_AUTO_FS_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.directory.autoLoad", false);
	private static final String CONFIGURATIONS = APP_CONSTANTS.getResolvedProperty("configurations.names.application");
	public static String ADDITIONAL_PROPERTIES_FILE_SUFFIX = APP_CONSTANTS.getString("ADDITIONAL.PROPERTIES.FILE.SUFFIX", null);
	public static final String DEFAULT_CONFIGURATION_FILE = "Configuration.xml";

	/**
	 * Checks if a configuration is stubbed or not
	 */
	public static boolean isConfigurationStubbed(ClassLoader classLoader) {
		return AppConstants.getInstance(classLoader).getBoolean(STUB4TESTTOOL_CONFIGURATION_KEY, false);
	}

	public static String getStubbedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		Map<String, Object> parameters = new Hashtable<String, Object>();
		// Parameter disableValidators has been used to test the impact of
		// validators on memory usage.
		parameters.put("disableValidators", AppConstants.getInstance(configuration.getClassLoader()).getBoolean(STUB4TESTTOOL_VALIDATORS_DISABLED_KEY, false));
		return transformConfiguration(configuration, originalConfig, STUB4TESTTOOL_XSLT, parameters);
	}

	public static String getActivatedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		return transformConfiguration(configuration, originalConfig, ACTIVE_XSLT, null);
	}

	public static String getCanonicalizedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		return transformConfiguration(configuration, originalConfig, CANONICALIZE_XSLT, null);
	}

	public static String transformConfiguration(Configuration configuration, String originalConfig, String xslt, Map<String, Object> parameters) throws ConfigurationException {
		URL xsltSource = ClassUtils.getResourceURL(configuration, xslt);
		if (xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + xslt + "]");
		}
		try {
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			XmlUtils.setTransformerParameters(transformer, parameters);
			// Use namespaceAware=true, otherwise for some reason the
			// transformation isn't working with a SAXSource, in system out it
			// generates:
			// jar:file: ... .jar!/xml/xsl/active.xsl; Line #34; Column #13; java.lang.NullPointerException
			return XmlUtils.transformXml(transformer, originalConfig, true);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + xslt + "]", e);
		} catch (SAXException|TransformerConfigurationException e) {
			throw new ConfigurationException("got error creating transformer from file [" + xslt + "]", e);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + xsltSource.toString() + "] from [" + xslt + "]", te);
		}
	}

	public static String getConfigurationFile(ClassLoader classLoader, String currentConfigurationName) {
		String configFileKey = "configurations." + currentConfigurationName + ".configurationFile";
		String configurationFile = AppConstants.getInstance(classLoader).getResolvedProperty(configFileKey);
		if (StringUtils.isEmpty(configurationFile) && classLoader != null) {
			configurationFile = AppConstants.getInstance(classLoader.getParent()).getResolvedProperty(configFileKey);
		}
		if (StringUtils.isEmpty(configurationFile)) {
			configurationFile = DEFAULT_CONFIGURATION_FILE;
		} else {
			int i = configurationFile.lastIndexOf('/');
			if (i != -1) { //Trim the BasePath, why is it even here!?
				configurationFile = configurationFile.substring(i + 1);
			}
		}
		return configurationFile;
	}

	/**
	 * Get the version (configuration.version + configuration.timestmap) 
	 * from the configuration's AppConstants
	 */
	public static String getConfigurationVersion(ClassLoader classLoader) {
		return getConfigurationVersion(AppConstants.getInstance(classLoader));
	}

	private static String getConfigurationVersion(Properties properties) {
		return getVersion(properties, "configuration.version", "configuration.timestamp");
	}

	/**
	 * Get the application version (instance.version + instance.timestamp)
	 */
	public static String getApplicationVersion() {
		return getVersion(AppConstants.getInstance(), "instance.version", "instance.timestamp");
	}

	private static String getVersion(Properties properties, String versionKey, String timestampKey) {
		String version = null;
		if (StringUtils.isNotEmpty(properties.getProperty(versionKey))) {
			version = properties.getProperty(versionKey);
			if (StringUtils.isNotEmpty(properties.getProperty(timestampKey))) {
				version = version + "_" + properties.getProperty(timestampKey);
			}
		}
		return version;
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name) throws ConfigurationException {
		return getConfigFromDatabase(ibisContext, name, null);
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm) throws ConfigurationException {
		return getConfigFromDatabase(ibisContext, name, jmsRealm, null);
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String dataSourceName, String version) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}
		if (StringUtils.isEmpty(version)) {
			version = null; //Make sure this is null when empty!
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			String query;
			if(version == null) {//Return active config
				query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG='"+(qs.getDbmsSupport().getBooleanValue(true))+"'";
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				rs = stmt.executeQuery();
			}
			else {
				query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				stmt.setString(2, version);
				rs = stmt.executeQuery();
			}
			if (!rs.next()) {
				log.warn("no configuration found in database with name ["+name+"] version ["+version+"]");
				return null;
			}

			Map<String, Object> configuration = new HashMap<String, Object>(5);
			byte[] jarBytes = rs.getBytes(1);
			if(jarBytes == null) return null;

			configuration.put("CONFIG", jarBytes);
			configuration.put("VERSION", rs.getString(2));
			configuration.put("FILENAME", rs.getString(3));
			configuration.put("CREATED", rs.getString(4));
			configuration.put("USER", rs.getString(5));
			return configuration;
		} catch (SenderException e) {
			throw new ConfigurationException(e);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
	}

	/**
	 * @return name of the configuration if successful SQL update
	 * @throws ConfigurationException when SQL error occurs or filename validation fails
	 */
	public static String addConfigToDatabase(IbisContext ibisContext, String datasource, boolean activate_config, boolean automatic_reload, String fileName, InputStream file, String ruser) throws ConfigurationException {
		ConfigurationValidator configDetails = new ConfigurationValidator(file);
		if(addConfigToDatabase(ibisContext, datasource, activate_config, automatic_reload, configDetails.getName(), configDetails.getVersion(), fileName, configDetails.getJar(), ruser)) {
			return configDetails.getName() +": "+ configDetails.getVersion();
		}
		return null;
	}

	/**
	 * Validates if the buildinfo is present, and if the name and version properties are set
	 */
	public static class ConfigurationValidator {
		private String name = null;
		private String version = null;
		private byte[] jar = null;
		private String buildInfoFilename = null;

		public ConfigurationValidator(InputStream stream) throws ConfigurationException {
			String buildInfoFilename = "BuildInfo";
			if(StringUtils.isNotEmpty(ADDITIONAL_PROPERTIES_FILE_SUFFIX)) {
				buildInfoFilename += ADDITIONAL_PROPERTIES_FILE_SUFFIX;
			}
			this.buildInfoFilename = buildInfoFilename + ".properties";

			try {
				jar = Misc.streamToBytes(stream);

				read();
				validate();
			} catch(IOException e) {
				throw new ConfigurationException("unable to read jarfile", e);
			}
		}

		private void read() throws IOException, ConfigurationException {
			boolean isBuildInfoPresent = false;
			try (JarInputStream zipInputStream = new JarInputStream(getJar())) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextJarEntry()) != null) {
					if (!zipEntry.isDirectory()) {
						String entryName = zipEntry.getName();
						String fileName = FilenameUtils.getName(entryName);

						if(buildInfoFilename.equals(fileName)) {
							name = FilenameUtils.getPathNoEndSeparator(entryName);
							Properties props = new Properties();
							props.load(zipInputStream);
							version = getConfigurationVersion(props);

							isBuildInfoPresent = true;
							break;
						}
					}
				}
			}
			if(!isBuildInfoPresent) {
				throw new ConfigurationException("no ["+buildInfoFilename+"] present in configuration");
			}
		}

		private void validate() throws ConfigurationException {
			if(StringUtils.isEmpty(name))
				throw new ConfigurationException("unknown configuration name");
			if(StringUtils.isEmpty(version))
				throw new ConfigurationException("unknown configuration version");
		}

		public InputStream getJar() {
			return new ByteArrayInputStream(jar);
		}
		public String getName() {
			return name;
		}
		public String getVersion() {
			return version;
		}
	}

	public static Map<String, String> processMultiConfigZipFile(IbisContext ibisContext, String datasource, boolean activate_config, boolean automatic_reload, InputStream file, String ruser) throws IOException, ConfigurationException {
		Map<String, String> result = new LinkedHashMap<String, String>();
		if (file.available() > 0) {
			try (ZipInputStream zipInputStream = new ZipInputStream(file)) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					String entryName = zipEntry.getName();
					try {
						String configName = ConfigurationUtils.addConfigToDatabase(ibisContext, datasource, activate_config, automatic_reload, entryName, StreamUtil.dontClose(zipInputStream), ruser);
						result.put(configName, "loaded");
					} catch (ConfigurationException e) {
						log.error("an error occured while trying to store new configuration using datasource ["+datasource+"]", e);
						result.put(entryName, e.getMessage());
					}
				}
			}
		}
		return result;
	}

	public static boolean addConfigToDatabase(IbisContext ibisContext, String dataSourceName, boolean activate_config, boolean automatic_reload, String name, String version, String fileName, InputStream file, String ruser) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			int updated = 0;

			if (activate_config) {
				String query = ("UPDATE IBISCONFIG SET ACTIVECONFIG = '"+(qs.getDbmsSupport().getBooleanValue(false))+"' WHERE NAME=?");
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				updated = stmt.executeUpdate();
			}
			if (updated > 0) {
				String query = ("DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION = ?");
				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				stmt.setString(2, version);
				stmt.execute();
			}

			String query = ("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)");
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, name);
			stmt.setString(2, version);
			stmt.setString(3, fileName);
			stmt.setBinaryStream(4, file);
			if (StringUtils.isEmpty(ruser)) {
				stmt.setNull(5, Types.VARCHAR);
			} else {
				stmt.setString(5, ruser);
			}
			stmt.setObject(6, qs.getDbmsSupport().getBooleanValue(activate_config));
			stmt.setObject(7, qs.getDbmsSupport().getBooleanValue(automatic_reload));

			return stmt.executeUpdate() > 0;
		} catch (SenderException e) {
			throw new ConfigurationException(e);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
	}

	public static void removeConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm, String version) throws ConfigurationException {
		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();

			String query = ("DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION=?");
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, name);
			stmt.setString(2, version);
			stmt.execute();
		} catch (SenderException e) {
			throw new ConfigurationException(e);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
	}

	/**
	 * Set the all ACTIVECONFIG to false and specified version to true
	 * @param value 
	 */
	public static boolean activateConfig(IbisContext ibisContext, String name, String version, boolean value, String dataSourceName) throws SenderException, ConfigurationException, JdbcException, SQLException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		String booleanValueFalse = qs.getDbmsSupport().getBooleanValue(false);
		String booleanValueTrue = qs.getDbmsSupport().getBooleanValue(true);

		try {
			qs.open();
			conn = qs.getConnection();
			int updated = 0;

			String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
			PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
			selectStmt.setString(1, name);
			selectStmt.setString(2, version);
			rs = selectStmt.executeQuery();
			if(rs.next()) {
				String query = "UPDATE IBISCONFIG SET ACTIVECONFIG='"+booleanValueFalse+"' WHERE NAME=?";

				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				updated = stmt.executeUpdate();

				if(updated > 0) {
					String query2 = "UPDATE IBISCONFIG SET ACTIVECONFIG='"+booleanValueTrue+"' WHERE NAME=? AND VERSION=?";
					PreparedStatement stmt2 = conn.prepareStatement(query2);
					stmt2.setString(1, name);
					stmt2.setString(2, version);
					return (stmt2.executeUpdate() > 0) ? true : false;
				}
			}
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
		return false;
	}

	/**
	 * Toggle AUTORELOAD
	 */
	public static boolean autoReloadConfig(IbisContext ibisContext, String name, String version, boolean booleanValue, String dataSourceName) throws SenderException, ConfigurationException, JdbcException, SQLException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();

		try {
			qs.open();
			conn = qs.getConnection();

			String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
			PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
			selectStmt.setString(1, name);
			selectStmt.setString(2, version);
			rs = selectStmt.executeQuery();
			if(rs.next()) {
				String query = "UPDATE IBISCONFIG SET AUTORELOAD='"+qs.getDbmsSupport().getBooleanValue(booleanValue)+"' WHERE NAME=? AND VERSION=?";

				PreparedStatement stmt = conn.prepareStatement(query);
				stmt.setString(1, name);
				stmt.setString(2, version);
				return stmt.executeUpdate() > 0;
			}
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
		return false;
	}

	/**
	 * 
	 * @return A map with all configurations to load (KEY = ConfigurationName, VALUE = ClassLoaderType)
	 */
	public static Map<String, String> retrieveAllConfigNames(IbisContext ibisContext) {
		Map<String, String> allConfigNameItems = new LinkedHashMap<String, String>();

		StringTokenizer tokenizer = new StringTokenizer(CONFIGURATIONS, ",");
		while (tokenizer.hasMoreTokens()) {
			allConfigNameItems.put(tokenizer.nextToken(), null);
		}

		if (CONFIG_AUTO_FS_CLASSLOADER) {
			try {
				String configDir = AppConstants.getInstance().getProperty("configurations.directory");
				if(StringUtils.isEmpty(configDir))
					throw new IOException("property [configurations.directory] not set");
	
				File directory = new File(configDir);
				if(!directory.exists())
					throw new IOException("failed to open configurations.directory ["+configDir+"]");
				if(!directory.isDirectory())
					throw new IOException("configurations.directory ["+configDir+"] is not a valid directory");
	
				for (File subFolder : directory.listFiles()) {
					if(subFolder.isDirectory()) {
						allConfigNameItems.put(subFolder.getName(), "DirectoryClassLoader");
					}
				}
			} catch (Exception e) {
				ibisContext.log("*ALL*", null, "failed to autoload configurations", MessageKeeperLevel.WARN, e);
			}
		}
		if (CONFIG_AUTO_DB_CLASSLOADER) {
			log.info("scanning database for configurations");
			try {
				List<String> dbConfigNames = ConfigurationUtils.retrieveConfigNamesFromDatabase(ibisContext, null);
				if (dbConfigNames != null && !dbConfigNames.isEmpty()) {
					log.debug("found database configurations "+dbConfigNames.toString()+"");
					for (String dbConfigName : dbConfigNames) {
						if (allConfigNameItems.get(dbConfigName) == null)
							allConfigNameItems.put(dbConfigName, "DatabaseClassLoader");
						else
							log.warn("config ["+dbConfigName+"] already exists in "+allConfigNameItems+", cannot add same config twice");
					}
				}
				else {
					log.debug("did not find any database configurations");
				}
			}
			catch (ConfigurationException e) {
				ibisContext.log("*ALL*", null, "error retrieving database configurations", MessageKeeperLevel.WARN, e);
			}
		}

		log.info("found configurations to load ["+allConfigNameItems+"]");

		return allConfigNameItems;
	}

	public static List<String> retrieveConfigNamesFromDatabase(IbisContext ibisContext, String jmsRealm) throws ConfigurationException {
		return retrieveConfigNamesFromDatabase(ibisContext, jmsRealm, false);
	}

	public static List<String> retrieveConfigNamesFromDatabase(IbisContext ibisContext, String jmsRealm, boolean onlyAutoReload) throws ConfigurationException {
		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			String query = "SELECT DISTINCT(NAME) FROM IBISCONFIG WHERE ACTIVECONFIG='"+(qs.getDbmsSupport().getBooleanValue(true))+"'";
			if (onlyAutoReload) {
				query = query + " AND AUTORELOAD='"	+ (qs.getDbmsSupport().getBooleanValue(true)) + "'";
			}
			PreparedStatement stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();
			List<String> stringList = new ArrayList<String>();
			while (rs.next()) {
				stringList.add(rs.getString(1));
			}
			return stringList;
		} catch (SenderException e) {
			throw new ConfigurationException(e);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
	}

	@Deprecated
	public static String[] retrieveBuildInfo(InputStream inputStream) throws IOException {
		String name = null;
		String version = null;
		try {
			ConfigurationValidator configDefails = new ConfigurationValidator(inputStream);
			name = configDefails.getName();
			version = configDefails.getVersion();
		} catch (ConfigurationException e) { } //Do nothing
		return new String[] { name, version };
	}
}