/*
   Copyright 2023 WeAreFrank!

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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Environment {
	private static final Logger log = LogManager.getLogger(Environment.class);

	public static Properties getEnvironmentVariables() throws IOException {
		Properties props = new Properties();

		try {
			System.getenv().forEach(props::setProperty);
		} catch (Exception e) {
			log.debug("Exception getting environment variables", e);
		}

		if (!props.isEmpty()) {
			return props;
		}
		return readEnvironmentFromOsCommand();

	}

	private static Properties readEnvironmentFromOsCommand() throws IOException {
		Properties props = new Properties();
		String command = determineOsSpecificEnvCommand();
		log.debug("Reading environment variables from OS using command [{}]", command);
		Runtime r = Runtime.getRuntime();
		Process p = r.exec(command);

		BufferedReader br = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			int idx = line.indexOf('=');
			if (idx>=0) {
				String key = line.substring(0, idx);
				String value = line.substring(idx + 1);
				props.setProperty(key,value);
			}
		}
		return props;
	}

	private static String determineOsSpecificEnvCommand() {
		String OS = System.getProperty("os.name").toLowerCase();
		String envCommand;
		if (OS.contains("windows 9")) {
			envCommand = "command.com /c set";
		} else if (
			(OS.contains("nt"))
				|| (OS.contains("windows 20"))
				|| (OS.contains("windows xp"))) {
			envCommand = "cmd.exe /c set";
		} else {
			//assume Unix
			envCommand = "env";
		}
		return envCommand;
	}
}
