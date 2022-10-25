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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.WebApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.lifecycle.ApplicationMetrics;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessMetrics;

@BusAware("frank-management-bus")
public class ServerStatistics implements ApplicationContextAware {
	private @Getter @Setter IbisManager ibisManager;
	private @Getter @Setter ApplicationContext applicationContext;

	@TopicSelector(BusTopic.STATUS)
	@ActionSelector(BusAction.GET)
	public Message<String> getServerInformation(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();

		AppConstants appConstants = AppConstants.getInstance();
		Map<String, Object> framework = new HashMap<>(2);
		framework.put("name", "FF!");
		framework.put("version", appConstants.getProperty("application.version"));
		returnMap.put("framework", framework);

		Map<String, Object> instance = new HashMap<>(2);
		instance.put("version", appConstants.getProperty("instance.version"));
		instance.put("name", getIbisManager().getIbisContext().getApplicationName());
		returnMap.put("instance", instance);

		String dtapStage = appConstants.getProperty("dtap.stage");
		returnMap.put("dtap.stage", dtapStage);
		String dtapSide = appConstants.getProperty("dtap.side");
		returnMap.put("dtap.side", dtapSide);

		UserDetails user = BusMessageUtils.getUserDetails();
		if(user != null) {
			returnMap.put("userName", user.getUsername());
		}

		returnMap.put("applicationServer", getApplicationServer());
		returnMap.put("javaVersion", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ")");
		Map<String, Object> fileSystem = new HashMap<>(2);
		fileSystem.put("totalSpace", Misc.getFileSystemTotalSpace());
		fileSystem.put("freeSpace", Misc.getFileSystemFreeSpace());
		returnMap.put("fileSystem", fileSystem);
		returnMap.put("processMetrics", ProcessMetrics.toMap());
		Date date = new Date();
		returnMap.put("serverTime", date.getTime());
		returnMap.put("machineName" , Misc.getHostname());
		ApplicationMetrics metrics = getApplicationContext().getBean("metrics", ApplicationMetrics.class);
		returnMap.put("uptime", (metrics != null) ? metrics.getUptimeDate() : "");

		return ResponseMessage.ok(returnMap);
	}

	private String getApplicationServer() {
		return getApplicationServer(applicationContext);
	}

	private String getApplicationServer(ApplicationContext ac) {
		if(ac instanceof WebApplicationContext) {
			ServletContext sc = ((WebApplicationContext) ac).getServletContext();
			if(sc != null) {
				return sc.getServerInfo();
			}
		}
		if(ac.getParent() != null) {
			return getApplicationServer(ac.getParent());
		}
		return "unknown";
	}
}
