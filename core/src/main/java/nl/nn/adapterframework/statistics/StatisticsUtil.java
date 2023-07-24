/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package nl.nn.adapterframework.statistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Some utilities for working with statistics files.
 *
 * @author  Peter Leeuwenburgh
 */
public class StatisticsUtil {

	public static String fileToString(String fileName, String timestamp, String adapterName) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		String line;
		boolean timestampActive = false;
		boolean adapterNameActive = false;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0) {
				if (line.startsWith("<statisticsCollection")) {
					sb.append(line);
					if (timestamp != null
						&& line.indexOf("timestamp=\"" + timestamp + "\"") > 0) {
						timestampActive = true;
					} else {
						timestampActive = false;
					}
				} else {
					if (line.startsWith("</statisticsCollection")) {
						sb.append(line);
					} else {
						if (line.startsWith("<statgroup")) {
							sb.append(line);
							if (adapterName != null
								&& line.indexOf("type=\"adapter\"") > 0) {
								if (line
									.indexOf("name=\"" + adapterName + "\"")
									> 0) {
									adapterNameActive = true;
								} else {
									adapterNameActive = false;
								}
							}
						} else {
							if (line.startsWith("</statgroup")) {
								sb.append(line);
							} else {
								if (timestampActive && adapterNameActive) {
									sb.append(line);
								}
							}
						}
					}
				}
			}
		}
		in.close();

		return sb.toString();
	}
}
