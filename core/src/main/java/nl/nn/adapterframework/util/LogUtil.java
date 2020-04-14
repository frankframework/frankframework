/*
   Copyright 2013, 2019-2020 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.extensions.log4j.IbisLoggerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

public class LogUtil {
	public static final String[] decprecated = new String[] {"log4j4ibis.xml"};

	static {
		// Make sure logger configuration is initialised each time servlet starts.
		IbisLoggerConfiguration.init();

		String message = "You seem to be using our old logger configuration file [%s]. " +
				"We have upgraded our logger system, and now using log4j2.xml instead. " +
				"Check out this url for more information: https://logging.apache.org/log4j/2.x/manual/configuration.html";
		for (String f : decprecated) {
			URL url = LogUtil.class.getClassLoader().getResource(f);
			if (url != null)
				ConfigurationWarnings.getInstance().add(String.format(message, f));
		}
	}

	public static Logger getRootLogger() {
		return LogManager.getRootLogger();
	}

	public static Logger getLogger(String name) {
		return LogManager.getLogger(name);
	}

	public static Logger getLogger(Class<?> clazz) {
		return LogManager.getLogger(clazz);
	}

	public static Logger getLogger(Object owner) {
		return LogManager.getLogger(owner);
	}
}
