/*
   Copyright 2013 Nationale-Nederlanden

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;


/**
 * Miscellanous conversion functions.
 */
public class Misc {
	static Logger log = LogUtil.getLogger(Misc.class);
	public static final int BUFFERSIZE=20000;
	public static final String DEFAULT_INPUT_STREAM_ENCODING="UTF-8";
	public static final String MESSAGE_SIZE_WARN_BY_DEFAULT_KEY = "message.size.warn.default";
	public static final String RESPONSE_BODY_SIZE_WARN_BY_DEFAULT_KEY = "response.body.size.warn.default";
	public static final String FORCE_FIXED_FORWARDING_BY_DEFAULT_KEY = "force.fixed.forwarding.default";

	private static Long messageSizeWarnByDefault = null;
	private static Long responseBodySizeWarnByDefault = null;
	private static Boolean forceFixedForwardingByDefault = null;

	/**
	* Creates a Universally Unique Identifier, via the java.rmi.server.UID class.
	*/
	public static String createSimpleUUID() {
		UID uid = new UID();

		String uidString=asHex(getIPAddress())+"-"+uid.toString();
		// Replace semi colons by underscores, so IBIS will support it
		uidString = uidString.replace(':', '_');
		return uidString;
	}

	static public String createUUID() {
		return createSimpleUUID();
	}

	/**
	* Creates a Universally Unique Identifier, via the java.util.UUID class (36 characters or 32 characters without dashes).
	*/
	static public String createRandomUUID(boolean removeDashes) {
		String uuidString = java.util.UUID.randomUUID().toString();
		if (removeDashes) {
			return uuidString.replaceAll("-", "");
		} else {
			return uuidString;
		}
	}

	static public String createRandomUUID() {
		return createRandomUUID(false);
	}

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String asHex(byte[] buf)
    {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i)
        {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

	static private byte[] getIPAddress() {
		InetAddress inetAddress = null;

		try {
			inetAddress = InetAddress.getLocalHost();
			return inetAddress.getAddress();
		}

		catch (UnknownHostException uhe) {
			return new byte[] {127,0,0,1};
		}
	}

	static public String createNumericUUID() {
		byte[] ipAddress = getIPAddress();
		DecimalFormat df = new DecimalFormat("000");
		String ia1 = df.format(unsignedByteToInt(ipAddress[0]));
		String ia2 = df.format(unsignedByteToInt(ipAddress[1]));
		String ia3 = df.format(unsignedByteToInt(ipAddress[2]));
		String ia4 = df.format(unsignedByteToInt(ipAddress[3]));
		String ia = ia1 + ia2 + ia3 + ia4;

		long hashL = Math.round(Math.random() * 1000000);
		df = new DecimalFormat("000000");
		String hash = df.format(hashL);

		//Unique string is <ipaddress with length 4*3><currentTime with length 13><hashcode with length 6>
		StringBuffer s = new StringBuffer();
		s.append(ia).append(getCurrentTimeMillis()).append(hash);

		return s.toString();
	}

	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
		}


	public static synchronized long getCurrentTimeMillis(){
		return System.currentTimeMillis();
	}

	public static void fileToWriter(String filename, Writer writer) throws IOException {
		readerToWriter(new FileReader(filename), writer);
	}

	public static void fileToStream(String filename, OutputStream output) throws IOException {
		streamToStream(new FileInputStream(filename), output);
	}

	public static void streamToStream(InputStream input, OutputStream output) throws IOException {
		streamToStream(input,output,true);
	}
	public static void streamToStream(InputStream input, OutputStream output, boolean closeInput) throws IOException {
		if (input!=null) {
			byte buffer[]=new byte[BUFFERSIZE];

			int bytesRead;
			while ((bytesRead=input.read(buffer,0,BUFFERSIZE))>0) {
				output.write(buffer,0,bytesRead);
			}
			if (closeInput) {
				input.close();
			}
		}
	}

