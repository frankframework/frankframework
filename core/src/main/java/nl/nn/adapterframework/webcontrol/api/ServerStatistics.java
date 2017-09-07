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
import org.apache.log4j.lf5.LogLevel;

import nl.nn.adapterframework.configuration.BaseConfigurationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.extensions.log4j.IbisAppenderWrapper;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
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

	@GET
	@PermitAll
	@Path("/server/info")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerInformation() throws ApiException {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		List<Object> configurations = new ArrayList<Object>();

		initBase(servletConfig);

		for (Configuration configuration : ibisManager.getConfigurations()) {
			Map<String, Object> cfg = new HashMap<String, Object>();
			cfg.put("name", configuration.getName());
			cfg.put("version", configuration.getVersion());
			cfg.put("type", configuration.getClassLoaderType());
			ClassLoader classLoader = configuration.getClassLoader().getParent();
			if(classLoader instanceof DatabaseClassLoader) {
				cfg.put("filename", ((DatabaseClassLoader) classLoader).getFileName());
				cfg.put("created", ((DatabaseClassLoader) classLoader).getCreationDate());
				cfg.put("user", ((DatabaseClassLoader) classLoader).getUser());
			}
			configurations.add(cfg);
		}
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
		ConfigurationWarnings globalConfigWarnings = ConfigurationWarnings.getInstance();

		List<Object> warnings = new ArrayList<Object>(); //(globalConfigWarnings.size() + 1); //Add 1 for ESR
		boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);

		List<IAdapter> registeredAdapters = ibisManager.getRegisteredAdapters();

		long esr = 0;
		if (showCountErrorStore) {
			for(Iterator<IAdapter> adapterIt=registeredAdapters.iterator(); adapterIt.hasNext();) {
				Adapter adapter = (Adapter)adapterIt.next();
				for(Iterator<?> receiverIt=adapter.getReceiverIterator(); receiverIt.hasNext();) {
					ReceiverBase receiver=(ReceiverBase)receiverIt.next();
					ITransactionalStorage errorStorage=receiver.getErrorStorage();
					if (errorStorage!=null) {
						try {
							esr += errorStorage.getMessageCount();
						} catch (Exception e) {
							//error("error occured on getting number of errorlog records for adapter ["+adapter.getName()+"]",e);
							log.warn("Assuming there are no errorlog records for adapter ["+adapter.getName()+"]");
						}
					}
				}
			}
		} else {
			esr = -1;
		}

		if (esr!=0) {
			Map<String, Object> messageObj = new HashMap<String, Object>(2);
			String message;
			if (esr==-1) {
				message = "Errorlog might contain records. This is unknown because errorStore.count.show is not set to true";
			} else if (esr==1) {
				message = "Errorlog contains 1 record. Service management should check whether this record has to be resent or deleted";
			} else {
				message = "Errorlog contains "+esr+" records. Service Management should check whether these records have to be resent or deleted";
			}
			messageObj.put("message", message);
			messageObj.put("type", "severe");
			warnings.add(messageObj);
		}

		for (Configuration config : ibisManager.getConfigurations()) {
			if (config.getConfigurationException()!=null) {
				Map<String, Object> messageObj = new HashMap<String, Object>(2);
				String message = config.getConfigurationException().getMessage();
				messageObj.put("message", message);
				messageObj.put("type", "exception");
				warnings.add(messageObj);
			}
		}

		//Configuration specific warnings
		for (Configuration configuration : ibisManager.getConfigurations()) {
			BaseConfigurationWarnings configWarns = configuration.getConfigurationWarnings();
			for (int j = 0; j < configWarns.size(); j++) {
				Map<String, Object> messageObj = new HashMap<String, Object>(1);
				messageObj.put("message", configWarns.get(j));
				messageObj.put("configuration", configuration.getName());
				warnings.add(messageObj);
			}
		}

		//Global warnings
		if (globalConfigWarnings.size()>0) {
			for (int j=0; j<globalConfigWarnings.size(); j++) {
				Map<String, Object> messageObj = new HashMap<String, Object>(1);
				messageObj.put("message", globalConfigWarnings.get(j));
				warnings.add(messageObj);
			}
		}

		return Response.status(Response.Status.CREATED).entity(warnings).build();
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
