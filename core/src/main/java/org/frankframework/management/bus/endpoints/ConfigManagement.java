/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.dto.ConfigurationDTO;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.StringMessage;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.CONFIGURATION)
public class ConfigManagement extends BusEndpointBase {

	private static final String HEADER_CONFIGURATION_VERSION_KEY = "version";
	private static final String ROOT_ELEMENT_NAME = "configurations";

	/**
	 * The header 'loaded' is used to differentiate between the loaded and original (raw) XML.
	 * @return Configuration XML
	 */
	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getXMLConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		boolean loadedConfiguration = BusMessageUtils.getBooleanHeader(message, "loaded", false);
		StringBuilder result = new StringBuilder();

		result.append("<" + ROOT_ELEMENT_NAME + ">");

		if(configurationName != null) {
			Configuration configuration = getConfigurationByName(configurationName);
			result.append(loadedConfiguration ? configuration.getLoadedConfiguration() : configuration.getOriginalConfiguration());
		} else {
			for (Configuration configuration : getIbisManager().getConfigurations()) {
				result.append(loadedConfiguration ? configuration.getLoadedConfiguration() : configuration.getOriginalConfiguration());
			}
		}

		result.append("</" + ROOT_ELEMENT_NAME + ">");

		return new StringMessage(result.toString(), MediaType.APPLICATION_XML);
	}

	/**
	 * header configuration The name of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 * @return If the configuration is of type DatabaseClassLoader, the metadata of the configurations found in the database.
	 */
	@ActionSelector(BusAction.FIND)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getConfigurationDetailsByName(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		if(StringUtils.isNotEmpty(configurationName)) {
			Configuration configuration = getConfigurationByName(configurationName);

			if("DatabaseClassLoader".equals(configuration.getClassLoaderType())) {
				String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
				List<ConfigurationDTO> configs = getConfigsFromDatabase(configurationName, datasourceName);

				for(ConfigurationDTO config: configs) {
					config.setLoaded(config.getVersion().equals(configuration.getVersion()));
				}

				return new JsonMessage(configs);
			}

			return new JsonMessage(Collections.singletonList(new ConfigurationDTO(configuration)));
		}

		List<ConfigurationDTO> configs = getIbisManager().getConfigurations()
				.stream()
				.map(ConfigurationDTO::new)
				.sorted(new ConfigurationDTO.NameComparator())
				.toList();
		return new JsonMessage(configs);
	}

	/**
	 * header configuration The name of the Configuration to manage
	 * header version The version of the Configuration to find
	 * header activate Whether the configuration should be activated
	 * header autoreload Whether the configuration should be reloaded (on the next ReloadJob interval)
	 * header datasourceName The name of the datasource where the configurations are located.
	 * @return Manages a configuration, either activates the config directly or sets the autoreload flag in the database
	 */
	@ActionSelector(BusAction.MANAGE)
	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	public Message<String> manageConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists

		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		Boolean activate = BusMessageUtils.getBooleanHeader(message, "activate", null);
		Boolean autoreload = BusMessageUtils.getBooleanHeader(message, "autoreload", null);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);

		try {
			if(activate != null) {
				if(ConfigurationUtils.activateConfig(getApplicationContext(), configurationName, version, datasourceName)) {
					return EmptyMessage.accepted();
				}
			}
			else if(autoreload != null && ConfigurationUtils.autoReloadConfig(getApplicationContext(), configurationName, version, autoreload, datasourceName)) {
				return EmptyMessage.accepted();
			}
		} catch(Exception e) {
			throw new BusException("unable to update configuration settings in database", e);
		}

		throw new BusException("neither [activate] or [autoreload] provided");
	}

	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	public Message<String> uploadConfiguration(Message<InputStream> message) {
		boolean multipleConfigs = BusMessageUtils.getBooleanHeader(message, "multiple_configs", false);
		boolean activateConfig = BusMessageUtils.getBooleanHeader(message, "activate_config", true);
		boolean automaticReload = BusMessageUtils.getBooleanHeader(message, "automatic_reload", false);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		String user = BusMessageUtils.getHeader(message, "user");
		InputStream file = message.getPayload();
		String filename = BusMessageUtils.getHeader(message, "filename");

		Map<String, String> result = new LinkedHashMap<>();
		try {
			if(multipleConfigs) {
				result = ConfigurationUtils.processMultiConfigZipFile(getApplicationContext(), datasourceName, activateConfig, automaticReload, file, user);
			} else {
				String configName=ConfigurationUtils.addConfigToDatabase(getApplicationContext(), datasourceName, activateConfig, automaticReload, filename, file, user);
				if(configName != null) {
					result.put(configName, "loaded");
				}
			}

			return new JsonMessage(result);
		} catch (Exception e) {
			throw new BusException("failed to upload Configuration", e);
		}
	}

	/**
	 * header configuration The name of the Configuration to download
	 * header version The version of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.DOWNLOAD)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public BinaryMessage downloadConfiguration(Message<?> message) throws IOException {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		if (BusMessageUtils.ALL_CONFIGS_KEY.equals(configurationName)) {
			String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
			List<Map<String, Object>> activeConfigsFromDb;
			try {
				activeConfigsFromDb = ConfigurationUtils.getActiveConfigsFromDatabase(getApplicationContext(), datasourceName);
			} catch (ConfigurationException e) {
				throw new BusException("unable to download configurations from database", e);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (ZipOutputStream zos = new ZipOutputStream(out)) {
				for (Map<String, Object> activeConfig : activeConfigsFromDb) {
					byte[] configFile = (byte[]) activeConfig.get("CONFIG");
					ZipEntry entry = new ZipEntry(""+activeConfig.get("FILENAME"));
					zos.putNextEntry(entry);
					zos.write(configFile);
					zos.closeEntry();
				}
			}

			BinaryMessage response = new BinaryMessage(out.toByteArray());
			response.setFilename("active_configurations.zip");
			return response;
		}

		getConfigurationByName(configurationName); //Validate the configuration exists
		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);

		Map<String, Object> configuration;
		try {
			configuration = ConfigurationUtils.getConfigFromDatabase(getApplicationContext(), configurationName, datasourceName, version);
		} catch (ConfigurationException e) {
			throw new BusException("unable to download configuration from database", e);
		}
		byte[] config = (byte[]) configuration.get("CONFIG");

		BinaryMessage response = new BinaryMessage(config);
		response.setFilename(""+configuration.get("FILENAME"));
		return response;
	}

	/**
	 * header configuration The name of the Configuration to delete
	 * header version The version of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.DELETE)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public void deleteConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists
		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);

		try {
			ConfigurationUtils.removeConfigFromDatabase(getApplicationContext(), configurationName, datasourceName, version);
		} catch (Exception e) {
			throw new BusException("unable to delete configuration from database", e);
		}
	}

	private List<ConfigurationDTO> getConfigsFromDatabase(String configurationName, @Nonnull final String dataSourceName) {
		List<ConfigurationDTO> configurations = new ArrayList<>();

		FixedQuerySender qs = createBean(FixedQuerySender.class);
		qs.setDatasourceName(dataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		try {
			qs.configure();
			qs.start();
			try (Connection conn = qs.getConnection()) {
				String query = "SELECT NAME, VERSION, FILENAME, RUSER, ACTIVECONFIG, AUTORELOAD, CRE_TYDST FROM IBISCONFIG WHERE NAME=?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, configurationName);
					try (ResultSet rs = stmt.executeQuery()) {
						while (rs.next()) {
							String name = rs.getString(1);
							String version = rs.getString(2);
							ConfigurationDTO configDoa = new ConfigurationDTO(name, version);

							String filename = rs.getString(3);
							String user = rs.getString(4);
							boolean active = rs.getBoolean(5);
							boolean autoreload = rs.getBoolean(6);
							Instant creationDate = rs.getTimestamp(7).toInstant();

							configDoa.setDatabaseAttributes(filename, creationDate, user, active, autoreload);
							configurations.add(configDoa);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new BusException("unable to retrieve configuration from database", e);
		} finally {
			qs.stop();
		}

		configurations.sort(new ConfigurationDTO.VersionComparator());
		return configurations;
	}
}
