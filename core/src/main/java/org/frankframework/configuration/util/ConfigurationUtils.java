/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.configuration.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.core.IbisTransaction;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.SpringUtils;

/**
 * Functions to manipulate the configuration.
 *
 * @author  Peter Leeuwenburgh
 * @author  Jaco de Groot
 */
@Log4j2
public class ConfigurationUtils {
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.database.autoLoad", false);
	private static final boolean CONFIG_AUTO_FS_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.directory.autoLoad", false);
	private static final String INSTANCE_NAME = AppConstants.getInstance().getProperty("instance.name", null);
	private static final String DUMMY_SELECT_QUERY = "SELECT COUNT(*) FROM IBISCONFIG";

	public static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	public static final String STUB4TESTTOOL_VALIDATORS_DISABLED_KEY = "validators.disabled";
	public static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	public static final String STUB4TESTTOOL_XSLT_KEY = "stub4testtool.xsl";
	public static final String STUB4TESTTOOL_XSLT_DEFAULT = "/xml/xsl/stub4testtool.xsl";

	public static final String FRANK_CONFIG_XSD = "/xml/xsd/FrankConfig-compatibility.xsd";
	public static final String DEFAULT_CONFIGURATION_FILE = "Configuration.xml";

	private ConfigurationUtils() {
		// Private constructor so that the utility-class cannot be instantiated.
	}

	/**
	 * Checks if a configuration is stubbed or not
	 */
	public static boolean isConfigurationStubbed(ClassLoader classLoader) {
		return AppConstants.getInstance(classLoader).getBoolean(STUB4TESTTOOL_CONFIGURATION_KEY, false);
	}

	public static boolean isConfigurationXmlOptional(Configuration configuration) {
		return CONFIG_AUTO_FS_CLASSLOADER &&
				configuration.getClassLoader() instanceof WebAppClassLoader &&
				configuration.getName().equals(INSTANCE_NAME);
	}

	public static String getConfigurationFile(ClassLoader classLoader, String currentConfigurationName) {
		String configFileKey = "configurations." + currentConfigurationName + ".configurationFile";
		String configurationFile = AppConstants.getInstance(classLoader).getProperty(configFileKey);
		if (StringUtils.isEmpty(configurationFile)) {
			configurationFile = AppConstants.getInstance(classLoader.getParent()).getProperty(configFileKey);
		}
		if (StringUtils.isEmpty(configurationFile)) {
			configurationFile = DEFAULT_CONFIGURATION_FILE;
		} else {
			int i = configurationFile.lastIndexOf('/');
			if (i != -1) { // Trim the BasePath, why is it even here!?
				configurationFile = configurationFile.substring(i + 1);
			}
		}
		return configurationFile;
	}

