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

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.MessageDispatcher;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Dir2Map;

/**
 * Configured in the SpringEnvironmentContext.xml not through the {@link MessageDispatcher}.
 */
public class Logging {

	private String defaultLogDirectory = AppConstants.getInstance().getResolvedProperty("logging.path").replace("\\\\", "\\");
	private String defaultLogWildcard = AppConstants.getInstance().getProperty("logging.wildcard");
	private boolean showDirectories = AppConstants.getInstance().getBoolean("logging.showdirectories", false);
	private int maxItems = AppConstants.getInstance().getInt("logging.items.max", 500);

	public Message<String> getLogDirectory(Message<?> message) {
		String directory = BusMessageUtils.getHeader(message, "directory", defaultLogDirectory);
		boolean sizeFormat = BusMessageUtils.getBooleanHeader(message, "sizeFormat", true);
		String wildcard = BusMessageUtils.getHeader(message, "wildcard", defaultLogWildcard);

		Map<String, Object> returnMap = new HashMap<>();
		Dir2Map dir = new Dir2Map(directory, sizeFormat, wildcard, showDirectories, maxItems);

		returnMap.put("list", dir.getList());
		returnMap.put("count", dir.size());
		returnMap.put("directory", dir.getDirectory());
		returnMap.put("sizeFormat", sizeFormat);
		returnMap.put("wildcard", wildcard);

		return ResponseMessage.ok(returnMap);
	}
}
