/*
Copyright 2016-2021 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.lifecycle.ApplicationMetrics;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle.BootState;
import nl.nn.adapterframework.lifecycle.MessageEventListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessMetrics;
import nl.nn.adapterframework.util.RunState;

/**
 * Collection of server and application statistics and information.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ServerStatistics extends Base {

	@Context private Request rsRequest;
	private static final int MAX_MESSAGE_SIZE = AppConstants.getInstance().getInt("adapter.message.max.size", 0);

	@GET
	@PermitAll
	@Path("/server/info")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerInformation() throws ApiException {
		Map<String, Object> returnMap = new HashMap<>();

		AppConstants appConstants = AppConstants.getInstance();
		Map<String, Object> framework = new HashMap<>(2);
		framework.put("name", "FF!");
		framework.put("version", appConstants.getProperty("application.version"));
		returnMap.put("framework", framework);

		Map<String, Object> instance = new HashMap<>(2);
		instance.put("version", appConstants.getProperty("instance.version"));
		instance.put("name", getIbisContext().getApplicationName());
		returnMap.put("instance", instance);

		String dtapStage = appConstants.getProperty("dtap.stage");
		returnMap.put("dtap.stage", dtapStage);
		String dtapSide = appConstants.getProperty("dtap.side");
		returnMap.put("dtap.side", dtapSide);

		returnMap.put("configurations", getConfigurations());

		String user = getUserPrincipalName();
		if(user != null) {
			returnMap.put("userName", user);
		}

		returnMap.put("applicationServer", servletConfig.getServletContext().getServerInfo());
		returnMap.put("javaVersion", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ")");
		Map<String, Object> fileSystem = new HashMap<>(2);
		fileSystem.put("totalSpace", Misc.getFileSystemTotalSpace());
		fileSystem.put("freeSpace", Misc.getFileSystemFreeSpace());
		returnMap.put("fileSystem", fileSystem);
		returnMap.put("processMetrics", ProcessMetrics.toMap());
		Date date = new Date();
		returnMap.put("serverTime", date.getTime());
		returnMap.put("machineName" , Misc.getHostname());
		ApplicationMetrics metrics = getIbisContext().getBean("metrics", ApplicationMetrics.class);
		returnMap.put("uptime", (metrics != null) ? metrics.getUptimeDate() : "");

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	@GET
	@PermitAll
	@Path("/server/configurations")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllConfigurations() throws ApiException {
		return Response.status(Response.Status.OK).entity(getConfigurations()).build();
	}

	private List<Map<String, Object>> getConfigurations() {
		List<Map<String, Object>> configurations = new ArrayList<>();

		for (Configuration configuration : getIbisManager().getConfigurations()) {
			Map<String, Object> cfg = new HashMap<>();
			cfg.put("name", configuration.getName());
			cfg.put("version", configuration.getVersion());
			cfg.put("stubbed", configuration.isStubbed());
			cfg.put("state", configuration.getState());

			cfg.put("type", configuration.getClassLoaderType());
			if(configuration.getConfigurationException() != null) {
				cfg.put("exception", configuration.getConfigurationException().getMessage());
			}

			ClassLoader classLoader = configuration.getClassLoader();
			if(classLoader instanceof DatabaseClassLoader) {
				cfg.put("filename", ((DatabaseClassLoader) classLoader).getFileName());
				cfg.put("created", ((DatabaseClassLoader) classLoader).getCreationDate());
				cfg.put("user", ((DatabaseClassLoader) classLoader).getUser());
			}

			String parentConfig = AppConstants.getInstance().getString("configurations." + configuration.getName() + ".parentConfig", null);
			if(parentConfig != null)
				cfg.put("parent", parentConfig);

				configurations.add(cfg);
		}

		configurations.sort(new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
				String name1 = (String) lhs.get("name");
				String name2 = (String) rhs.get("name");
				if(name1 == null || name2 == null) return 0;

				return name1.startsWith("IAF_") ? -1 : name2.startsWith("IAF_") ? 1 : name1.compareTo(name2);
			}
		});

		return configurations;
	}


	@GET
	@PermitAll
	@Path("/server/warnings")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerConfiguration() throws ApiException {

		Map<String, Object> returnMap = new HashMap<String, Object>();
		ApplicationWarnings globalConfigWarnings = getIbisContext().getBean("applicationWarnings", ApplicationWarnings.class);
		MessageEventListener eventListener = getIbisContext().getBean("MessageEventListener", MessageEventListener.class);

		long totalErrorStoreCount = 0;
		boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);
		if(!showCountErrorStore)
			totalErrorStoreCount = -1;

		for (Configuration configuration : getIbisManager().getConfigurations()) {
			Map<String, Object> configurationsMap = new HashMap<String, Object>();

			//Configuration specific exceptions
			if (configuration.getConfigurationException()!=null) {
				String message = configuration.getConfigurationException().getMessage();
				configurationsMap.put("exception", message);
			}

			if (configuration.isActive()) {
				//ErrorStore count
				if (showCountErrorStore) {
					long esr = 0;
					for (Adapter adapter : configuration.getRegisteredAdapters()) {
						for (Receiver<?> receiver: adapter.getReceivers()) {
							IMessageBrowser<?> errorStorage = receiver.getMessageBrowser(ProcessState.ERROR);
							if (errorStorage != null) {
								try {
									esr += errorStorage.getMessageCount();
								} catch (Exception e) {
									//error("error occured on getting number of errorlog records for adapter ["+adapter.getName()+"]",e);
									log.warn("Assuming there are no errorlog records for adapter ["+adapter.getName()+"]");
								}
							}
						}
					}
					totalErrorStoreCount += esr;
					configurationsMap.put("errorStoreCount", esr);
				}

				//Configuration specific warnings
				ConfigurationWarnings configWarns = configuration.getConfigurationWarnings();
				if(configWarns != null && configWarns.size() > 0) {
					configurationsMap.put("warnings", configWarns.getWarnings());
				}

				//Configuration specific messages
				MessageKeeper messageKeeper = eventListener.getMessageKeeper(configuration.getName());
				if(messageKeeper != null) {
					List<Object> messages = mapMessageKeeperMessages(messageKeeper);
					if(!messages.isEmpty()) {
						configurationsMap.put("messages", messages);
					}
				}
			}

			returnMap.put(configuration.getName(), configurationsMap);
		}

		//Total ErrorStore Count
		returnMap.put("totalErrorStoreCount", totalErrorStoreCount);

		//Global warnings
		if (globalConfigWarnings.size()>0) {
			List<Object> warnings = new ArrayList<>();
			for (int j=0; j<globalConfigWarnings.size(); j++) {
				warnings.add(globalConfigWarnings.get(j));
			}
			returnMap.put("warnings", warnings);
		}

		//Global messages
		MessageKeeper messageKeeper = eventListener.getMessageKeeper();
		List<Object> messages = mapMessageKeeperMessages(messageKeeper);
		if(!messages.isEmpty()) {
			returnMap.put("messages", messages);
		}

		Response.ResponseBuilder response = null;

		//Calculate the ETag on last modified date of user resource 
		EntityTag etag = new EntityTag(returnMap.hashCode() + "");

		//Verify if it matched with etag available in http request
		response = rsRequest.evaluatePreconditions(etag);

		//If ETag matches the response will be non-null; 
		if (response != null) {
			return response.tag(etag).build();
		}

		response = Response.status(Response.Status.OK).entity(returnMap).tag(etag);
		return response.build();
	}

	private List<Object> mapMessageKeeperMessages(MessageKeeper messageKeeper) {
		List<Object> messages = new ArrayList<Object>();
		for (int t = 0; t < messageKeeper.size(); t++) {
			Map<String, Object> configurationMessage = new HashMap<String, Object>();
			String msg = messageKeeper.getMessage(t).getMessageText();
			if (MAX_MESSAGE_SIZE > 0 && msg.length() > MAX_MESSAGE_SIZE) {
				msg = msg.substring(0, MAX_MESSAGE_SIZE) + "...(" + (msg.length() - MAX_MESSAGE_SIZE)
						+ " characters more)";
			}
			configurationMessage.put("message", msg);
			Date date = messageKeeper.getMessage(t).getMessageDate();
			configurationMessage.put("date", DateUtils.format(date, DateUtils.FORMAT_FULL_GENERIC));
			String level = messageKeeper.getMessage(t).getMessageLevel();
			configurationMessage.put("level", level);
			messages.add(configurationMessage);
		}
		return messages;
	}

	@GET
	@PermitAll
	@Path("/server/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getIbisHealth() {

		Map<String, Object> response = new HashMap<>();

		try {
			getIbisManager();
		}
		catch(ApiException e) {
			response.put("status", Response.Status.INTERNAL_SERVER_ERROR);
			response.put("error", e.getMessage());

			Throwable cause = e.getCause();
			if(cause != null && cause.getStackTrace() != null) {
				String dtapStage = AppConstants.getInstance().getString("dtap.stage", null);
				if((!"ACC".equals(dtapStage) && !"PRD".equals(dtapStage))) {
					response.put("stackTrace", cause.getStackTrace());
				}
			}

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
		catch(Exception e) {
			throw new ApiException(e);
		}

		Map<RunState, Integer> stateCount = new HashMap<>();
		List<String> errors = new ArrayList<>();

		for(Configuration config : getIbisManager().getConfigurations()) {
			BootState state = config.getState();
			if(state != BootState.STARTED) {
				if(config.getConfigurationException() != null) {
					errors.add("configuration["+config.getName()+"] is in state[ERROR]");
				} else {
					errors.add("configuration["+config.getName()+"] is in state["+state+"]");
				}
				stateCount.put(RunState.ERROR, 1); //We're not really using stateCount other then to determine the HTTP response code.
			}
		}

		for (Adapter adapter : getIbisManager().getRegisteredAdapters()) {
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

		Status status = Response.Status.OK;
		if(stateCount.containsKey(RunState.ERROR))
			status = Response.Status.SERVICE_UNAVAILABLE;

		if(!errors.isEmpty()) {
			response.put("errors", errors);
		}
		response.put("status", status);

		return Response.status(status).entity(response).build();
	}
}
