/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import nl.nn.adapterframework.core.IMessageBrowser.HideMethod;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;


/**
 * Miscellaneous conversion functions.
 */
//Be careful: UTIL classes should NOT depend on the Servlet-API
public class Misc {
	private static final Logger log = LogUtil.getLogger(Misc.class);
	public static final String MESSAGE_SIZE_WARN_BY_DEFAULT_KEY = "message.size.warn.default";

	private static Long messageSizeWarnByDefault = null;
	public static final String LINE_SEPARATOR = System.lineSeparator();

	private static int maximumTransactionTimeout=-1;

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

	public static void copyContext(String keys, Map<String,Object> from, Map<String,Object> to, INamedObject requester) {
		if (to!=null) {
			log.debug("returning context, returned sessionkeys ["+keys+"]");
			copyIfExists(PipeLineSession.EXIT_CODE_CONTEXT_KEY, from, to);
			copyIfExists(PipeLineSession.EXIT_STATE_CONTEXT_KEY, from, to);
			if (StringUtils.isNotEmpty(keys)) {
				StringTokenizer st = new StringTokenizer(keys,",;");
				while (st.hasMoreTokens()) {
					String key=st.nextToken();
					copySessionKey(key, from, to, requester);
				}
			} else if (keys==null) { // if keys are not set explicitly ...
				for (String key:from.keySet()) { // ... all keys will be copied
					copySessionKey(key, from, to, requester);
				}
			}
		}
	}

	private static void copySessionKey(String key, Map<String,Object> from, Map<String,Object> to, INamedObject requester) {
		Object value=from.get(key);
		if (value == null) {
			return;
		}
		Message message = Message.asMessage(value);
		if (from instanceof PipeLineSession) {
			message.unscheduleFromCloseOnExitOf((PipeLineSession)from);
		}
		if (to instanceof PipeLineSession) {
			message.closeOnCloseOf((PipeLineSession)to, requester);
		}
		to.put(key,value);
	}

	private static void copyIfExists(String key, Map<String,Object> from, Map<String,Object> to) {
		if (from.containsKey(key)) {
			to.put(key, from.get(key));
		}
	}