	/**
	 * Get the version (configuration.version + configuration.timestamp) from the configuration's AppConstants.
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

	public static List<Map<String, Object>> getActiveConfigsFromDatabase(ApplicationContext applicationContext, String dataSourceName) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		if(log.isInfoEnabled()) log.info("trying to fetch all active configurations from database with dataSourceName [{}]", workdataSourceName);

		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try(Connection conn = qs.getConnection()) {
				String query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(true));
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					return extractConfigurationsFromResultSet(stmt);
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}

	public static Map<String, Object> getActiveConfigFromDatabase(ApplicationContext applicationContext, String name, String dataSourceName) throws ConfigurationException {
		return getConfigFromDatabase(applicationContext, name, dataSourceName, null);
	}

	public static Map<String, Object> getConfigFromDatabase(ApplicationContext applicationContext, String name, String dataSourceName, String version) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}
		if (StringUtils.isEmpty(version)) {
			version = null; // Make sure this is null when empty!
		}
		if(log.isInfoEnabled()) log.info("trying to fetch configuration [{}] version [{}] from database with dataSourceName [{}]", name, version, workdataSourceName);

		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try(Connection conn = qs.getConnection()) {
				if (version == null) { // Return active config
					String query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(true));
					try (PreparedStatement stmt = conn.prepareStatement(query)) {
						stmt.setString(1, name);
						return extractConfigurationFromResultSet(stmt, name, null);
					}
				}
				else {
					String query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
					try (PreparedStatement stmt = conn.prepareStatement(query)) {
						stmt.setString(1, name);
						stmt.setString(2, version);
						return extractConfigurationFromResultSet(stmt, name, version);
					}
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}

	private static Map<String, Object> extractConfigurationFromResultSetRow (ResultSet rs) throws SQLException {
		Map<String, Object> configuration = new HashMap<>(5);
		byte[] jarBytes = rs.getBytes(1);
		if (jarBytes == null) return null;

		configuration.put("CONFIG", jarBytes);
		configuration.put("VERSION", rs.getString(2));
		configuration.put("FILENAME", rs.getString(3));
		configuration.put("CREATED", rs.getString(4));
		configuration.put("USER", rs.getString(5));
		return configuration;
	}

	private static List<Map<String, Object>> extractConfigurationsFromResultSet(PreparedStatement stmt) throws SQLException {
		try(ResultSet rs = stmt.executeQuery()) {
			List<Map<String, Object>> configs = new ArrayList<>();

			while (rs.next()) {
				Map<String, Object> rowConfig = extractConfigurationFromResultSetRow(rs);
				if(rowConfig != null) {
					configs.add(rowConfig);
				}
			}

			return configs;
		}
	}

	private static Map<String, Object> extractConfigurationFromResultSet(PreparedStatement stmt, String name, String version) throws SQLException {
		try(ResultSet rs = stmt.executeQuery()) {
			if (!rs.next()) {
				log.error("no configuration found in database with name [{}] {}", name, version != null ? "version [" + version + "]" : "activeconfig [TRUE]");
				return null;
			}

			return extractConfigurationFromResultSetRow(rs);
		}
	}

	/**
	 * @return name of the configuration if successful SQL update
	 * @throws ConfigurationException when SQL error occurs or filename validation fails
	 */
	public static String addConfigToDatabase(ApplicationContext applicationContext, String datasource, boolean activate_config, boolean automatic_reload, String fileName, InputStream file, String ruser) throws ConfigurationException {
		BuildInfoValidator configDetails = new BuildInfoValidator(file);
		if(addConfigToDatabase(applicationContext, datasource, activate_config, automatic_reload, configDetails.getName(), configDetails.getVersion(), fileName, configDetails.getJar(), ruser)) {
			return configDetails.getName() +": "+ configDetails.getVersion();
		}
		return null;
	}