	public static void streamToFile(InputStream inputStream, File file)
			throws IOException {
		OutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(file);
			Misc.streamToStream(inputStream, fileOut);
		} finally {
			try {
				if (fileOut != null) {
					fileOut.close();
				}
			} catch (IOException e) {
				log.warn("exception closing outputstream", e);
			}
		}
	}

	public static byte[] streamToBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (true) {
		    int r = inputStream.read(buffer);
		    if (r == -1) break;
		    out.write(buffer, 0, r);
		}

		return out.toByteArray();
	}
	
	
	public static void readerToWriter(Reader reader, Writer writer) throws IOException {
		readerToWriter(reader,writer,true);
	}
	public static void readerToWriter(Reader reader, Writer writer, boolean closeInput) throws IOException {
		if (reader!=null) {
			char buffer[]=new char[BUFFERSIZE];

			int charsRead;
			while ((charsRead=reader.read(buffer,0,BUFFERSIZE))>0) {
				writer.write(buffer,0,charsRead);
			}
			if (closeInput) {
				reader.close();
			}
		}
	}

	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	public static String fileToString(String fileName) throws IOException {
		return fileToString(fileName, null, false);
	}
	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	public static String fileToString(String fileName, String endOfLineString) throws IOException {
		return fileToString(fileName, endOfLineString, false);
	}
	/**
	  * Please consider using resourceToString() instead of relying on files.
	 */
	public static String fileToString(String fileName, String endOfLineString, boolean xmlEncode) throws IOException {
		FileReader reader = new FileReader(fileName);
		try {
			return readerToString(reader, endOfLineString, xmlEncode);
		}
		finally {
			reader.close();
		}
	}


	public static String readerToString(Reader reader, String endOfLineString, boolean xmlEncode) throws IOException {
		StringBuffer sb = new StringBuffer();
		int curChar = -1;
		int prevChar = -1;
		while ((curChar = reader.read()) != -1 || prevChar == '\r') {
			if (prevChar == '\r' || curChar == '\n') {
				if (endOfLineString == null) {
					if (prevChar == '\r')
						sb.append((char) prevChar);
					if (curChar == '\n')
						sb.append((char) curChar);
				}
				else {
					sb.append(endOfLineString);
				}
			}
			if (curChar != '\r' && curChar != '\n' && curChar != -1) {
				String appendStr =""+(char) curChar;
				sb.append(xmlEncode ? (XmlUtils.encodeChars(appendStr)):(appendStr));
			}
			prevChar = curChar;
		}
		return sb.toString();
	}

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding, boolean xmlEncode)
		throws IOException {
		return readerToString(new InputStreamReader(stream,streamEncoding), endOfLineString, xmlEncode);
	}


	public static String streamToString(InputStream stream, String endOfLineString, boolean xmlEncode)
		throws IOException {
		return streamToString(stream,endOfLineString, DEFAULT_INPUT_STREAM_ENCODING, xmlEncode);
	}

	public static String resourceToString(URL resource, String endOfLineString, boolean xmlEncode) throws IOException {
		InputStream stream = resource.openStream();
		try {
			return streamToString(stream, endOfLineString, xmlEncode);
		}
		finally {
			stream.close();
		}
	}

	public static String resourceToString(URL resource) throws IOException {
		return resourceToString(resource, null, false);
	}

	public static String resourceToString(URL resource, String endOfLineString) throws IOException {
		return resourceToString(resource, endOfLineString, false);
	}

	public static void stringToFile(String string, String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		try {
			fw.write(string);
		}
		finally {
			fw.close();
		}
	}

	/**
	 * String replacer.
	 *
	 * @param source	is the original string
	 * @param from		is the string to be replaced
	 * @param to		is the string which will used to replace

	 */
	public static String replace (String source, String from, String to) {
		int start = source.indexOf(from);
		if (start==-1) {
			return source;
		}
		int fromLength = from.length();
		char [] sourceArray = source.toCharArray();

		StringBuffer buffer = new StringBuffer();
		int srcPos=0;

		while (start != -1) {
			buffer.append (sourceArray, srcPos, start-srcPos);
			buffer.append (to);
			srcPos=start+fromLength;
			start = source.indexOf (from, srcPos);
		}
		buffer.append (sourceArray, srcPos, sourceArray.length-srcPos);
		return buffer.toString();
	 }

	public static String concatStrings(String part1, String separator, String part2) {
		if (StringUtils.isEmpty(part1)) {
			return part2;
		}
		if (StringUtils.isEmpty(part2)) {
			return part1;
		}
		return part1+separator+part2;
	}

	public static String hide(String string) {
		return StringUtils.repeat("*", string.length());
	}

	public static String byteArrayToString(byte[] input, String endOfLineString, boolean xmlEncode) throws IOException{
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		return streamToString(bis, endOfLineString, xmlEncode);
	}


	public static byte[] gzip(String input) throws IOException {
		return gzip(input.getBytes(DEFAULT_INPUT_STREAM_ENCODING));
	}

	public static byte[] gzip(byte[] input) throws IOException {

		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
		GZIPOutputStream gz = new GZIPOutputStream(bos);
		gz.write(input);
		gz.close();
		bos.close();

		// Get the compressed data
		return bos.toByteArray();
	}


	public static String gunzipToString(byte[] input) throws DataFormatException, IOException {
		return byteArrayToString(gunzip(input),"\n",false);
	}

	public static byte[] gunzip(byte[] input) throws DataFormatException, IOException {

		// Create an expandable byte array to hold the decompressed data
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		GZIPInputStream gz = new GZIPInputStream(bis);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		// Decompress the data
		byte[] buf = new byte[1024];
		while (gz.available()>0) {
			 int count = gz.read(buf,0,1024);
			 if (count>0) {
				bos.write(buf, 0, count);
			 }
		}
		bos.close();

		// Get the decompressed data
		return bos.toByteArray();
	}


	public static byte[] compress(String input) throws IOException {
		return compress(input.getBytes(DEFAULT_INPUT_STREAM_ENCODING));
	}
	public static byte[] compress(byte[] input) throws IOException {

		// Create the compressor with highest level of compression
		Deflater compressor = new Deflater();
		compressor.setLevel(Deflater.BEST_COMPRESSION);

		// Give the compressor the data to compress
		compressor.setInput(input);
		compressor.finish();

		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		// Compress the data
		byte[] buf = new byte[1024];
		while (!compressor.finished()) {
			int count = compressor.deflate(buf);
			bos.write(buf, 0, count);
		}
		bos.close();

		// Get the compressed data
		return bos.toByteArray();
	}

	public static String decompressToString(byte[] input) throws DataFormatException, IOException {
		return byteArrayToString(decompress(input),"\n",false);
	}

	public static byte[] decompress(byte[] input) throws DataFormatException, IOException {
		// Create the decompressor and give it the data to compress
		Inflater decompressor = new Inflater();
		decompressor.setInput(input);

		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		// Decompress the data
		byte[] buf = new byte[1024];
		while (!decompressor.finished()) {
			 int count = decompressor.inflate(buf);
			 bos.write(buf, 0, count);
		}
			 bos.close();

		// Get the decompressed data
		return bos.toByteArray();
	}

	public static Properties getEnvironmentVariables() throws IOException {
		Properties props = new Properties();

		try {
			Method getenvs = System.class.getMethod( "getenv", (java.lang.Class[]) null );
			Map env = (Map) getenvs.invoke( null, (java.lang.Object[]) null );
			for (Iterator it = env.keySet().iterator(); it.hasNext();) {
				String key = (String)it.next();
				String value = (String)env.get(key);
				props.setProperty(key,value);
			}
		} catch ( NoSuchMethodException e ) {
			log.debug("Caught NoSuchMethodException, just not on JDK 1.5: "+e.getMessage());
		} catch ( IllegalAccessException e ) {
			log.debug("Caught IllegalAccessException, using JDK 1.4 method",e);
		} catch ( InvocationTargetException e ) {
			log.debug("Caught InvocationTargetException, using JDK 1.4 method",e);
		}

		if (props.size() == 0) {
			BufferedReader br = null;
			Process p = null;
			Runtime r = Runtime.getRuntime();
			String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("windows 9") > -1) {
				p = r.exec("command.com /c set");
			} else if (
				(OS.indexOf("nt") > -1)
					|| (OS.indexOf("windows 20") > -1)
					|| (OS.indexOf("windows xp") > -1)) {
				p = r.exec("cmd.exe /c set");
			} else {
				//assume Unix
				p = r.exec("env");
			}
//			props.load(p.getInputStream()); // this does not work, due to potential malformed escape sequences
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
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

	public static String getHostname() {
		String localHost=null;
		// String localIP="";
		try {
			InetAddress localMachine = InetAddress.getLocalHost();
			//localIP = localMachine.getHostAddress();
			localHost = localMachine.getHostName();
		} catch(UnknownHostException uhe) {
			if (localHost==null) {
				localHost="unknown ("+uhe.getMessage()+")";
			}
		}
		return localHost;
	}

	public static void copyContext(String keys, Map from, Map to) {
		if (StringUtils.isNotEmpty(keys) && from!=null && to!=null) {
			StringTokenizer st = new StringTokenizer(keys,",;");
			while (st.hasMoreTokens()) {
				String key=st.nextToken();
				Object value=from.get(key);
				to.put(key,value);
			}
		}
	}

	public static String getDeployedApplicationBindings() throws IOException {
		String addp = getApplicationDeploymentDescriptorPath();
		if (addp==null) {
			log.debug("applicationDeploymentDescriptorPath not found");
			return null;
		}
		String appBndFile = addp + File.separator + "ibm-application-bnd.xmi";
		log.debug("deployedApplicationBindingsFile [" + appBndFile + "]");
		return fileToString(appBndFile);
	}

	public static String getApplicationDeploymentDescriptorPath() throws IOException {
		try {
            return (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getApplicationDeploymentDescriptorPath").invoke(null);
		} catch (Exception e) {
            log.debug("Caught NoClassDefFoundError, just not on Websphere Application Server: " + e.getMessage());
            return null;
        }
	}

	public static String getApplicationDeploymentDescriptor () throws IOException {
		String addp = getApplicationDeploymentDescriptorPath();
		if (addp==null) {
			log.debug("applicationDeploymentDescriptorPath not found");
			return null;
		}
		String appFile = addp + File.separator + "application.xml";
		log.debug("applicationDeploymentDescriptorFile [" + appFile + "]");
		return fileToString(appFile);
	}

	public static String getConfigurationResources() throws IOException {
        try {
            return (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getConfigurationResources").invoke(null);
        } catch (Exception e) {
            log.debug("Caught NoClassDefFoundError, just not on Websphere Application Server: " + e.getMessage());
            return null;
        }
	}

	public static String getConfigurationServer() throws IOException {
        try {
            return (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getConfigurationServer").invoke(null);
        } catch (Exception e) {
            log.debug("Caught NoClassDefFoundError, just not on Websphere Application Server: " + e.getMessage());
            return null;
        }
	}

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
		  multiplier = 1024*1024;
		  s = s.substring(0, index);
		}
		else if((index = s.indexOf("GB")) != -1) {
		  multiplier = 1024*1024*1024;
		  s = s.substring(0, index);
		}
		if(s != null) {
			try {
				return Long.valueOf(s).longValue() * multiplier;
			}
			catch (NumberFormatException e) {
				log.error("[" + value + "] not in expected format", e);
			}
		}
		return defaultValue;
	  }

	public static String toFileSize(long value) {
		return toFileSize(value, false);
	}

	public static String toFileSize(long value, boolean format) {
		long divider = 1024*1024*1024;
		String suffix = null;
		if (value>=divider) {
			suffix = "GB";
		} else {
			divider = 1024*1024;
			if (value>=divider) {
				suffix = "MB";
			} else {
				divider = 1024;
				if (value>=divider) {
					if (format) {
						suffix = "kB";
					} else {
						suffix = "KB";
					}
				}
			}
		}
		if (suffix==null) {
			if (format) {
				if (value>0) {
					return "1 kB";
				} else {
					return "0 kB";
				}
			} else {
				return Long.toString(value);
			}
		} else {
			float f = (float)value / divider;
			return Math.round(f) + (format?" ":"") + suffix;
		}
	}

	public static synchronized long getMessageSizeWarnByDefault() {
		if (messageSizeWarnByDefault==null) {
			String definitionString=AppConstants.getInstance().getString(MESSAGE_SIZE_WARN_BY_DEFAULT_KEY, null);
			long definition=toFileSize(definitionString, -1);
			messageSizeWarnByDefault = new Long(definition);
		}
		return messageSizeWarnByDefault.longValue();
	}

	public static synchronized long getResponseBodySizeWarnByDefault() {
		if (responseBodySizeWarnByDefault==null) {
			String definitionString=AppConstants.getInstance().getString(RESPONSE_BODY_SIZE_WARN_BY_DEFAULT_KEY, null);
			long definition=toFileSize(definitionString, -1);
			responseBodySizeWarnByDefault = new Long(definition);
		}
		return responseBodySizeWarnByDefault.longValue();
	}

	public static synchronized boolean isForceFixedForwardingByDefault() {
		if (forceFixedForwardingByDefault==null) {
			boolean force=AppConstants.getInstance().getBoolean(FORCE_FIXED_FORWARDING_BY_DEFAULT_KEY, false);
			forceFixedForwardingByDefault = new Boolean(force);
		}
		return forceFixedForwardingByDefault.booleanValue();
	}

	public static String listToString(List list) {
		StringBuffer sb = new StringBuffer();
		for (Iterator it=list.iterator(); it.hasNext();) {
			sb.append((String) it.next());
		}
		return sb.toString();
	}

	public static String getFileSystemTotalSpace() {
		try {
			Method getTotalSpace = File.class.getMethod("getTotalSpace", (java.lang.Class[]) null);
			String dirName = System.getProperty("APPSERVER_ROOT_DIR");
			if (dirName==null) {
				dirName = System.getProperty("user.dir");
				if (dirName==null) {
					return null;
				}
			}
			File file = new File(dirName);
			long l = ((Long) getTotalSpace.invoke(file, (java.lang.Object[]) null)).longValue();
			return toFileSize(l);
		} catch ( NoSuchMethodException e ) {
			log.debug("Caught NoSuchMethodException, just not on JDK 1.6: "+e.getMessage());
			return null;
		} catch ( Exception e ) {
			log.debug("Caught Exception",e);
			return null;
		}
	}

	public static String getFileSystemFreeSpace() {
		try {
			Method getFreeSpace = File.class.getMethod("getFreeSpace", (java.lang.Class[]) null);
			String dirName = System.getProperty("APPSERVER_ROOT_DIR");
			if (dirName==null) {
				dirName = System.getProperty("user.dir");
				if (dirName==null) {
					return null;
				}
			}
			File file = new File(dirName);
			long l = ((Long) getFreeSpace.invoke(file, (java.lang.Object[]) null)).longValue();
			return toFileSize(l);
		} catch ( NoSuchMethodException e ) {
			log.debug("Caught NoSuchMethodException, just not on JDK 1.6: "+e.getMessage());
			return null;
		} catch ( Exception e ) {
			log.debug("Caught Exception",e);
			return null;
		}
	}

	public static String getTotalTransactionLifetimeTimeout() throws IOException, DomBuilderException, TransformerException {
		String confSrvString = getConfigurationServer();
		if (confSrvString==null) {
			return null;
		}
		confSrvString = XmlUtils.removeNamespaces(confSrvString);
		String xPath = "Server/components/services/@totalTranLifetimeTimeout";
		TransformerPool tp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(xPath));
		return tp.transform(confSrvString, null);
	}

	public static String getMaximumTransactionTimeout() throws IOException, DomBuilderException, TransformerException {
		String confSrvString = getConfigurationServer();
		if (confSrvString==null) {
			return null;
		}
		confSrvString = XmlUtils.removeNamespaces(confSrvString);
		String xPath = "Server/components/services/@propogatedOrBMTTranLifetimeTimeout";
		TransformerPool tp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(xPath));
		return tp.transform(confSrvString, null);
	}

	public static String getSystemTransactionTimeout() {
		String totalTransactionLifetimeTimeout;
		String maximumTransactionTimeout;
		try {
			totalTransactionLifetimeTimeout = Misc.getTotalTransactionLifetimeTimeout();
		} catch (Exception e) {
			log.warn("Exception getting totalTransactionLifetimeTimeout",e);
			totalTransactionLifetimeTimeout = null;
		}
		try {
			maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
		} catch (Exception e) {
			log.warn("Exception getting maximumTransactionTimeout",e);
			maximumTransactionTimeout = null;
		}
		if (totalTransactionLifetimeTimeout==null || maximumTransactionTimeout==null) {
			return null;
		} else {
			if (StringUtils.isNumeric(totalTransactionLifetimeTimeout) && StringUtils.isNumeric(maximumTransactionTimeout)) {
				int ttlf = Integer.parseInt(totalTransactionLifetimeTimeout);
				int mtt = Integer.parseInt(maximumTransactionTimeout);
				int stt = Math.min(ttlf, mtt);
				return String.valueOf(stt);
			} else {
				return null;
			}
		}
	  }

	public static String getAge(long value) {
		long currentTime = (new Date()).getTime();
		long age = currentTime - value;
		String ageString = DurationFormatUtils.formatDuration(age, "d") + "d";
		if (ageString.equals("0d")) {
			;
			ageString = DurationFormatUtils.formatDuration(age, "H") + "h";
			if (ageString.equals("0h")) {
				;
				ageString = DurationFormatUtils.formatDuration(age, "m") + "m";
				if (ageString.equals("0m")) {
					;
					ageString = DurationFormatUtils.formatDuration(age, "s")
							+ "s";
				}
			}
		}
		return ageString;
	}

	public static long parseAge(String value, long defaultValue) {
		if (value == null)
			return defaultValue;

		String s = value.trim().toUpperCase();
		long multiplier = 1;
		int index;

		if ((index = s.indexOf("S")) != -1) {
			multiplier = 1000L;
			s = s.substring(0, index);
		} else if ((index = s.indexOf("M")) != -1) {
			multiplier = 60L * 1000L;
			s = s.substring(0, index);
		} else if ((index = s.indexOf("H")) != -1) {
			multiplier = 60L * 60L * 1000L;
			s = s.substring(0, index);
		} else if ((index = s.indexOf("D")) != -1) {
			multiplier = 24L * 60L * 60L * 1000L;
			s = s.substring(0, index);
		}
		if (s != null) {
			try {
				return Long.valueOf(s).longValue() * multiplier;
			} catch (NumberFormatException e) {
				log.error("[" + value + "] not in expected format", e);
			}
		}
		return defaultValue;
	}
	
	public static String hideFirstHalf(String inputString, String regex) {
		return hideAll(inputString, regex, 1);
	}
	
	public static String hideAll(String inputString, String regex) {
		return hideAll(inputString, regex, 0);
	}
	
	public static String hideAll(String inputString, String regex, int mode) {
		StringBuilder result = new StringBuilder();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(inputString);
		int previous = 0;
		while (matcher.find()) {
			result.append(inputString.substring(previous, matcher.start()));
			int len = matcher.end() - matcher.start();
			if (mode == 1) {
				int lenFirstHalf = (int) Math.ceil((double) len / 2);
				result.append(StringUtils.repeat("*", lenFirstHalf));
				result.append(inputString.substring(matcher.start()
						+ lenFirstHalf, matcher.start() + len));
			} else {
				result.append(StringUtils.repeat("*", len));
			}
			previous = matcher.end();
		}
		result.append(inputString.substring(previous, inputString.length()));
		return result.toString();
	}
}
