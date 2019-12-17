/*
   Copyright 2013, 2016-2019 Nationale-Nederlanden

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeperMessage;
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
	private static final String UGLIFY_XSLT = "/xml/xsl/uglify.xsl";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.autoDatabaseClassLoader", false);
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
		return getTweakedConfiguration(configuration, originalConfig, STUB4TESTTOOL_XSLT, parameters);
	}

	public static String getActivatedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		return getTweakedConfiguration(configuration, originalConfig, ACTIVE_XSLT, null);
	}

	public static String getUglifiedConfiguration(Configuration configuration, String originalConfig) throws ConfigurationException {
		return getTweakedConfiguration(configuration, originalConfig, UGLIFY_XSLT, null);
	}

	public static String getTweakedConfiguration(Configuration configuration,
			String originalConfig, String tweakXslt,
			Map<String, Object> parameters) throws ConfigurationException {
		URL tweak_xsltSource = ClassUtils.getResourceURL(configuration.getClassLoader(), tweakXslt);
		if (tweak_xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + tweakXslt + "]");
		}
		try {
			Transformer tweak_transformer = XmlUtils.createTransformer(tweak_xsltSource);
			XmlUtils.setTransformerParameters(tweak_transformer, parameters);
			// Use namespaceAware=true, otherwise for some reason the
			// transformation isn't working with a SAXSource, in system out it
			// generates:
			// jar:file: ... .jar!/xml/xsl/active.xsl; Line #34; Column #13; java.lang.NullPointerException
			return XmlUtils.transformXml(tweak_transformer, originalConfig, true);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + tweakXslt + "]", e);
		} catch (SAXException|TransformerConfigurationException e) {
			throw new ConfigurationException("got error creating transformer from file [" + tweakXslt + "]", e);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + tweak_xsltSource.toString() + "] from [" + tweakXslt + "]", te);
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
		}
		return configurationFile;
	}

	public static String getConfigurationVersion(ClassLoader classLoader) {
		return getVersion(classLoader, "configuration.version", "configuration.timestamp");
	}

	public static String getApplicationVersion() {
		return getVersion(ConfigurationUtils.class.getClassLoader(), "instance.version", "instance.timestamp");
	}

	private static String getVersion(ClassLoader classLoader, String versionKey, String timestampKey) {
		AppConstants constants = AppConstants.getInstance(classLoader);
		String version = null;
		if (StringUtils.isNotEmpty(constants.getProperty(versionKey))) {
			version = constants.getProperty(versionKey);
			if (StringUtils.isNotEmpty(constants.getProperty(timestampKey))) {
				version = version + "_" + constants.getProperty(timestampKey);
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

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm, String version) throws ConfigurationException {
		String workJmsRealm = jmsRealm;
		if (StringUtils.isEmpty(workJmsRealm)) {
			workJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(workJmsRealm)) {
				log.warn("no JMSRealm found");
				return null;
			}
		}
		if (StringUtils.isEmpty(version)) {
			version = null; //Make sure this is null when empty!
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(workJmsRealm);
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
			qs.close();
			JdbcUtil.fullClose(conn, rs);
		}
	}

	public static boolean addConfigToDatabase(IbisContext ibisContext, String jmsRealm, boolean activate_config, boolean automatic_reload, String name, String version, String fileName, InputStream file, String ruser) throws ConfigurationException {
		String workJmsRealm = jmsRealm;
		if (StringUtils.isEmpty(workJmsRealm)) {
			workJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(workJmsRealm)) {
				return false;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(workJmsRealm);
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
			qs.close();
			JdbcUtil.fullClose(conn, rs);
		}
	}

	public static void removeConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm, String version) throws ConfigurationException {
		String workJmsRealm = jmsRealm;
		if (StringUtils.isEmpty(workJmsRealm)) {
			workJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(workJmsRealm)) {
				throw new ConfigurationException("no JmsRealm found");
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(workJmsRealm);
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
			qs.close();
			JdbcUtil.fullClose(conn, rs);
		}
	}

	/**
	 * Set the all ACTIVECONFIG to false and specified version to true
	 * @param value 
	 */
	public static boolean activateConfig(IbisContext ibisContext, String name, String version, boolean value, String jmsRealm) throws SenderException, ConfigurationException, JdbcException, SQLException {
		String workJmsRealm = jmsRealm;
		if (StringUtils.isEmpty(workJmsRealm)) {
			workJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(workJmsRealm)) {
				return false;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(workJmsRealm);
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
			qs.close();
			JdbcUtil.fullClose(conn, rs);
		}
		return false;
	}

	/**
	 * Toggle AUTORELOAD
	 */
	public static boolean autoReloadConfig(IbisContext ibisContext, String name, String version, boolean booleanValue, String jmsRealm) throws SenderException, ConfigurationException, JdbcException, SQLException {
		String workJmsRealm = jmsRealm;
		if (StringUtils.isEmpty(workJmsRealm)) {
			workJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(workJmsRealm)) {
				return false;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(workJmsRealm);
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
			qs.close();
			JdbcUtil.fullClose(conn, rs);
		}
		return false;
	}

	/**
	 * 
	 * @param ibisContext
	 * @return A map with all configurations to load (KEY = configName, VALUE = ClassLoader)
	 */
	public static Map<String, String> retrieveAllConfigNames(IbisContext ibisContext) {
		// For now only database configurations are returned, but also
		// configuration from other resources (like file system directories) can
		// be added
		Map<String, String> allConfigNameItems = new LinkedHashMap<String, String>();

		StringTokenizer tokenizer = new StringTokenizer(CONFIGURATIONS, ",");
		while (tokenizer.hasMoreTokens()) {
			allConfigNameItems.put(tokenizer.nextToken(), null);
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
				ibisContext.log("*ALL*", null, "error retrieving database configurations", MessageKeeperMessage.WARN_LEVEL, e);
			}
		}

		log.info("found configurations to load ["+allConfigNameItems+"]");

		return allConfigNameItems;
	}

	public static List<String> retrieveConfigNamesFromDatabase(IbisContext ibisContext, String jmsRealm) throws ConfigurationException {
		String workJmsRealm = jmsRealm;
		if (StringUtils.isEmpty(workJmsRealm)) {
			workJmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(workJmsRealm)) {
				return null;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(workJmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		qs.configure();
		try {
			qs.open();
			conn = qs.getConnection();
			String query = "SELECT DISTINCT(NAME) FROM IBISCONFIG WHERE ACTIVECONFIG='"+(qs.getDbmsSupport().getBooleanValue(true))+"'";
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
			qs.close();
			JdbcUtil.fullClose(conn, rs);
		}
	}

	public static String[] retrieveBuildInfo(InputStream inputStream)
			throws IOException {
		ZipInputStream zipInputStream = new ZipInputStream(
				new BufferedInputStream(inputStream));
		ZipEntry zipEntry;
		String name = null;
		String version = null;
		boolean buildInfoFound = false;
		while ((zipEntry = zipInputStream.getNextEntry()) != null
				&& !buildInfoFound) {
			if (!zipEntry.isDirectory()) {
				String entryName = zipEntry.getName();
				String entryNameMinusPath = FilenameUtils.getName(entryName);

				String buildInfoFilename = "BuildInfo";
				if(StringUtils.isNotEmpty(ADDITIONAL_PROPERTIES_FILE_SUFFIX))
					buildInfoFilename += ADDITIONAL_PROPERTIES_FILE_SUFFIX;

				if((buildInfoFilename+".properties").equals(entryNameMinusPath)) {
					name = FilenameUtils.getPathNoEndSeparator(entryName);
					byte[] b = new byte[4096];
					int rb = 0;
					int direct;
					while ((direct = zipInputStream.read(b, 0,
							b.length)) >= 0) {
						rb = rb + direct;
					}
					String buildInfoContent = new String(b, 0, rb);
					Properties props = new Properties();
					props.load(new StringReader(buildInfoContent));
					version = props.getProperty("configuration.version") + "_"
							+ props.getProperty("configuration.timestamp");
					buildInfoFound = true;
				}
			}
			zipInputStream.closeEntry();
		}
		zipInputStream.close();
		return new String[] { name, version };
	}
}