	public static Map<String, String> processMultiConfigZipFile(ApplicationContext applicationContext, String datasource, boolean activate_config, boolean automatic_reload, InputStream file, String ruser) throws IOException, ConfigurationException {
		Map<String, String> result = new LinkedHashMap<>();
		if (file.available() > 0) {
			try (ZipInputStream zipInputStream = new ZipInputStream(file)) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					String entryName = zipEntry.getName();
					try {
						String configName = ConfigurationUtils.addConfigToDatabase(applicationContext, datasource, activate_config, automatic_reload, entryName, CloseUtils.dontClose(zipInputStream), ruser);
						result.put(configName, "loaded");
					} catch (ConfigurationException e) {
						log.error("an error occurred while trying to store new configuration using datasource [{}]", datasource, e);
						result.put(entryName, e.getMessage());
					}
				}
			}
		}
		return result;
	}

	public static boolean addConfigToDatabase(ApplicationContext applicationContext, String dataSourceName, boolean activateConfig, boolean automaticReload, String name, String version, String fileName, InputStream file, String ruser) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();

		PlatformTransactionManager txManager = applicationContext.getBean("txManager", PlatformTransactionManager.class);
		TransactionDefinition txDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		IbisTransaction itx = new IbisTransaction(txManager , txDef, "add config ["+name+"] to database");
		try {
			qs.start();
			conn = qs.getConnection();
			int updated = 0;

			if (activateConfig) {
				String query = "UPDATE IBISCONFIG SET ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(false))+" WHERE NAME=?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, name);
					updated = stmt.executeUpdate();
				}
			}
			if (updated > 0) {
				String query = "DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION = ?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, name);
					stmt.setString(2, version);
					stmt.execute();
				}
			}

			String activeBool = qs.getDbmsSupport().getBooleanValue(activateConfig);
			String reloadBool = qs.getDbmsSupport().getBooleanValue(automaticReload);
			String query = "INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, "+activeBool+", "+reloadBool+")";
			try (PreparedStatement stmt = conn.prepareStatement(query)) {
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
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			itx.setRollbackOnly();
			throw new ConfigurationException(e);
		} finally {
			itx.complete();
			JdbcUtil.close(conn);
			qs.stop();
		}
	}

	public static void removeConfigFromDatabase(ApplicationContext applicationContext, String name, String datasourceName, String version) throws ConfigurationException {
		String workdataSourceName = datasourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try (Connection conn = qs.getConnection()) {
				String query = "DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, name);
					stmt.setString(2, version);
					stmt.execute();
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}

	/**
	 * Set the all ACTIVECONFIG to false and specified version to true
	 */
	public static boolean activateConfig(ApplicationContext applicationContext, String name, String version, String dataSourceName) throws ConfigurationException, JdbcException, SQLException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		String booleanValueFalse = qs.getDbmsSupport().getBooleanValue(false);
		String booleanValueTrue = qs.getDbmsSupport().getBooleanValue(true);

		try {
			qs.start();
			conn = qs.getConnection();
			int updated;

			String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
			try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
				selectStmt.setString(1, name);
				selectStmt.setString(2, version);
				try (ResultSet rs = selectStmt.executeQuery()) {
					if (rs.next()) {
						String query = "UPDATE IBISCONFIG SET ACTIVECONFIG=" + booleanValueFalse + " WHERE NAME=?";

						try (PreparedStatement stmt = conn.prepareStatement(query)) {
							stmt.setString(1, name);
							updated = stmt.executeUpdate();
						}
						if (updated > 0) {
							String query2 = "UPDATE IBISCONFIG SET ACTIVECONFIG=" + booleanValueTrue + " WHERE NAME=? AND VERSION=?";
							try (PreparedStatement stmt2 = conn.prepareStatement(query2)) {
								stmt2.setString(1, name);
								stmt2.setString(2, version);
								return stmt2.executeUpdate() > 0;
							}
						}
					}
				}
			}
		} finally {
			JdbcUtil.close(conn);
			qs.stop();
		}
		return false;
	}

	/**
	 * Toggle AUTORELOAD
	 */
	public static boolean autoReloadConfig(ApplicationContext applicationContext, String name, String version, boolean booleanValue, String dataSourceName) throws ConfigurationException, JdbcException, SQLException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		FixedQuerySender qs =  SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();

		try {
			qs.start();
			try (Connection conn = qs.getConnection()) {
				String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
				try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
					selectStmt.setString(1, name);
					selectStmt.setString(2, version);
					try (ResultSet rs = selectStmt.executeQuery()) {
						if (rs.next()) {
							String query = "UPDATE IBISCONFIG SET AUTORELOAD=" + qs.getDbmsSupport().getBooleanValue(booleanValue) + " WHERE NAME=? AND VERSION=?";

							try (PreparedStatement stmt = conn.prepareStatement(query)) {
								stmt.setString(1, name);
								stmt.setString(2, version);
								return stmt.executeUpdate() > 0;
							}
						}
					}
				}
			}
		} finally {
			qs.stop();
		}
		return false;
	}

	@Nonnull
	public static Path getConfigurationDirectory() throws IOException {
		String configDir = AppConstants.getInstance().getProperty("configurations.directory");
		if (configDir == null) {
			throw new IOException("Could not find property configurations.directory");
		}

		Path configPath = Path.of(FilenameUtils.normalize(configDir, true));
		if (!Files.isDirectory(configPath)) {
			throw new IOException("path ["+configDir+"] is not a valid directory");
		}
		return configPath;
	}

	/**
	 * @return A map with all configurations to load (KEY = ConfigurationName, VALUE = ClassLoaderType)
	 */
	public static Map<String, Class<? extends IConfigurationClassLoader>> retrieveAllConfigNames(ApplicationContext applicationContext) {
		ConfigurationAutoDiscovery discovery = SpringUtils.createBean(applicationContext);
		try {
			if (CONFIG_AUTO_FS_CLASSLOADER) discovery.withDirectoryScanner();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		if (CONFIG_AUTO_DB_CLASSLOADER) discovery.withDatabaseScanner();

		return discovery.scan(true);
	}
}
