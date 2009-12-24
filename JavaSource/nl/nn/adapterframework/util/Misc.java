/*
 * $Log: Misc.java,v $
 * Revision 1.26  2009-12-24 08:27:55  m168309
 * added methods getResponseBodySizeWarnByDefault and getResponseBodySizeErrorByDefault
 *
 * Revision 1.25  2009/11/12 12:36:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Pipeline: added attributes messageSizeWarn and messageSizeError
 *
 * Revision 1.24  2009/11/10 10:27:40  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * method getEnvironmentVariables splitted for J2SE 1.4 and J2SE 5.0
 *
 * Revision 1.23  2009/01/29 07:02:29  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * removed "j2c.properties resource path" from getEnvironmentVariables()
 *
 * Revision 1.22  2009/01/28 11:21:29  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added "j2c.properties resource path" to getEnvironmentVariables()
 *
 * Revision 1.21  2008/12/15 12:21:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added getApplicationDeploymentDescriptor
 *
 * Revision 1.20  2008/12/15 09:39:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * getDeployedApplicationBindings: replaced property WAS_HOMES by user.install.root
 *
 * Revision 1.19  2008/12/08 13:06:58  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added createNumericUUID
 *
 * Revision 1.18  2008/11/25 10:17:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added getDeployedApplicationName and getDeployedApplicationBindings
 *
 * Revision 1.17  2008/09/08 15:00:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE in getEnvironmentVariables
 *
 * Revision 1.16  2008/08/27 16:24:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made closeInput optional in streamToStream
 *
 * Revision 1.15  2007/09/05 13:05:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added function to copy context
 *
 * Revision 1.14  2007/06/12 11:24:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getHostname()
 *
 * Revision 1.13  2005/10/27 08:43:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved getEnvironmentVariables to Misc
 *
 * Revision 1.12  2005/10/17 11:26:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * addded concatString and compression-functions
 *
 * Revision 1.11  2005/09/22 15:54:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added replace function
 *
 * Revision 1.10  2005/07/19 11:37:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added stream copying functions
 *
 * Revision 1.9  2004/10/26 15:36:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set UTF-8 as default inputstream encoding
 *
 */
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Miscellanous conversion functions.
 * @version Id
 */
public class Misc {
	public static final String version="$RCSfile: Misc.java,v $ $Revision: 1.26 $ $Date: 2009-12-24 08:27:55 $";
	static Logger log = LogUtil.getLogger(Misc.class);
	public static final int BUFFERSIZE=20000;
	public static final String DEFAULT_INPUT_STREAM_ENCODING="UTF-8";
	public static final String MESSAGE_SIZE_WARN_BY_DEFAULT_KEY = "message.size.warn.default";
	public static final String MESSAGE_SIZE_ERROR_BY_DEFAULT_KEY = "message.size.error.default";
	public static final String RESPONSE_BODY_SIZE_WARN_BY_DEFAULT_KEY = "response.body.size.warn.default";
	public static final String RESPONSE_BODY_SIZE_ERROR_BY_DEFAULT_KEY = "response.body.size.error.default";

	private static Long messageSizeWarnByDefault = null;
	private static Long messageSizeErrorByDefault = null;
	private static Long responseBodySizeWarnByDefault = null;
	private static Long responseBodySizeErrorByDefault = null;

	public static String createSimpleUUID_old() {
		StringBuffer sb = new StringBuffer();
		sb.append(System.currentTimeMillis());
		sb.append('-');
		sb.append(Math.round(Math.random() * 1000000));
		return sb.toString();
	}

	/**
	* Creates a Universally Unique Identifier, via the java.rmi.server.UID class.
	*/
	public static String createSimpleUUID() {
		UID uid = new UID();

		// Replace semi colons by underscores, so IBIS will support it
		String uidString = uid.toString().replace(':', '_');
		return uidString;
	}
	/**
	* Creates a Universally Unique Identifier.
	*
	* Similar to javax.mail.internet.UniqueValue, this implementation
	* generates a unique value by random number, the current
	* time (in milliseconds), and this system's hostname generated by
	* <code>InternetAddress.getLocalAddress()</code>.
	* @return A unique identifier is returned.
	*/
	static public String createUUID() {
		String user = System.getProperty("user.name");
		String ipAddress = getIPAddress();

		StringBuffer s = new StringBuffer();

		//Unique string is <ipaddress>.<currentTime>.<username>.<hashcode>
		s.append(ipAddress).append('.').append(System.currentTimeMillis()).append('.').append(user).append('.').append(
			Math.round(Math.random() * 1000000));

		return s.toString();
	}

	static private String getIPAddress() {
		InetAddress inetAddress = null;
		String ipAddress = null;

		try {
			inetAddress = InetAddress.getLocalHost();
			return inetAddress.getHostAddress();
		}

		catch (UnknownHostException uhe) {
			return "127.0.0.1";
		}
	}

