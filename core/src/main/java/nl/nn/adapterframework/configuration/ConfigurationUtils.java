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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Functions to manipulate the configuration. 
 *
 * @author  Peter Leeuwenburgh
 * @author  Jaco de Groot
 */
public class ConfigurationUtils {
	private static Logger log = LogUtil.getLogger(ConfigurationUtils.class);

	public static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	public static final String STUB4TESTTOOL_VALIDATORS_DISABLED_KEY = "validators.disabled";
	public static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	public static final String STUB4TESTTOOL_XSLT = "/xml/xsl/stub4testtool.xsl";

	public static final String FRANK_CONFIG_XSD = "/xml/xsd/FrankConfig-compatibility.xsd";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.database.autoLoad", false);
	private static final boolean CONFIG_AUTO_FS_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.directory.autoLoad", false);
	private static final String CONFIGURATIONS = APP_CONSTANTS.getResolvedProperty("configurations.names.application");
	public static final String DEFAULT_CONFIGURATION_FILE = "Configuration.xml";

	/**
	 * Checks if a configuration is stubbed or not
	 */
	public static boolean isConfigurationStubbed(ClassLoader classLoader) {
		return AppConstants.getInstance(classLoader).getBoolean(STUB4TESTTOOL_CONFIGURATION_KEY, false);
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

	protected static String getConfigurationVersion(Properties properties) {
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

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String dataSourceName) throws ConfigurationException {
		return getConfigFromDatabase(ibisContext, name, dataSourceName, null);
	}

	public static Map<String, Object> getConfigFromDatabase(IbisContext ibisContext, String name, String dataSourceName, String version) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}
		if (StringUtils.isEmpty(version)) {
			version = null; //Make sure this is null when empty!
		}
		if(log.isInfoEnabled()) log.info("trying to fetch configuration [{}] version [{}] from database with dataSourceName [{}]", name, version, workdataSourceName);

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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
				log.error("no configuration found in database with name ["+name+"] " + (version!=null ? "version ["+version+"]" : "activeconfig [TRUE]"));
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
		} catch (SenderException | JdbcException | SQLException e) {
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
		BuildInfoValidator configDetails = new BuildInfoValidator(file);
		if(addConfigToDatabase(ibisContext, datasource, activate_config, automatic_reload, configDetails.getName(), configDetails.getVersion(), fileName, configDetails.getJar(), ruser)) {
			return configDetails.getName() +": "+ configDetails.getVersion();
		}
		return null;
	}

	public static Map<String, String> processMultiConfigZipFile(IbisContext ibisContext, String datasource, boolean activate_config, boolean automatic_reload, InputStream file, String ruser) throws IOException, ConfigurationException {
		Map<String, String> result = new LinkedHashMap<>();
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

	public static boolean addConfigToDatabase(IbisContext ibisContext, String dataSourceName, boolean activateConfig, boolean automaticReload, String name, String version, String fileName, InputStream file, String ruser) throws ConfigurationException {
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

		PlatformTransactionManager txManager = ibisContext.getBean("txManager", PlatformTransactionManager.class);
		TransactionDefinition txDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		IbisTransaction itx = new IbisTransaction(txManager , txDef, "add config ["+name+"] to database");
		try {
			qs.open();
			conn = qs.getConnection();
			int updated = 0;

			if (activateConfig) {
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

			String activeBool = qs.getDbmsSupport().getBooleanValue(activateConfig);
			String reloadBool = qs.getDbmsSupport().getBooleanValue(automaticReload);
			String query = ("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, "+activeBool+", "+reloadBool+")");
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

			return stmt.executeUpdate() > 0;
		} catch (SenderException | JdbcException | SQLException e) {
			itx.setRollbackOnly();
			throw new ConfigurationException(e);
		} finally {
			itx.commit();
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
	}

	public static void removeConfigFromDatabase(IbisContext ibisContext, String name, String jmsRealm, String version) throws ConfigurationException {
		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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
		} catch (JdbcException | SQLException e) {
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
		FixedQuerySender qs = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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
		FixedQuerySender qs = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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
		Map<String, String> allConfigNameItems = new LinkedHashMap<>();

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
				ibisContext.log("failed to autoload configurations", MessageKeeperLevel.WARN, e);
			}
		}
		if (CONFIG_AUTO_DB_CLASSLOADER) {
			log.info("scanning database for configurations");
			try {
				List<String> dbConfigNames = ConfigurationUtils.retrieveConfigNamesFromDatabase(ibisContext);
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
				ibisContext.log("error retrieving database configurations", MessageKeeperLevel.WARN, e);
			}
		}

		log.info("found configurations to load ["+allConfigNameItems+"]");

		return allConfigNameItems;
	}

	public static List<String> retrieveConfigNamesFromDatabase(IbisContext ibisContext) throws ConfigurationException {
		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
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
		} catch (SenderException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			JdbcUtil.fullClose(conn, rs);
			qs.close();
		}
	}
}