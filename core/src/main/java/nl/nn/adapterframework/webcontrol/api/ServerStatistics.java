/*
Copyright 2016-2017 Integration Partners B.V.

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
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.emory.mathcs.backport.java.util.Collections;
import nl.nn.adapterframework.configuration.BaseConfigurationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.extensions.log4j.IbisAppenderWrapper;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessMetrics;

/**
 * Collection of server and application statistics and information.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ServerStatistics extends Base {
	@Context ServletConfig servletConfig;
	private static final int MAX_MESSAGE_SIZE = AppConstants.getInstance().getInt("adapter.message.max.size", 0);

	@GET
	@PermitAll
	@Path("/server/info")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerInformation() throws ApiException {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		List<Object> configurations = new ArrayList<Object>();

		initBase(servletConfig);

		for (Configuration configuration : ibisManager.getConfigurations()) {
			Map<String, String> cfg = new HashMap<String, String>();
			cfg.put("name", configuration.getName());
			cfg.put("version", configuration.getVersion());
			cfg.put("type", configuration.getClassLoaderType());

			if(configuration.getConfigurationException() != null)
				cfg.put("exception", configuration.getConfigurationException().getMessage());

			ClassLoader classLoader = configuration.getClassLoader().getParent();
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

		Collections.sort(configurations, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> lhs, Map<String, String> rhs) {
				String name1 = lhs.get("name");
				String name2 = rhs.get("name");
				return name1.startsWith("IAF_") ? -1 : name2.startsWith("IAF_") ? 1 : name1.compareTo(name2);
			}
		});

		returnMap.put("configurations", configurations);

		returnMap.put("version", ibisContext.getFrameworkVersion());
		returnMap.put("name", ibisContext.getApplicationName());
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
		returnMap.put("uptime", ibisContext.getUptimeDate());

		return Response.status(Response.Status.CREATED).entity(returnMap).build();
	}

	@GET
	@PermitAll
	@Path("/server/warnings")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerConfiguration() throws ApiException {

		initBase(servletConfig);
		Map<String, Object> returnMap = new HashMap<String, Object>();
		ConfigurationWarnings globalConfigWarnings = ConfigurationWarnings.getInstance();

		long totalErrorStoreCount = 0;
		boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);
		if(!showCountErrorStore)
			totalErrorStoreCount = -1;

		for (Configuration configuration : ibisManager.getConfigurations()) {
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
						ITransactionalStorage errorStorage = receiver.getErrorStorage();
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
			MessageKeeper messageKeeper = ibisManager.getIbisContext().getMessageKeeper(configuration.getName());
			List<Object> messages = mapMessageKeeperMessages(messageKeeper);
			if(messages.size() > 0)
				configurationsMap.put("messages", messages);

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
		MessageKeeper messageKeeper = ibisManager.getIbisContext().getMessageKeeper();
		List<Object> messages = mapMessageKeeperMessages(messageKeeper);
		if(messages.size() > 0)
			returnMap.put("messages", messages);

		return Response.status(Response.Status.CREATED).entity(returnMap).build();
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

		Appender appender = rootLogger.getAppender("appwrap");
		IbisAppenderWrapper iaw = null;
		if (appender!=null && appender instanceof IbisAppenderWrapper) {
			iaw = (IbisAppenderWrapper) appender;
			logSettings.put("maxMessageLength", iaw.getMaxMessageLength());
		}
		else {
			logSettings.put("maxMessageLength", -1);
		}

		List<String> errorLevels = new ArrayList<String>(Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"));
		logSettings.put("errorLevels", errorLevels);

		for (Iterator<String> iterator = errorLevels.iterator(); iterator.hasNext();) {
			String level = iterator.next();
			if(rootLogger.getLevel() == Level.toLevel(level))
				logSettings.put("loglevel", level);
		}

		logSettings.put("logIntermediaryResults", AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true));

		return Response.status(Response.Status.CREATED).entity(logSettings).build();
	}

	@PUT
	@RolesAllowed({"IbisAdmin", "IbisTester"})
	@Path("/server/log")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateLogConfiguration(LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);

		Level loglevel = null;
		Boolean logIntermediaryResults = true;
		int maxMessageLength = -1;
		StringBuilder msg = new StringBuilder();

		Logger rootLogger = LogUtil.getRootLogger();

		Appender appender = rootLogger.getAppender("appwrap");
		IbisAppenderWrapper iaw = null;
		if (appender!=null && appender instanceof IbisAppenderWrapper) {
			iaw = (IbisAppenderWrapper) appender;
		}

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
		}

		if(loglevel != null && rootLogger.getLevel() != loglevel) {
			rootLogger.setLevel(loglevel);
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

		if (iaw != null) {
			if(iaw.getMaxMessageLength() != maxMessageLength) {
				if(msg.length() > 0)
					msg.append(", logMaxMessageLength from [" + iaw.getMaxMessageLength() + "] to [" + maxMessageLength + "]");
				else
					msg.append("logMaxMessageLength changed from [" + iaw.getMaxMessageLength() + "] to [" + maxMessageLength + "]");
				iaw.setMaxMessageLength(maxMessageLength);
			}
		}

		if(msg.length() > 0) {
			log.warn(msg.toString());
			LogUtil.getLogger("SEC").info(msg.toString());
		}

		return Response.status(Response.Status.NO_CONTENT).build();
	}
}