	// IBM specific methods using reflection so no dependency on the iaf-ibm module is required.
	public static String getApplicationDeploymentDescriptorPath() throws IOException {
		try {
			return (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getApplicationDeploymentDescriptorPath").invoke(null);
		} catch (Exception e) {
			if("WAS".equals(AppConstants.getInstance().getString(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, ""))) {
				throw new IOException(e);
			}
			log.debug("Caught NoClassDefFoundError for getApplicationDeploymentDescriptorPath, just not on Websphere Application Server", e);
			return null;
		}
	}

	// IBM specific methods using reflection so no dependency on the iaf-ibm module is required.
	public static String getDeployedApplicationBindings() throws IOException {
		String addp = getApplicationDeploymentDescriptorPath();
		if (addp==null) {
			log.debug("applicationDeploymentDescriptorPath not found");
			return null;
		}
		String appBndFile = addp + File.separator + "ibm-application-bnd.xmi";
		log.debug("deployedApplicationBindingsFile [" + appBndFile + "]");
		return StreamUtil.fileToString(appBndFile);
	}

	// IBM specific methods using reflection so no dependency on the iaf-ibm module is required.
	public static String getApplicationDeploymentDescriptor() throws IOException {
		String addp = getApplicationDeploymentDescriptorPath();
		if (addp==null) {
			log.debug("applicationDeploymentDescriptorPath not found");
			return null;
		}
		String appFile = addp + File.separator + "application.xml";
		log.debug("applicationDeploymentDescriptorFile [" + appFile + "]");
		return StreamUtil.fileToString(appFile);
	}

	// IBM specific methods using reflection so no dependency on the iaf-ibm module is required.
	public static String getConfigurationResources() {
		try {
			String path = (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getConfigurationResourcePath").invoke(null);
			return StreamUtil.fileToString(path);
		} catch (Exception e) {
			log.debug("Caught NoClassDefFoundError for getConfigurationResources, just not on Websphere Application Server", e);
			return null;
		}
	}

	// IBM specific methods using reflection so no dependency on the iaf-ibm module is required.
	public static String getConfigurationServer() throws IOException {
		try {
			String path = (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getConfigurationServerPath").invoke(null);
			return StreamUtil.fileToString(path);
		} catch (Exception e) {
			log.debug("Caught NoClassDefFoundError for getConfigurationServer, just not on Websphere Application Server ({}): {}", () -> e.getClass().getName(), () -> e.getMessage());
			return null;
		}
	}

	public static String getConnectionPoolProperties(String confResString, String providerType, String jndiName) {
		try {
			Class<?>[] args_types = new Class<?>[3];
			args_types[0] = String.class;
			args_types[1] = String.class;
			args_types[2] = String.class;
			Object[] args = new Object[3];
			args[0] = confResString;
			args[1] = providerType;
			args[2] = jndiName;
			return (String) Class
					.forName("nl.nn.adapterframework.util.IbmMisc")
					.getMethod("getConnectionPoolProperties", args_types)
					.invoke(null, args);
		} catch (Exception e) {
			log.debug("Caught NoClassDefFoundError for getConnectionPoolProperties, just not on Websphere Application Server", e);
			return null;
		}
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
	public static String toFileSize(long value) {
		return toFileSize(value, false);
	}

	/**
	 * @see #toFileSize(long, boolean, boolean)
	 */
	public static String toFileSize(long value, boolean format) {
		return toFileSize(value, format, false);
	}

	/**
	 * Converts the input value in bytes to the highest degree of file size, and formats and floors the value, if set to true.
	 * <pre>
	 *      String mb = Misc.toFileSize(15000000, true); // gives out "14 MB"
	 *      String kb = Misc.toFileSize(150000, false, true); // gives out "146KB"
	 * </pre>
	 */
	public static String toFileSize(long value, boolean format, boolean floor) {
		long divider = 1024L * 1024 * 1024;
		String suffix = null;
		if (value >= divider) {
			suffix = "GB";
		} else {
			divider = 1024L * 1024;
			if (value >= divider) {
				suffix = "MB";
			} else {
				divider = 1024;
				if (value >= divider) {
					if (format) {
						suffix = "kB";
					} else {
						suffix = "KB";
					}
				}
			}
		}
		if (suffix == null) {
			if (format) {
				if (value > 0) {
					return "1 kB";
				}
				return "0 kB";
			}
			return value + (floor ? "B" : "");
		}
		float f = (float) value / divider;
		return Math.round(f) + (format ? " " : "") + suffix;
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
	 * <pre>
	 *      List<String> list = new ArrayList<>();
	 *      list.add("We Are");
	 *      list.add(" Frank");
	 *      String res = Misc.listToString(list); // res gives out "We Are Frank"
	 * </pre>
	 */
	public static String listToString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s);
		}
		return sb.toString();
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

	public static String getFileSystemTotalSpace() {
		try {
			File systemDir = getSystemDir();
			if (systemDir == null) return null;
			long l = systemDir.getTotalSpace();
			return toFileSize(l);
		} catch ( Exception e ) {
			log.debug("Caught Exception",e);
			return null;
		}
	}

	public static String getFileSystemFreeSpace() {
		try {
			File systemDir = getSystemDir();
			if (systemDir == null) return null;
			long l = systemDir.getFreeSpace();
			return toFileSize(l);
		} catch ( Exception e ) {
			log.debug("Caught Exception",e);
			return null;
		}
	}

	private static File getSystemDir() {
		String dirName = System.getProperty("APPSERVER_ROOT_DIR");
		if (dirName==null) {
			dirName = System.getProperty("user.dir");
			if (dirName==null) {
				return null;
			}
		}
		return new File(dirName);
	}

	public static Integer getTotalTransactionLifetimeTimeout() {
		String confSrvString = null;
		try {
			confSrvString = getConfigurationServer();
		} catch (Exception e) {
			log.warn("Exception getting configurationServer",e);
		}
		return getTotalTransactionLifetimeTimeout(confSrvString);
	}

	public static Integer getTotalTransactionLifetimeTimeout(String configServerXml){
		if (configServerXml==null) {
			return null;
		}
		try {
			String confSrvString = XmlUtils.removeNamespaces(configServerXml);
			confSrvString = XmlUtils.removeNamespaces(confSrvString);
			String xPath = "Server/components/services/@totalTranLifetimeTimeout";
			TransformerPool tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(xPath));
			String ttlt = tp.transform(confSrvString, null);
			if(ttlt != null) {
				return Integer.valueOf(ttlt);
			}
		} catch(Exception e) {
			log.warn("Exception getting totalTransactionLifetimeTimeout",e);
		}
		return null;
	}

	public static int getMaximumTransactionTimeout() {
		if (maximumTransactionTimeout<0) {
			Integer transactionTimeout=0;
			try {
				String confSrvString = null;
				try {
					confSrvString = getConfigurationServer();
				} catch (Exception e) {
					log.warn("Exception getting configurationServer ({}): {}", e.getClass().getName(), e.getMessage());
				}
				transactionTimeout=getMaximumTransactionTimeout(confSrvString);
			} finally {
				maximumTransactionTimeout=transactionTimeout!=null ? transactionTimeout : 0;
			}
		}
		return maximumTransactionTimeout;
	}

	public static Integer getMaximumTransactionTimeout(String configServerXml) {
		if (configServerXml==null) {
			return null;
		}
		try {
			String confSrvString = XmlUtils.removeNamespaces(configServerXml);
			confSrvString = XmlUtils.removeNamespaces(confSrvString);
			String xPath = "Server/components/services/@propogatedOrBMTTranLifetimeTimeout";
			TransformerPool tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(xPath));
			String mtt = tp.transform(confSrvString, null);
			if(StringUtils.isNotEmpty(mtt)) {
				return Integer.valueOf(mtt);
			}
		} catch(Exception e) {
			log.warn("Exception getting maximumTransactionTimeout",e);
		}
		return null;
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

	public static String getProjectBaseDir() {
		String buildOutputDirectory = getBuildOutputDirectory();
		if (buildOutputDirectory != null) {
			// classic java project: {project.basedir}/WebContent/WEB-INF/classes
			// maven project: {project.basedir}/target/classes
			File dir = new File(buildOutputDirectory);
			while (dir != null) {
				String name = dir.getName();
				if ("WebContent".equalsIgnoreCase(name)
						|| "target".equalsIgnoreCase(name)) {
					return dir.getParent();
				}
				dir = dir.getParentFile();
			}
		}
		return null;
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