	static public String createNumericUUID() {
		String ipAddress = getIPAddress();
		DecimalFormat df = new DecimalFormat("000");
		String[] iaArray = ipAddress.split("[.]");
		String ia1 = df.format(Integer.parseInt(iaArray[0]));
		String ia2 = df.format(Integer.parseInt(iaArray[1]));
		String ia3 = df.format(Integer.parseInt(iaArray[2]));
		String ia4 = df.format(Integer.parseInt(iaArray[3]));
		String ia = ia1 + ia2 + ia3 + ia4;

		long hashL = Math.round(Math.random() * 1000000);
		df = new DecimalFormat("000000");
		String hash = df.format(hashL);

		//Unique string is <ipaddress with length 4*3><currentTime with length 13><hashcode with length 6>
		StringBuffer s = new StringBuffer();
		s.append(ia).append(getCurrentTimeMillis()).append(hash);

		return s.toString();
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

	public static void readerToWriter(Reader reader, Writer writer) throws IOException {
		if (reader!=null) {
			char buffer[]=new char[BUFFERSIZE]; 
				
			int charsRead;
			while ((charsRead=reader.read(buffer,0,BUFFERSIZE))>0) {
				writer.write(buffer,0,charsRead);
			}
			reader.close();
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
	 * @param target	is the original string
	 * @param from		is the string to be replaced
	 * @param to		is the string which will used to replace
	 * @return
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
			// ok, just not on JDK 1.5
		} catch ( IllegalAccessException e ) {
			// Unexpected error obtaining environment - using JDK 1.4 method
		} catch ( InvocationTargetException e ) {
			// Unexpected error obtaining environment - using JDK 1.4 method
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

	public static String getDeployedApplicationName() {
		URL url= ClassUtils.getResourceURL(Misc.class, "");
		String path = url.getPath();
		log.debug("classloader resource [" + path + "]");
		StringTokenizer st = new StringTokenizer(path, File.separator);
		String appName = null;
		while (st.hasMoreTokens() && appName == null) {
			String token = st.nextToken();
			if (StringUtils.upperCase(token).endsWith(".EAR")) {
				appName = token.substring(0,token.length()-4);
			}
		}
		log.debug("deployedApplicationName [" + appName + "]");
		return appName;
	}

	public static String getDeployedApplicationBindings(String appName) throws IOException {
		String appBndFile =
		getApplicationDeploymentDescriptorPath(appName)
				+ File.separator
				+ "ibm-application-bnd.xmi";
		log.debug("deployedApplicationBindingsFile [" + appBndFile + "]");
		return fileToString(appBndFile);
	}

	public static String getApplicationDeploymentDescriptorPath(String appName) throws IOException {
		String appPath =
//			"${WAS_HOME}"
			"${user.install.root}"
				+ File.separator
				+ "config"
				+ File.separator
				+ "cells"
				+ File.separator
				+ "${WAS_CELL}"
				+ File.separator
				+ "applications"
				+ File.separator
				+ appName
				+ ".ear"
				+ File.separator
				+ "deployments"
				+ File.separator
				+ appName
				+ File.separator
				+ "META-INF";
		Properties props = Misc.getEnvironmentVariables();
		props.putAll(System.getProperties());
		String resolvedAppPath = StringResolver.substVars(appPath, props);
		return resolvedAppPath;
	}

	public static String getApplicationDeploymentDescriptor (String appName) throws IOException {
		String appFile =
			getApplicationDeploymentDescriptorPath(appName)
				+ File.separator
				+ "application.xml";
		log.debug("applicationDeploymentDescriptor [" + appFile + "]");
		return fileToString(appFile);
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
					suffix = "KB"; 
				}
			}
		}
		if (suffix==null) {
			return Long.toString(value);
		} else {
			float f = (float)value / divider;
			return Math.round(f) + suffix;
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

	public static synchronized long getMessageSizeErrorByDefault() {
		if (messageSizeErrorByDefault==null) {
			String definitionString=AppConstants.getInstance().getString(MESSAGE_SIZE_ERROR_BY_DEFAULT_KEY, null);
			long definition=toFileSize(definitionString, -1);
			messageSizeErrorByDefault = new Long(definition);
		}
		return messageSizeErrorByDefault.longValue();
	}

	public static synchronized long getResponseBodySizeWarnByDefault() {
		if (responseBodySizeWarnByDefault==null) {
			String definitionString=AppConstants.getInstance().getString(RESPONSE_BODY_SIZE_WARN_BY_DEFAULT_KEY, null);
			long definition=toFileSize(definitionString, -1);
			responseBodySizeWarnByDefault = new Long(definition);
		}
		return responseBodySizeWarnByDefault.longValue();
	}

	public static synchronized long getResponseBodySizeErrorByDefault() {
		if (responseBodySizeErrorByDefault==null) {
			String definitionString=AppConstants.getInstance().getString(RESPONSE_BODY_SIZE_ERROR_BY_DEFAULT_KEY, null);
			long definition=toFileSize(definitionString, -1);
			responseBodySizeErrorByDefault = new Long(definition);
		}
		return responseBodySizeErrorByDefault.longValue();
	}
}