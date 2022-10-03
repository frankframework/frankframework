/*
   Copyright 2022 WeAreFrank!

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.NameComparatorBase;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.webcontrol.api.ApiException;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.CONFIGURATION)
public class ConfigManagement {

	private String orderBy = AppConstants.getInstance().getProperty("iaf-api.configurations.orderby", "version").trim();
	private @Getter @Setter IbisManager ibisManager;
	private Logger log = LogUtil.getLogger(this);
	private static final String HEADER_CONFIGURATION_NAME_KEY = "configuration";
	private static final String HEADER_CONFIGURATION_VERSION_KEY = "version";
	private static final String HEADER_DATASOURCE_NAME_KEY = "datasourceName";

	private IbisContext getIbisContext() {
		return ibisManager.getIbisContext();
	}

	/**
	 * @return Configuration XML
	 * header loaded to differentiate between the loaded and original (raw) XML.
	 */
	@ActionSelector(BusAction.GET)
	public Message<String> getXMLConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_NAME_KEY);
		boolean loadedConfiguration = BusMessageUtils.getBooleanHeader(message, "loaded", false);
		StringBuilder result = new StringBuilder();

		if(configurationName != null) {
			Configuration configuration = getIbisManager().getConfiguration(configurationName);
			result.append(loadedConfiguration ? configuration.getLoadedConfiguration() : configuration.getOriginalConfiguration());
		} else {
			for (Configuration configuration : getIbisManager().getConfigurations()) {
				result.append(loadedConfiguration ? configuration.getLoadedConfiguration() : configuration.getOriginalConfiguration());
			}
		}

		return ResponseMessage.ok(result.toString());
	}

	private Configuration getConfigurationByName(String configurationName) {
		Configuration configuration = getIbisManager().getConfiguration(configurationName);
		if(configuration == null) {
			throw new IllegalStateException("configuration ["+configurationName+"] does not exists");
		}
		return configuration;
	}

	/**
	 * @return If the configuration is of type DatabaseClassLoader, the metadata of the configurations found in the database.
	 * header configuration The name of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.FIND)
	public Message<String> getConfigurationDetailsByName(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_NAME_KEY);
		Configuration configuration = getConfigurationByName(configurationName);

		if("DatabaseClassLoader".equals(configuration.getClassLoaderType())) {
			String datasourceName = BusMessageUtils.getHeader(message, HEADER_DATASOURCE_NAME_KEY);
			List<Map<String, Object>> configs = getConfigsFromDatabase(configurationName, datasourceName);

			for(Map<String, Object> config: configs) {
				config.put("loaded", config.get("version").toString().equals(configuration.getVersion()));
			}

			return ResponseMessage.ok(configs);
		}

		return ResponseMessage.noContent();
	}

	/**
	 * @return Manages a configuration, either activates the config directly or sets the autoreload flag in the database
	 * header configuration The name of the Configuration to manage
	 * header version The version of the Configuration to find
	 * header activate Whether the configuration should be activated
	 * header autoreload Whether the configuration should be reloaded (on the next ReloadJob interval)
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.MANAGE)
	public Message<String> manageConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists

		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		Boolean activate = BusMessageUtils.getBooleanHeader(message, "activate", null);
		Boolean autoreload = BusMessageUtils.getBooleanHeader(message, "autoreload", null);
		String datasourceName = BusMessageUtils.getHeader(message, HEADER_DATASOURCE_NAME_KEY);

		try {
			if(activate != null) {
				if(ConfigurationUtils.activateConfig(getIbisContext(), configurationName, version, activate, datasourceName)) {
					return ResponseMessage.accepted();
				}
			}
			else if(autoreload != null && ConfigurationUtils.autoReloadConfig(getIbisContext(), configurationName, version, autoreload, datasourceName)) {
				return ResponseMessage.accepted();
			}
		} catch(Exception e) {
			log.warn("unable to update configuration settings in database", e);
			throw new IllegalStateException("unable to update configuration settings in database"); //don't pass e, we should limit sensitive information from being sent over the bus
		}

		log.debug("header [activate] or [autoreload] not found");
		return ResponseMessage.badRequest();
	}

	/**
	 * header configuration The name of the Configuration to download
	 * header version The version of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.DOWNLOAD)
	public Message<byte[]> downloadConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists
		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		String datasourceName = BusMessageUtils.getHeader(message, HEADER_DATASOURCE_NAME_KEY);

		Map<String, Object> configuration;
		try {
			configuration = ConfigurationUtils.getConfigFromDatabase(getIbisContext(), configurationName, datasourceName, version);
		} catch (ConfigurationException e) {
			log.warn("unable to download configuration from database", e);
			throw new IllegalStateException("unable to download configuration from database"); //don't pass e, we should limit sensitive information from being sent over the bus
		}
		byte[] config = (byte[]) configuration.get("CONFIG");

		Map<String, Object> headers = new HashMap<>();
		headers.put(ResponseMessage.STATUS_KEY, 200);
		headers.put(ResponseMessage.MIMETYPE_KEY, MediaType.APPLICATION_OCTET_STREAM.toString());
		headers.put(ResponseMessage.CONTENT_DISPOSITION_KEY, "attachment; filename=\"" + configuration.get("FILENAME") + "\"");
		return new GenericMessage<>(config, headers);
	}

	/**
	 * header configuration The name of the Configuration to delete
	 * header version The version of the Configuration to find
	 * header datasourceName The name of the datasource where the configurations are located.
	 */
	@ActionSelector(BusAction.DELETE)
	public void deleteConfiguration(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_NAME_KEY);
		getConfigurationByName(configurationName); //Validate the configuration exists
		String version = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_VERSION_KEY);
		String datasourceName = BusMessageUtils.getHeader(message, HEADER_DATASOURCE_NAME_KEY);

		try {
			ConfigurationUtils.removeConfigFromDatabase(getIbisContext(), configurationName, datasourceName, version);
		} catch (Exception e) {
			log.warn("unable to delete configuration from database", e);
			throw new IllegalStateException("unable to delete configuration from database");
		}
	}

	/**
	 * @return The status of a configuration. If an Adapter is not in state STARTED it is flagged as NOT-OK.
	 * header configuration The name of the Configuration to delete
	 */
	@ActionSelector(BusAction.STATUS)
	public Message<String> getConfigurationHealth(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, HEADER_CONFIGURATION_NAME_KEY);
		Configuration configuration = getConfigurationByName(configurationName);
		if(!configuration.isActive()) {
			throw new IllegalStateException("configuration not active", configuration.getConfigurationException());
		}

		Map<String, Object> response = new HashMap<>();
		Map<RunState, Integer> stateCount = new EnumMap<>(RunState.class);
		List<String> errors = new ArrayList<>();

		for (IAdapter adapter : configuration.getRegisteredAdapters()) {
			RunState state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

			if(state==RunState.STARTED) {
				for (Receiver<?> receiver: adapter.getReceivers()) {
					RunState rState = receiver.getRunState();

					if(rState!=RunState.STARTED) {
						errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
						state = RunState.ERROR;
					}
				}
			}
			else {
				errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
				state = RunState.ERROR;
			}

			int count;
			if(stateCount.containsKey(state))
				count = stateCount.get(state);
			else
				count = 0;

			stateCount.put(state, ++count);
		}

		Status status = Status.OK;
		if(stateCount.containsKey(RunState.ERROR))
			status = Status.SERVICE_UNAVAILABLE;

		if(!errors.isEmpty())
			response.put("errors", errors);
		response.put("status", status);

		return ResponseMessage.ok(response);
	}

	private List<Map<String, Object>> getConfigsFromDatabase(String configurationName, String dataSourceName) {
		List<Map<String, Object>> returnMap = new ArrayList<>();

		if (StringUtils.isEmpty(dataSourceName)) {
			dataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
			if (StringUtils.isEmpty(dataSourceName)) {
				throw new IllegalStateException("no datasource specified and default datasource not found");
			}
		}

		FixedQuerySender qs = getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
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
							Map<String, Object> config = new HashMap<>();
							config.put("name", rs.getString(1));
							config.put("version", rs.getString(2));
							config.put("filename", rs.getString(3));
							config.put("user", rs.getString(4));
							config.put("active", rs.getBoolean(5));
							config.put("autoreload", rs.getBoolean(6));

							Date creationDate = rs.getDate(7);
							config.put("created", DateUtils.format(creationDate, DateUtils.FORMAT_GENERICDATETIME));
							returnMap.add(config);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new ApiException(e);
		} finally {
			qs.close();
		}

		returnMap.sort(new NameComparatorBase<Map<String, Object>>() {

			@Override
			public int compare(Map<String, Object> obj1, Map<String, Object> obj2) {
				String filename1 = (String) obj1.get(orderBy);
				String filename2 = (String) obj2.get(orderBy);

				return -compareNames(filename1, filename2); //invert the results as we want the latest version first
			}

		});
		return returnMap;
	}
}
