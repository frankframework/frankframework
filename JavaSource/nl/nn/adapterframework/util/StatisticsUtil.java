/*
 * $Log: StatisticsUtil.java,v $
 * Revision 1.1  2009-04-03 14:33:21  m168309
 * Initial version
 *
 */
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Some utilities for working with statistics files. 
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class StatisticsUtil {

	public static String fileToString(String fileName, String timestamp, String adapterName) throws IOException {
		StringBuffer sb = new StringBuffer();
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
