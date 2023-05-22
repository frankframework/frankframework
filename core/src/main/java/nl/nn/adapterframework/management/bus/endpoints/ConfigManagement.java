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
package nl.nn.adapterframework.management.bus.endpoints;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BinaryResponseMessage;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.EmptyResponseMessage;
import nl.nn.adapterframework.management.bus.StringResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.management.bus.dto.ConfigurationDTO;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.CONFIGURATION)
public class ConfigManagement extends BusEndpointBase {

	private static final String HEADER_CONFIGURATION_VERSION_KEY = "version";

	/**
	 * The header 'loaded' is used to differentiate between the loaded and original (raw) XML.
	 * @return Configuration XML
	 */
	@ActionSelector(BusAction.GET)
	public Message<String> getXMLConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		boolean loadedConfiguration = BusMessageUtils.getBooleanHeader(message, "loaded", false);
		StringBuilder result = new StringBuilder();

		if(configurationName != null) {
			Configuration configuration = getConfigurationByName(configurationName);
			result.append(loadedConfiguration ? configuration.getLoadedConfiguration() : configuration.getOriginalConfiguration());
		} else {
			for (Configuration configuration : getIbisManager().getConfigurations()) {
				result.append(loadedConfiguration ? configuration.getLoadedConfiguration() : configuration.getOriginalConfiguration());
			}
		}

		return new StringResponseMessage(result.toString(), MediaType.APPLICATION_XML);
	}

	/**
	 * header configuration The name of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 * @return If the configuration is of type DatabaseClassLoader, the metadata of the configurations found in the database.
	 */
	@ActionSelector(BusAction.FIND)
	public Message<String> getConfigurationDetailsByName(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		if(StringUtils.isNotEmpty(configurationName)) {
			Configuration configuration = getConfigurationByName(configurationName);

			if("DatabaseClassLoader".equals(configuration.getClassLoaderType())) {
				String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
				List<ConfigurationDTO> configs = getConfigsFromDatabase(configurationName, datasourceName);

				for(ConfigurationDTO config: configs) {
					config.setLoaded(config.getVersion().equals(configuration.getVersion()));
				}

				return new JsonResponseMessage(configs);
			}

			return new JsonResponseMessage(Collections.singletonList(new ConfigurationDTO(configuration)));
		}

		List<ConfigurationDTO> configs = new LinkedList<>();
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			configs.add(new ConfigurationDTO(configuration));
		}
		configs.sort(new ConfigurationDTO.NameComparator());
		return new JsonResponseMessage(configs);
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
	public Message<String> manageConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists

		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		Boolean activate = BusMessageUtils.getBooleanHeader(message, "activate", null);
		Boolean autoreload = BusMessageUtils.getBooleanHeader(message, "autoreload", null);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);

		try {
			if(activate != null) {
				if(ConfigurationUtils.activateConfig(getApplicationContext(), configurationName, version, datasourceName)) {
					return EmptyResponseMessage.accepted();
				}
			}
			else if(autoreload != null && ConfigurationUtils.autoReloadConfig(getApplicationContext(), configurationName, version, autoreload, datasourceName)) {
				return EmptyResponseMessage.accepted();
			}
		} catch(Exception e) {
			throw new BusException("unable to update configuration settings in database", e);
		}

		throw new BusException("neither [activate] or [autoreload] provided");
	}

	@ActionSelector(BusAction.UPLOAD)
	public Message<String> uploadConfiguration(Message<InputStream> message) {
		boolean multipleConfigs = BusMessageUtils.getBooleanHeader(message, "multiple_configs", false);
		boolean activateConfig = BusMessageUtils.getBooleanHeader(message, "activate_config", true);
		boolean automaticReload = BusMessageUtils.getBooleanHeader(message, "automatic_reload", false);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
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

			return new JsonResponseMessage(result);
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
	public BinaryResponseMessage downloadConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists
		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);

		Map<String, Object> configuration;
		try {
			configuration = ConfigurationUtils.getConfigFromDatabase(getApplicationContext(), configurationName, datasourceName, version);
		} catch (ConfigurationException e) {
			throw new BusException("unable to download configuration from database", e);
		}
		byte[] config = (byte[]) configuration.get("CONFIG");

		BinaryResponseMessage response = new BinaryResponseMessage(config);
		response.setFilename(""+configuration.get("FILENAME"));
		return response;
	}

	/**
	 * header configuration The name of the Configuration to delete
	 * header version The version of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.DELETE)
	public void deleteConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists
		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		String datasourceName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);

		try {
			ConfigurationUtils.removeConfigFromDatabase(getApplicationContext(), configurationName, datasourceName, version);
		} catch (Exception e) {
			throw new BusException("unable to delete configuration from database", e);
		}
	}

	private List<ConfigurationDTO> getConfigsFromDatabase(String configurationName, @Nonnull final String dataSourceName) {
		List<ConfigurationDTO> configurations = new LinkedList<>();

		FixedQuerySender qs = createBean(FixedQuerySender.class);
		qs.setDatasourceName(dataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		try {
			qs.configure();
			qs.open();
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
							Date creationDate = rs.getDate(7);

							configDoa.setDatabaseAttributes(filename, creationDate, user, active, autoreload);
							configurations.add(configDoa);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new BusException("unable to retrieve configuration from database", e);
		} finally {
			qs.close();
		}

		configurations.sort(new ConfigurationDTO.VersionComparator());
		return configurations;
	}
}
