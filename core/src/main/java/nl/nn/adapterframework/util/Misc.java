/*
   Copyright 2013, 2018 Nationale-Nederlanden

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.text.DecimalFormat;
import java.util.Collection;
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


/**
 * Miscellaneous conversion functions.
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
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

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

	public static String createUUID() {
		return createSimpleUUID();
	}

	/**
	 * Creates a Universally Unique Identifier, via the java.util.UUID class (36 characters or 32 characters without dashes).
	 */
	public static String createRandomUUID(boolean removeDashes) {
		String uuidString = java.util.UUID.randomUUID().toString();
		if (removeDashes) {
			return uuidString.replaceAll("-", "");
		} else {
			return uuidString;
		}
	}

	public static String createRandomUUID() {
		return createRandomUUID(false);
	}

	/**
	 *
	 * @return the hexadecimal string representation of the byte array.
	 */
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

	/**
	 *
	 * @return the ip address of the machine that the program runs on.
	 */
	private static byte[] getIPAddress() {
		InetAddress inetAddress = null;

		try {
			inetAddress = InetAddress.getLocalHost();
			return inetAddress.getAddress();
		}

		catch (UnknownHostException uhe) {
			return new byte[] {127,0,0,1};
		}
	}

	/**
	 *
	 * @return a unique UUID string with length 31 (ipaddress with length 4*3, currentTime with length 13, hashcode with length 6)
	 */
	public static String createNumericUUID() {
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
		StringBuilder s = new StringBuilder();
		s.append(ia).append(getCurrentTimeMillis()).append(hash);

		return s.toString();
	}

	/**
	 * Converts an unsigned byte to its integer representation.
	 * Examples:
	 * <pre>
	 * Misc.unsignedByteToInt(new Btye(12)) returns 12
	 * Misc.unsignedByteToInt(new Byte(-12)) returns 244
	 * </pre>
	 * @param b byte to be converted.
	 * @return integer that is converted from unsigned byte.
	 */
	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}

	/**
	 * @return the current time in milliseconds.
	 */
	public static synchronized long getCurrentTimeMillis(){
		return System.currentTimeMillis();
	}

	/**
	 * Copies the content of the specified file to a writer.
	 *
	 * <p>
	 *     Example:
	 *     <pre>
	 *         Writer writer = new StringWriter();
	 *         Misc.fileToWriter(someFileName, writer);
	 *         System.out.println(writer.toString) // prints the content of the writer
	 *         				       // that's copied from the file.
	 *     </pre>
	 * </p>
	 * @throws IOException exception to be thrown exception to be thrown if an I/O exception occurs
	 */
	public static void fileToWriter(String filename, Writer writer) throws IOException {
		readerToWriter(new FileReader(filename), writer);
	}

	/**
	 * Copies the content of the specified file to an output stream.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         OutputStream os = new ByteArrayOutputStream
	 *         Misc.fileToStream(someFileName, os);
	 *         System.out.println(os.toString) // prints the content of the output stream
	 *         				   // that's copied from the file.
	 *     </pre>
	 * </p>
	 * @throws IOException exception to be thrown if an I/O exception occurs
	 */
	public static void fileToStream(String filename, OutputStream output) throws IOException {
		streamToStream(new FileInputStream(filename), output);
	}

	/**
	 * Overloaded version of streamToStream that calls the main version with closeInput set to true.
	 * @see #streamToStream(InputStream, OutputStream, boolean)
	 */
	public static void streamToStream(InputStream input, OutputStream output) throws IOException {
		streamToStream(input,output,true);
	}

	/**
	 * Writes the content of an input stream to an output stream by copying the buffer of input stream to the buffer of the output stream.
	 * Closes the input stream if specified.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         OutputStream baos = new ByteArrayOutputStream();
	 *         Misc.streamToStream(bais, baos);
	 *         System.out.println(baos.toString()); // prints "test"
	 *     </pre>
	 * </p>
	 * @param closeInput if set to 'true', the input stream gets closed.
	 * @throws IOException  exception to be thrown if an I/O eexception occurs
	 */
	public static void streamToStream(InputStream input, OutputStream output, boolean closeInput) throws IOException {
		if (input!=null) {
			byte[] buffer=new byte[BUFFERSIZE];
			int bytesRead;
			while ((bytesRead=input.read(buffer,0,BUFFERSIZE))>-1) {
				output.write(buffer,0,bytesRead);
			}
			if (closeInput) {
				input.close();
			}
		}
	}

	/**
	 * Writes the content of an input stream to a specified file.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         Misc.streamToFile(bais, file); // "test" copied inside the file.
	 *     </pre>
	 * </p>
	 * @throws IOException exception to be thrown if an I/O exception occurs
	 */
	public static void streamToFile(InputStream inputStream, File file) throws IOException {
		try (OutputStream fileOut = new FileOutputStream(file)) {
			Misc.streamToStream(inputStream, fileOut);
		}
	}

	/**
	 * Writes the content of an input stream to a byte array.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         byte[] arr = Misc.streamToBytes(bais);
	 *         System.out.println(new String(arr, StandardCharsets.UTF_8)); // prints "test"
	 *     </pre>
	 * </p>
	 */
	public static byte[] streamToBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (true) {
			int r = inputStream.read(buffer);
			if (r == -1) {
				break;
			}
			out.write(buffer, 0, r);
		}

		return out.toByteArray();
	}

	/**
	 * @see #readerToWriter(Reader, Writer, boolean)
	 */
	public static void readerToWriter(Reader reader, Writer writer) throws IOException {
		readerToWriter(reader,writer,true);
	}

	/**
	 * Copies the content of a reader to the buffer of a writer.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         Reader reader = new StringReader("test");
	 *         Writer writer = new StringWriter();
	 *         Misc.readerToWriter(reader, writer, true);
	 *         System.out.println(writer.toString)); // prints "test"
	 *     </pre>
	 * </p>
	 */
	public static void readerToWriter(Reader reader, Writer writer, boolean closeInput) throws IOException {
		if (reader!=null) {
			try {
				char[] buffer=new char[BUFFERSIZE];
				int charsRead;
				while ((charsRead=reader.read(buffer,0,BUFFERSIZE))>-1) {
					writer.write(buffer,0,charsRead);
				}
			} finally {
				if (closeInput) {
					reader.close();
				}
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
		try (FileReader reader = new FileReader(fileName)) {
			return readerToString(reader, endOfLineString, xmlEncode);
		}
	}

	/**
	 * Copies the content of a reader into a string, adds specified string to the end of the line, if specified.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         Reader r = new StringReader("<root> WeAreFrank'</root> \n";
	 *         String s = Misc.readerToString(r, "!!", true);
	 *         System.out.println(s);
	 *         // prints "&lt;root&gt; WeAreFrank!!&lt;/root&gt;"
	 *     </pre>
	 * </p>
	 * @param xmlEncode if set to true, applies XML encodings to the content of the reader
	 */
	public static String readerToString(Reader reader, String endOfLineString, boolean xmlEncode) throws IOException {
		StringBuilder sb = new StringBuilder();
		int curChar = -1;
		int prevChar = -1;
		try {
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
					sb.append(xmlEncode ? XmlUtils.encodeChars(appendStr) : appendStr);
				}
				prevChar = curChar;
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * @see #streamToString(InputStream, String, boolean)
	 * @return String that's included in the stream
	 */
	public static String streamToString(InputStream stream) throws IOException {
		return streamToString(stream, null, false);
	}

	/**
	 * @see #streamToString(InputStream, String, String, boolean)
	 */
	public static String streamToString(InputStream stream, String streamEncoding) throws IOException {
		return streamToString(stream, null, streamEncoding, false);
	}

	/**
	 * @see #streamToString(InputStream, String, String, boolean)
	 */
	public static String streamToString(InputStream stream, String endOfLineString, boolean xmlEncode) throws IOException {
		return streamToString(stream,endOfLineString, DEFAULT_INPUT_STREAM_ENCODING, xmlEncode);
	}

	/**
	 * @see #readerToString(Reader, String, boolean)
	 */
	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding, boolean xmlEncode) throws IOException {
		return readerToString(StreamUtil.getCharsetDetectingInputStreamReader(stream, streamEncoding), endOfLineString, xmlEncode);
	}

	/**
	 * @see #streamToString(InputStream, String, boolean)
	 */
	public static String resourceToString(URL resource, String endOfLineString, boolean xmlEncode) throws IOException {
		InputStream stream = resource.openStream();
		return streamToString(stream, endOfLineString, xmlEncode);
	}

	/**
	 * @see #streamToString(InputStream, String, boolean)
	 */
	public static String resourceToString(URL resource) throws IOException {
		return resourceToString(resource, null, false);
	}

	/**
	 * @see #streamToString(InputStream, String, boolean)
	 */
	public static String resourceToString(URL resource, String endOfLineString) throws IOException {
		return resourceToString(resource, endOfLineString, false);
	}

	/**
	 * Writes the string to a file.
	 */
	public static void stringToFile(String string, String fileName) throws IOException {
		try (FileWriter fw = new FileWriter(fileName)) {
			fw.write(string);
		}
	}

	/**
	 * String replacer.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String a = "WeAreFrank";
	 *         String res = Misc.replace(a, "WeAre", "IAm");
	 *         System.out.println(res); // prints "IAmFrank"
	 *     </pre>
	 * </p>
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

		StringBuilder buffer = new StringBuilder();
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

	/**
	 * Concatenates two strings, if specified, uses the separator in between two strings. 
	 * Does not use any seperators if both or one of the strings are empty.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String a = "We";
	 *         String b = "Frank";
	 *         String seperator = "Are";
	 *         String res = Misc.concatStrings(a, seperator, b);
	 *         System.out.println(res); // prints "WeAreFrank"
	 *     </pre>
	 * </p>
	 * @param part1 First string
	 * @param separator Specified separator
	 * @param part2 Second string
	 * @return the concatenated string
	 */
	public static String concatStrings(String part1, String separator, String part2) {
		if (StringUtils.isEmpty(part1)) {
			return part2;
		}
		if (StringUtils.isEmpty(part2)) {
			return part1;
		}
		return part1+separator+part2;
	}

	/**
	 * @see #hide(String)
	 * @return hidden string with all characters replaced with '*'
	 */
	public static String hide(String string) {
		return hide(string, 0);
	}

	/**
	 * Hides the string based on the mode given.
	 * Mode 1 hides starting from the second character of the string
	 * until, excluding, the last character.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String a = "test";
	 *         String res = Misc.hide(a, 1);
	 *         System.out.println(res) // prints "t**t"
	 *     </pre>
	 * </p>
	 */
	public static String hide(String string, int mode) {
		if (StringUtils.isEmpty(string)) {
			return string;
		}
		int len = string.length();
		if (mode == 1) {
			if (len <= 2) {
				return string;
			}
			char firstChar = string.charAt(0);
			char lastChar = string.charAt(len - 1);
			return firstChar + StringUtils.repeat("*", len - 2) + lastChar;
		} else {
			return StringUtils.repeat("*", len);
		}
	}

	/**
	 * Converts a byte array into a string, and adds a specified string to the end of the converted string.
	 * @see #streamToString(InputStream, String, boolean)
	 */
	public static String byteArrayToString(byte[] input, String endOfLineString, boolean xmlEncode) throws IOException{
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		return streamToString(bis, endOfLineString, xmlEncode);
	}

	/**
	 * Zips the input string with the default input stream encoding.
	 * @see #gzip(byte[])
	 */
	public static byte[] gzip(String input) throws IOException {
		return gzip(input.getBytes(DEFAULT_INPUT_STREAM_ENCODING));
	}

	/**
	 * Creates an expandable byte array to hold the compressed data.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String s = "test";
	 *         byte[] arr = s.getBytes();
	 *         byte[] zipped = Misc.gzip(arr);
	 *         System.out.println(Misc.gunzipToString(zipped)); // prints "test"
	 *     </pre>
	 * </p>
	 */
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

	/**
	 * Unzips a zipped byte array to a string
	 * @see #gunzip(byte[])
	 * @param input
	 */
	public static String gunzipToString(byte[] input) throws DataFormatException, IOException {
		return byteArrayToString(gunzip(input),"\n",false);
	}

	/**
	 * Unzips a zipped byte array to a string by create an expandable byte array to hold the decompressed data.
	 * Creates an expandable byte array to hold the compressed data.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String s = "test";
	 *         byte[] arr = s.getBytes();
	 *         byte[] zipped = Misc.gzip(arr);
	 *         System.out.println(Misc.gunzipToString(zipped)); // prints "test"
	 *     </pre>
	 * </p>
	 */
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

	/**
	 * Compresses the input string using the default stream encoding.
	 * @see #compress(byte[])
	 */
	public static byte[] compress(String input) throws IOException {
		return compress(input.getBytes(DEFAULT_INPUT_STREAM_ENCODING));
	}

	/**
	 *  Compresses the input string using the Deflater class of Java.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String s = "#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$#!@#$";
	 *         byte[] compressedSymbols = Misc.compress(s);
	 *         assertTrue(compressedSymbols.length < s1.length()); // will assertTrue as compressed one's length should be less than the normal string. However, this may not be the case for shorter strings.
	 *     </pre>
	 * </p>
	 */
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

	/**
	 * @see #decompress(byte[])
	 */
	public static String decompressToString(byte[] input) throws DataFormatException, IOException {
		return byteArrayToString(decompress(input),"\n",false);
	}

	/**
	 * Decompresses the compressed byte array.
	 * <p>
	 *     Example:
	 *     <pre>
	 *          String s = "test";
	 *         byte[] compressed = Misc.compress(s);
	 *         String decompressed = Misc.decompressToString(compressed);
	 *         System.out.print(decompressed); // prints "test"
	 *     </pre>
	 * </p>
	 */
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
			BufferedReader br;
			Process p;
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

	public static String getHostname() {
		String localHost=null;
		try {
			InetAddress localMachine = InetAddress.getLocalHost();
			localHost = localMachine.getHostName();
		} catch(UnknownHostException uhe) {
			localHost="unknown ("+uhe.getMessage()+")";
		}
		return localHost;
	}

	public static void copyContext(String keys, Map<String,Object> from, Map<String,Object> to) {
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
			if("WAS".equals(AppConstants.getInstance().getString("application.server.type", ""))) {
				throw new IOException(e);
			}
			log.debug("Caught NoClassDefFoundError for getApplicationDeploymentDescriptorPath, just not on Websphere Application Server: " + e.getMessage());
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
			log.debug("Caught NoClassDefFoundError for getConfigurationResources, just not on Websphere Application Server: " + e.getMessage());
			return null;
		}
	}

	public static String getConfigurationServer() throws IOException {
		try {
			return (String) Class.forName("nl.nn.adapterframework.util.IbmMisc").getMethod("getConfigurationServer").invoke(null);
		} catch (Exception e) {
			log.debug("Caught NoClassDefFoundError for getConfigurationServer, just not on Websphere Application Server: " + e.getMessage());
			return null;
		}
	}

	public static String getConnectionPoolProperties(String confResString,
	                                                 String providerType, String jndiName) {
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
			log.debug("Caught NoClassDefFoundError for getConnectionPoolProperties, just not on Websphere Application Server: "
					+ e.getMessage());
			return null;
		}
	}

	public static String getJmsDestinations(String confResString) {
		try {
			Class<?>[] args_types = new Class<?>[1];
			args_types[0] = String.class;
			Object[] args = new Object[1];
			args[0] = confResString;
			return (String) Class
					.forName("nl.nn.adapterframework.util.IbmMisc")
					.getMethod("getJmsDestinations", args_types)
					.invoke(null, args);
		} catch (Exception e) {
			log.debug("Caught NoClassDefFoundError for getJmsDestinations, just not on Websphere Application Server: "
					+ e.getMessage());
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
		if(s != null) {
			try {
				return Long.parseLong(s) * multiplier;
			}
			catch (NumberFormatException e) {
				log.error("[" + value + "] not in expected format", e);
			}
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
				} else {
					return "0 kB";
				}
			} else {
				return Long.toString(value) + (floor ? "B" : "");
			}
		} else {
			float f = (float) value / divider;
			return Math.round(f) + (format ? " " : "") + suffix;
		}
	}

	public static synchronized long getMessageSizeWarnByDefault() {
		if (messageSizeWarnByDefault == null) {
			String definitionString = AppConstants.getInstance().getString(MESSAGE_SIZE_WARN_BY_DEFAULT_KEY, null);
			long definition = toFileSize(definitionString, -1);
			messageSizeWarnByDefault = new Long(definition);
		}
		return messageSizeWarnByDefault.longValue();
	}

	public static synchronized long getResponseBodySizeWarnByDefault() {
		if (responseBodySizeWarnByDefault == null) {
			String definitionString = AppConstants.getInstance().getString(RESPONSE_BODY_SIZE_WARN_BY_DEFAULT_KEY,
					null);
			long definition = toFileSize(definitionString, -1);
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

	/**
	 * Converts the list to a string.
	 * <pre>
	 *      List list = new ArrayList<Integer>();
	 *      list.add("We Are");
	 *      list.add(" Frank");
	 *      String res = Misc.listToString(list); // res gives out "We Are Frank"
	 * </pre>
	 */
	public static String listToString(List list) {
		StringBuilder sb = new StringBuilder();
		for (Iterator it=list.iterator(); it.hasNext();) {
			sb.append((String) it.next());
		}
		return sb.toString();
	}

	/**
	 * Adds items on a string, added by comma seperator (ex: "1,2,3"), into a list.
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

	public static String getTotalTransactionLifetimeTimeout() throws IOException, SAXException, TransformerException {
		String confSrvString = getConfigurationServer();
		if (confSrvString==null) {
			return null;
		}
		return getTotalTransactionLifetimeTimeout(confSrvString);
	}

	public static String getTotalTransactionLifetimeTimeout(String configServerXml) throws IOException, SAXException, TransformerException {
		if (configServerXml==null) {
			return null;
		}
		String confSrvString = XmlUtils.removeNamespaces(configServerXml);
		confSrvString = XmlUtils.removeNamespaces(confSrvString);
		String xPath = "Server/components/services/@totalTranLifetimeTimeout";
		TransformerPool tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(xPath));
		return tp.transform(confSrvString, null);
	}

	public static String getMaximumTransactionTimeout() throws IOException, SAXException, TransformerException {
		String confSrvString = getConfigurationServer();
		if (confSrvString==null) {
			return null;
		}
		return getMaximumTransactionTimeout(confSrvString);
	}

	public static String getMaximumTransactionTimeout(String configServerXml) throws IOException, SAXException, TransformerException {
		if (configServerXml==null) {
			return null;
		}
		String confSrvString = XmlUtils.removeNamespaces(configServerXml);
		confSrvString = XmlUtils.removeNamespaces(confSrvString);
		String xPath = "Server/components/services/@propogatedOrBMTTranLifetimeTimeout";
		TransformerPool tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(xPath));
		return tp.transform(confSrvString, null);
	}

	public static String getSystemTransactionTimeout() {
		String confSrvString = null;
		try {
			confSrvString = getConfigurationServer();
		} catch (Exception e) {
			log.warn("Exception getting configurationServer",e);
		}
		if (confSrvString==null) {
			return null;
		}
		String totalTransactionLifetimeTimeout = null;
		String maximumTransactionTimeout = null;
		try {
			totalTransactionLifetimeTimeout = Misc.getTotalTransactionLifetimeTimeout(confSrvString);
		} catch (Exception e) {
			log.warn("Exception getting totalTransactionLifetimeTimeout",e);
		}
		try {
			maximumTransactionTimeout = Misc.getMaximumTransactionTimeout(confSrvString);
		} catch (Exception e) {
			log.warn("Exception getting maximumTransactionTimeout",e);
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
		if (s != null) {
			try {
				return Long.parseLong(s) * multiplier;
			} catch (NumberFormatException e) {
				log.error("[" + value + "] not in expected format", e);
			}
		}
		return defaultValue;
	}

	/**
	 * Edits the input string according to the regex and the hide method specified.
	 * @see #hideFirstHalf(String, String)
	 * @see #hideAll(String, String)
	 */
	public static String cleanseMessage(String inputString, String regexForHiding, String hideMethod) {
		if (StringUtils.isEmpty(regexForHiding)) {
			return inputString;
		}
		if ("firstHalf".equalsIgnoreCase(hideMethod)) {
			return hideFirstHalf(inputString, regexForHiding);
		} else {
			return hideAll(inputString, regexForHiding);
		}
	}

	/**
	 * Hides the first half of the string.
	 * @see #hideAll(String, String, int)
	 */
	public static String hideFirstHalf(String inputString, String regex) {
		return hideAll(inputString, regex, 1);
	}

	/**
	 * Hide all characters matching the given Regular Expression.
	 * If the set of expressions is null or empty it will return the raw message.
	 * @see #hideAll(String, Collection, int)
	 */
	public static String hideAll(String message, Collection<String> collection) {
		return hideAll(message, collection, 0);
	}

	/**
	 * Hide all characters matching the given Regular Expression.
	 * If the set of expressions is null or empty it will return the raw message
	 * @see #hideAll(String, String, int)
	 */
	public static String hideAll(String message, Collection<String> collection, int mode) {
		if(collection == null || collection.isEmpty() || StringUtils.isEmpty(message))
			return message; //Nothing to do!

		for (String regex : collection) {
			if (StringUtils.isNotEmpty(regex))
				message = hideAll(message, regex, mode);
		}
		return message;
	}

	/**
	 * @see #hideAll(String, String, int)
	 */
	public static String hideAll(String inputString, String regex) {
		return hideAll(inputString, regex, 0);
	}

	/**
	 * Hides the input string according to the given regex and mode.
	 * If mode is set to 1, then the first half of the string gets hidden.
	 * Else, all of it.
	 */
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

	public static String getBuildOutputDirectory() {
		String path = new File(
				AppConstants.class.getClassLoader().getResource("").getPath())
				.getPath();
		try {
			return URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warn("Error decoding path [" + path + "]", e);
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
				} else {
					dir = dir.getParentFile();
				}
			}
		}
		return null;
	}

	/**
	 * Replaces low line (x'5f') by asterisk (x'2a) so it's sorted before any digit and letter
	 * <pre>
	 *      Misc.toSortName("new_name"); // gives out "NEW*NAME"
	 * </pre>
	 */
	public static String toSortName(String name) {
		// replace low line (x'5f') by asterisk (x'2a) so it's sorted before any digit and letter
		return StringUtils.upperCase(StringUtils.replace(name,"_", "*"));
	}

	/**
	 * Counts the number of characters that the specified reges will affect in the specified string.
	 * <pre>
	 *     String s = "12ab34";
	 *     String regex = "\\d";
	 *     int regexCount = Misc.countRegex(s, regex); // regexCount gives out 4
	 * </pre>
	 */
	public static int countRegex(String string, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(string);
		int count = 0;
		while (matcher.find())
			count++;
		return count;
	}
}
