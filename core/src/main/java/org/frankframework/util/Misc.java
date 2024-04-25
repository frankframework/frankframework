/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.IMessageBrowser.HideMethod;
import org.xml.sax.InputSource;


/**
 * Miscellaneous conversion functions.
 */
//Be careful: UTIL classes should NOT depend on the Servlet-API
public class Misc {
	private static final Logger log = LogUtil.getLogger(Misc.class);
	public static final String MESSAGE_SIZE_WARN_BY_DEFAULT_KEY = "message.size.warn.default";

	private static Long messageSizeWarnByDefault = null;
	public static final String LINE_SEPARATOR = System.lineSeparator();

	private static final String[] BYTE_UNITS_SI = new String[]{"kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
	private static final String[] BYTE_UNITS_IEC = new String[]{"KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"};

	public static String insertAuthorityInUrlString(String url, String authAlias, String username, String password) {
		if (StringUtils.isNotEmpty(authAlias) || StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(password)) {
			CredentialFactory cf = new CredentialFactory(authAlias, username, password);
			int posPrefixEnd;
			String prefix;
			String tail;
			if ((posPrefixEnd=url.indexOf("://"))>0) {
				prefix=url.substring(0, posPrefixEnd+3);
				tail=url.substring(posPrefixEnd+3);
			} else {
				prefix="";
				tail=url;
			}
			int posTail2Start;
			if ((posTail2Start=tail.indexOf("@"))>0) {
				tail=tail.substring(posTail2Start+1);
			}
			url=prefix+cf.getUsername()+":"+cf.getPassword()+"@"+tail;
		}
		return url;
	}

	/**
	 * Converts a byte array into a string, and adds a specified string to the end of the converted string.
	 * @see StreamUtil#streamToString(InputStream, String, boolean)
	 */
	public static String byteArrayToString(byte[] input, String endOfLineString, boolean xmlEncode) throws IOException{
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		return StreamUtil.streamToString(bis, endOfLineString, xmlEncode);
	}

	public static String getHostname() {
		String localHost;
		try {
			InetAddress localMachine = InetAddress.getLocalHost();
			localHost = localMachine.getHostName();
		} catch(UnknownHostException uhe) {
			localHost="unknown ("+uhe.getMessage()+")";
		}
		return localHost;
	}

	/**
	 * Converts the file size to bytes.
	 * <pre>Misc.toFileSize("14GB", 20); // gives out 15032385536</pre>
	 */
	public static long toFileSize(String value, long defaultValue) {
		if(value == null)
			return defaultValue;

		String s = value.trim().toUpperCase();
		long multiplier = 1;
		int index;

		if((index = s.indexOf("KB")) != -1) {
			multiplier = 1024;
			s = s.substring(0, index);
		}
		else if((index = s.indexOf("MB")) != -1) {
			multiplier = 1024L*1024;
			s = s.substring(0, index);
		}
		else if((index = s.indexOf("GB")) != -1) {
			multiplier = 1024L*1024*1024;
			s = s.substring(0, index);
		}
		try {
			return Long.parseLong(s) * multiplier;
		} catch (NumberFormatException e) {
			log.error("[" + value + "] not in expected format", e);
		}
		return defaultValue;
	}

	/**
	 * @see #toFileSize(long, boolean)
	 */
	public static String toFileSize(long bytes) {
		return toFileSize(bytes, false);
	}

	/**
	 * @see #toFileSize(long, boolean, boolean)
	 */
	public static String toFileSize(long bytes, boolean format) {
		return toFileSize(bytes, format, false);
	}

	/**
	 * Format bytes as human-readable text.
	 *
	 * <pre>
	 *     String kbIecUnits = Misc.toFileSize(150000, false, false); 			// gives out "146KiB"
	 * 	   String mbIecUnits = Misc.toFileSize(15000000, true); 				// gives out "14 MiB"
	 *     String gbIecUnits = Misc.toFileSize(Long.parseLong("3221225472"));	// gives out "3GiB"
	 *
	 * 	   String kbSiUnits = Misc.toFileSize(150000, true, false); 						// gives out "150kB"
	 * 	   String mbSiUnits = Misc.toFileSize(15000000, true, true); 						// gives out "15 MB"
	 * 	   String gbSiUnits = Misc.toFileSize(Long.parseLong("3221225472"), true, false); 	// gives out "3GB"
	 * </pre>
	 *
	 * @param bytes Number of bytes. Should be positive, value is returned as string if negative.
	 * @param format True to insert space between value and unit.
	 * @param useSiUnits True to use metric (SI) units, aka powers of 1000. False to use
	 *           binary (IEC), aka powers of 1024.
	 *
	 * @return Formatted string.
	 */
	public static String toFileSize(long bytes, boolean format, boolean useSiUnits) {
		if (bytes < 0) {
			return String.valueOf(bytes);
		}

		int threshold = useSiUnits ? 1000 : 1024;

		if (Math.abs(bytes) < threshold) {
			return bytes + (format ? " " : "") + "B";
		}

		String[] units = useSiUnits ? BYTE_UNITS_SI : BYTE_UNITS_IEC;
		int index = -1;
		double roundingPrecision = 10;

		long dividedBytes = bytes;

		do {
			dividedBytes /= threshold;
			++index;
		} while (
			Math.round(Math.abs(dividedBytes) * roundingPrecision) / roundingPrecision >=
				threshold &&
				index < units.length - 1
		);

		return dividedBytes + (format ? " " : "") + units[index];
	}

	public static synchronized long getMessageSizeWarnByDefault() {
		if (messageSizeWarnByDefault == null) {
			String definitionString = AppConstants.getInstance().getString(MESSAGE_SIZE_WARN_BY_DEFAULT_KEY, null);
			messageSizeWarnByDefault = toFileSize(definitionString, -1);
		}
		return messageSizeWarnByDefault;
	}

	/**
	 * Converts the list to a string.
	 * {@code
	 * <pre>
	 *      List<String> list = new ArrayList<>();
	 *      list.add("We Are");
	 *      list.add(" Frank");
	 *      String res = Misc.listToString(list); // res = "We Are Frank"
	 * </pre>
	 * }
	 */
	public static String listToString(List<String> list) {
		return String.join("", list);
	}

	/**
	 * Adds items on a string, added by comma separator (ex: "1,2,3"), into a list.
	 * @param collectionDescription description of the list
	 */
	public static void addItemsToList(Collection<String> collection, String list, String collectionDescription, boolean lowercase) {
		if (list==null) {
			return;
		}
		StringTokenizer st = new StringTokenizer(list, ",");
		while (st.hasMoreTokens()) {
			String item = st.nextToken().trim();
			if (lowercase) {
				item=item.toLowerCase();
			}
			log.debug("adding item to "+collectionDescription+" ["+item+"]");
			collection.add(item);
		}
		if (list.trim().endsWith(",")) {
			log.debug("adding item to "+collectionDescription+" <empty string>");
			collection.add("");
		}
	}

	public static String getAge(long value) {
		long currentTime = (new Date()).getTime();
		long age = currentTime - value;
		String ageString = DurationFormatUtils.formatDuration(age, "d") + "d";
		if ("0d".equals(ageString)) {
			ageString = DurationFormatUtils.formatDuration(age, "H") + "h";
			if ("0h".equals(ageString)) {
				ageString = DurationFormatUtils.formatDuration(age, "m") + "m";
				if ("0m".equals(ageString)) {
					ageString = DurationFormatUtils.formatDuration(age, "s") + "s";
					if ("0s".equals(ageString)) {
						ageString = age + "ms";
					}
				}
			}
		}
		return ageString;
	}

	public static String getDurationInMs(long value) {
		long currentTime = (new Date()).getTime();
		long duration = currentTime - value;
		return duration + "ms";
	}

	/**
	 * @return 'age' in milliseconds.
	 */
	public static long parseAge(String value, long defaultValue) {
		if (value == null)
			return defaultValue;

		String s = value.trim().toUpperCase();
		long multiplier = 1;
		int index;

		if ((index = s.indexOf('S')) != -1) {
			multiplier = 1000L;
			s = s.substring(0, index);
		} else if ((index = s.indexOf('M')) != -1) {
			multiplier = 60L * 1000L;
			s = s.substring(0, index);
		} else if ((index = s.indexOf('H')) != -1) {
			multiplier = 60L * 60L * 1000L;
			s = s.substring(0, index);
		} else if ((index = s.indexOf('D')) != -1) {
			multiplier = 24L * 60L * 60L * 1000L;
			s = s.substring(0, index);
		}
		try {
			return Long.parseLong(s) * multiplier;
		} catch (NumberFormatException e) {
			log.error("[" + value + "] not in expected format", e);
		}
		return defaultValue;
	}

	/**
	 * Edits the input string according to the regex and the hide method specified.
	 * @see StringUtil#hideFirstHalf(String, String)
	 * @see StringUtil#hideAll(String, String)
	 */
	public static String cleanseMessage(String inputString, String regexForHiding, HideMethod hideMethod) {
		if (StringUtils.isEmpty(regexForHiding)) {
			return inputString;
		}
		if (hideMethod == HideMethod.FIRSTHALF) {
			return StringUtil.hideFirstHalf(inputString, regexForHiding);
		}
		return StringUtil.hideAll(inputString, regexForHiding);
	}

	public static String getBuildOutputDirectory() {
		// TODO: Warning from Sonarlint of Potential NPE?
		String path = new File(AppConstants.class.getClassLoader().getResource("").getPath()).getPath();

		try {
			return URLDecoder.decode(path, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		} catch (UnsupportedEncodingException e) {
			log.warn("unable to parse build-output-directory using charset [{}]", StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, e);
			return null;
		}
	}

	public static <T> void addToSortedListUnique(List<T> list, T item) {
		int index = Collections.binarySearch(list, item, null);
		if (index < 0) {
			list.add(Misc.binarySearchResultToInsertionPoint(index), item);
		}
	}

	public static <T> void addToSortedListNonUnique(List<T> list, T item) {
		int index = Misc.binarySearchResultToInsertionPoint(Collections.binarySearch(list, item, null));
		list.add(index, item);
	}

	private static int binarySearchResultToInsertionPoint(int index) {
		// See https://stackoverflow.com/questions/16764007/insert-into-an-already-sorted-list/16764413
		// for more information.
		if (index < 0) {
			index = -index - 1;
		}
		return index;
	}

	public static String jsonPretty(String json) {
		StringWriter sw = new StringWriter();
		try(JsonReader jr = Json.createReader(new StringReader(json))) {
			JsonStructure jobj = jr.read();

			Map<String, Object> properties = new HashMap<>(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);

			JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
			try (JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
				jsonWriter.write(jobj);
			}
		}
		return sw.toString().trim();
	}

	public static InputSource asInputSource(URL url) throws IOException {
		InputSource inputSource = new InputSource(url.openStream());
		inputSource.setSystemId(url.toExternalForm());
		return inputSource;
	}
}
