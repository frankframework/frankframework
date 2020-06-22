/*
Copyright 2016-2020 WeAreFrank!

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.context.ApplicationEventPublisher;

import edu.emory.mathcs.backport.java.util.Collections;
import nl.nn.adapterframework.configuration.BaseConfigurationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessMetrics;
import nl.nn.adapterframework.util.RunStateEnum;

/**
 * Collection of server and application statistics and information.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ServerStatistics extends Base {

	@Context Request request;
	private static final int MAX_MESSAGE_SIZE = AppConstants.getInstance().getInt("adapter.message.max.size", 0);

	@GET
	@PermitAll
	@Path("/server/info")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerInformation() throws ApiException {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		List<Object> configurations = new ArrayList<Object>();

		AppConstants appConstants = AppConstants.getInstance();

		for (Configuration configuration : getIbisManager().getConfigurations()) {
			Map<String, Object> cfg = new HashMap<String, Object>();
			cfg.put("name", configuration.getName());
			cfg.put("version", configuration.getVersion());
			cfg.put("stubbed", configuration.isStubbed());

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

		//TODO Replace this with java.util.Collections!
		Collections.sort(configurations, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> lhs, Map<String, String> rhs) {
				String name1 = lhs.get("name");
				String name2 = rhs.get("name");
				return name1.startsWith("IAF_") ? -1 : name2.startsWith("IAF_") ? 1 : name1.compareTo(name2);
			}
		});

		returnMap.put("configurations", configurations);

		Map<String, Object> framework = new HashMap<String, Object>(2);
		framework.put("name", "FF!");
		framework.put("version", appConstants.getProperty("application.version"));
		returnMap.put("framework", framework);

		Map<String, Object> instance = new HashMap<String, Object>(2);
		instance.put("version", appConstants.getProperty("instance.version"));
		instance.put("name", getIbisContext().getApplicationName());
		returnMap.put("instance", instance);

		String dtapStage = appConstants.getProperty("dtap.stage");
		returnMap.put("dtap.stage", dtapStage);
		String dtapSide = appConstants.getProperty("dtap.side");
		returnMap.put("dtap.side", dtapSide);

		returnMap.put("applicationServer", servletConfig.getServletContext().getServerInfo());
		returnMap.put("javaVersion", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ")");
		Map<String, Object> fileSystem = new HashMap<String, Object>(2);
		fileSystem.put("totalSpace", Misc.getFileSystemTotalSpace());
		fileSystem.put("freeSpace", Misc.getFileSystemFreeSpace());
		returnMap.put("fileSystem", fileSystem);
		returnMap.put("processMetrics", ProcessMetrics.toMap());
		Date date = new Date();
		returnMap.put("serverTime", date.getTime());
		returnMap.put("machineName" , Misc.getHostname());
		returnMap.put("uptime", getIbisContext().getUptimeDate());

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	@GET
	@PermitAll
	@Path("/server/warnings")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerConfiguration() throws ApiException {

		Map<String, Object> returnMap = new HashMap<String, Object>();
		ConfigurationWarnings globalConfigWarnings = ConfigurationWarnings.getInstance();

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

			//ErrorStore count
			if (showCountErrorStore) {
				long esr = 0;
				for (IAdapter adapter : configuration.getAdapterService().getAdapters().values()) {
					for(Iterator<?> receiverIt = adapter.getReceiverIterator(); receiverIt.hasNext();) {
						ReceiverBase receiver = (ReceiverBase) receiverIt.next();
						IMessageBrowser errorStorage = receiver.getErrorStorageBrowser();
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
			BaseConfigurationWarnings configWarns = configuration.getConfigurationWarnings();
			List<Object> warnings = new ArrayList<Object>();
			for (int j = 0; j < configWarns.size(); j++) {
				warnings.add(configWarns.get(j));
			}
			if(warnings.size() > 0)
				configurationsMap.put("warnings", warnings);

			//Configuration specific messages
			MessageKeeper messageKeeper = getIbisContext().getMessageKeeper(configuration.getName());
			if(messageKeeper != null) {
				List<Object> messages = mapMessageKeeperMessages(messageKeeper);
				if(messages.size() > 0)
					configurationsMap.put("messages", messages);
			}

			returnMap.put(configuration.getName(), configurationsMap);
		}

		//Total ErrorStore Count
		returnMap.put("totalErrorStoreCount", totalErrorStoreCount);

		//Global warnings
		if (globalConfigWarnings.size()>0) {
			List<Object> warnings = new ArrayList<Object>();
			for (int j=0; j<globalConfigWarnings.size(); j++) {
				warnings.add(globalConfigWarnings.get(j));
			}
			returnMap.put("warnings", warnings);
		}

		//Global messages
		MessageKeeper messageKeeper = getIbisContext().getMessageKeeper();
		List<Object> messages = mapMessageKeeperMessages(messageKeeper);
		if(messages.size() > 0)
			returnMap.put("messages", messages);

		Response.ResponseBuilder response = null;

		//Calculate the ETag on last modified date of user resource 
		EntityTag etag = new EntityTag(returnMap.hashCode() + "");

		//Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

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
	@Path("/server/log")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogConfiguration() throws ApiException {

		Map<String, Object> logSettings = new HashMap<String, Object>(3);
		Logger rootLogger = LogUtil.getRootLogger();

		logSettings.put("maxMessageLength", IbisMaskingLayout.getMaxLength());

		List<String> errorLevels = new ArrayList<String>(Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"));
		logSettings.put("errorLevels", errorLevels);
		logSettings.put("loglevel", rootLogger.getLevel().toString());

		logSettings.put("logIntermediaryResults", AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true));

		logSettings.put("enableDebugger", AppConstants.getInstance().getBoolean("testtool.enabled", true));

		return Response.status(Response.Status.CREATED).entity(logSettings).build();
	}

	@PUT
	@RolesAllowed({"IbisAdmin", "IbisTester"})
	@Path("/server/log")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateLogConfiguration(LinkedHashMap<String, Object> json) throws ApiException {

		Level loglevel = null;
		Boolean logIntermediaryResults = true;
		int maxMessageLength = -1;
		Boolean enableDebugger = null;
		StringBuilder msg = new StringBuilder();

		Logger rootLogger = LogUtil.getRootLogger();

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("loglevel")) {
				loglevel = Level.toLevel(""+value);
			}
			else if(key.equalsIgnoreCase("logIntermediaryResults")) {
				logIntermediaryResults = Boolean.parseBoolean(""+value);
			}
			else if(key.equalsIgnoreCase("maxMessageLength")) {
				maxMessageLength = Integer.parseInt(""+value);
			}
			else if(key.equalsIgnoreCase("enableDebugger")) {
				enableDebugger = Boolean.parseBoolean(""+value);
			}
		}

		if(loglevel != null && rootLogger.getLevel() != loglevel) {
			Configurator.setLevel(rootLogger.getName(), loglevel);
			msg.append("LogLevel changed from [" + rootLogger.getLevel() + "] to [" + loglevel +"]");
		}

		boolean logIntermediary = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true);
		if(logIntermediary != logIntermediaryResults) {
			AppConstants.getInstance().put("log.logIntermediaryResults", "" + logIntermediaryResults);

			if(msg.length() > 0)
				msg.append(", logIntermediaryResults from [" + logIntermediary+ "] to [" + logIntermediaryResults + "]");
			else
				msg.append("logIntermediaryResults changed from [" + logIntermediary+ "] to [" + logIntermediaryResults + "]");
		}

		if (maxMessageLength != IbisMaskingLayout.getMaxLength()) {
			if(msg.length() > 0)
				msg.append(", logMaxMessageLength from [" + IbisMaskingLayout.getMaxLength() + "] to [" + maxMessageLength + "]");
			else
				msg.append("logMaxMessageLength changed from [" + IbisMaskingLayout.getMaxLength() + "] to [" + maxMessageLength + "]");
			IbisMaskingLayout.setMaxLength(maxMessageLength);
		}

		if (enableDebugger!=null) {
			boolean testtoolEnabled=AppConstants.getInstance().getBoolean("testtool.enabled", true);
			if (testtoolEnabled!=enableDebugger) {
				AppConstants.getInstance().put("testtool.enabled", "" + enableDebugger);
				DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, enableDebugger);
				ApplicationEventPublisher applicationEventPublisher = getIbisManager().getApplicationEventPublisher();
				if (applicationEventPublisher!=null) {
					log.info("setting debugger enabled ["+enableDebugger+"]");
					applicationEventPublisher.publishEvent(event);
				} else {
					log.warn("no applicationEventPublisher, cannot set debugger enabled ["+enableDebugger+"]");
				}
			}
 		}
		
		
		if(msg.length() > 0) {
			log.warn(msg.toString());
			LogUtil.getLogger("SEC").info(msg.toString());
		}

		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@PermitAll
	@Path("/server/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getIbisHealth() throws ApiException {

		Map<String, Object> response = new HashMap<String, Object>();

		try {
			getIbisManager();
		}
		catch(ApiException e) {
			Throwable c = e.getCause();
			response.put("status", Response.Status.INTERNAL_SERVER_ERROR);
			response.put("error", c.getMessage());
			response.put("stacktrace", c.getStackTrace());

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}

		List<IAdapter> adapters = getIbisManager().getRegisteredAdapters();
		Map<RunStateEnum, Integer> stateCount = new HashMap<RunStateEnum, Integer>();
		List<String> errors = new ArrayList<String>();

		for (IAdapter adapter : adapters) {
			RunStateEnum state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

			if(state.equals(RunStateEnum.STARTED)) {
				Iterator<IReceiver> receiverIterator = adapter.getReceiverIterator();
				while (receiverIterator.hasNext()) {
					IReceiver receiver = receiverIterator.next();
					RunStateEnum rState = receiver.getRunState();
	
					if(!rState.equals(RunStateEnum.STARTED)) {
						errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
						state = RunStateEnum.ERROR;
					}
				}
			}
			else {
				errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
				state = RunStateEnum.ERROR;
			}

			int count;
			if(stateCount.containsKey(state))
				count = stateCount.get(state);
			else
				count = 0;

			stateCount.put(state, ++count);
		}

		Status status = Response.Status.OK;
		if(stateCount.containsKey(RunStateEnum.ERROR))
			status = Response.Status.SERVICE_UNAVAILABLE;

		if(errors.size() > 0)
			response.put("errors", errors);
		response.put("status", status);

		return Response.status(status).entity(response).build();
	}
}
