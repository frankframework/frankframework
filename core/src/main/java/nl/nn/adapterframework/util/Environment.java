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

		if (props.size() == 0) {
			BufferedReader br;
			Process p;
			Runtime r = Runtime.getRuntime();
			String OS = System.getProperty("os.name").toLowerCase();
			if (OS.contains("windows 9")) {
				p = r.exec("command.com /c set");
			} else if (
					(OS.contains("nt"))
							|| (OS.contains("windows 20"))
							|| (OS.contains("windows xp"))) {
				p = r.exec("cmd.exe /c set");
			} else {
				//assume Unix
				p = r.exec("env");
			}
			br = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				int idx = line.indexOf('=');
				if (idx>=0) {
					String key = line.substring(0, idx);
					String value = line.substring(idx + 1);
					props.setProperty(key,value);
				}
			}
		}

		return props;
	}
}
