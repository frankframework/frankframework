/*
   Copyright 2013 Nationale-Nederlanden, 2024 WeAreFrank!

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
package org.frankframework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.Map;

/**
 * Utility class to report process parameters like memory usage as an xml-element.
 *
 * @since 4.2
 */
public class ProcessMetrics {

	private static final long K_LIMIT=10*1024;
	private static final long M_LIMIT=K_LIMIT*1024;
	private static final long G_LIMIT=M_LIMIT*1024;
	private static final long T_LIMIT=G_LIMIT*1024;
	private static final Logger log = LoggerFactory.getLogger(ProcessMetrics.class);

	public static String normalizedNotation(long value) {
		String valueString;

		if (value < K_LIMIT) {
			valueString = Long.toString(value);
		} else {
			if (value < M_LIMIT) {
				valueString = Long.toString(value/1024)+"K";
			} else {
				if (value < G_LIMIT) {
					valueString = Long.toString(value/(1024*1024))+"M";
				} else {
					if (value < T_LIMIT) {
						valueString = Long.toString(value/(1024*1024*1024))+"G";
					} else {
						valueString = Long.toString(value/(1024*1024*1024*1024))+"T";
					}
				}
			}
		}
		return valueString;
	}

	public static void addNumberProperty(XmlBuilder list, String name, long value) {
		addProperty(list,name,Misc.toFileSize(value));
	}

	public static void addProperty(XmlBuilder list, String name, String value) {
		XmlBuilder p=new XmlBuilder("property");
		p.addAttribute("name", name);
		p.setValue(value);
		list.addSubElement(p);
	}

	public static String toXml() {
		XmlBuilder xmlh=new XmlBuilder("processMetrics");
		XmlBuilder props=new XmlBuilder("properties");
		xmlh.addSubElement(props);

		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		long maxMemory = Runtime.getRuntime().maxMemory();

		addNumberProperty(props, "freeMemory", freeMem);
		addNumberProperty(props, "totalMemory", totalMem);
		addNumberProperty(props, "heapSize", totalMem-freeMem);
		addNumberProperty(props, "maxMemory", maxMemory);
		addProperty(props, "currentTime", DateFormatUtils.now());
		return xmlh.toXML();
	}

	public static Map<String, String> toMap() {
		Map<String, String> memoryStatistics = new Hashtable<>(3);

		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		long maxMemory = Runtime.getRuntime().maxMemory();

		memoryStatistics.put("freeMemory", String.valueOf(freeMem));
		memoryStatistics.put("totalMemory", String.valueOf(totalMem));
		memoryStatistics.put("heapSize", String.valueOf(totalMem-freeMem));
		memoryStatistics.put("maxMemory", String.valueOf(maxMemory));
		return memoryStatistics;
	}
}
