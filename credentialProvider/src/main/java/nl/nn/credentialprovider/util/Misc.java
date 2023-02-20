/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package nl.nn.credentialprovider.util;

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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	private static Logger log = Logger.getLogger(Misc.class.getName());

	public static final int BUFFERSIZE=20000;
	@Deprecated
	public static final String DEFAULT_INPUT_STREAM_ENCODING="UTF-8";

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	public static final String LINE_SEPARATOR = System.lineSeparator();

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
		s.append(ia).append(System.currentTimeMillis()).append(hash);

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

	public static void streamToStream(InputStream input, OutputStream output) throws IOException {
		streamToStream(input, output, null);
	}

	/**
	 * Writes the content of an input stream to an output stream by copying the buffer of input stream to the buffer of the output stream.
	 * If eof is specified, appends the eof(could represent a new line) to the outputstream
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
	 * @throws IOException  exception to be thrown if an I/O exception occurs
	 */
	public static void streamToStream(InputStream input, OutputStream output, byte[] eof) throws IOException {
		if (input!=null) {
			try {
				byte[] buffer=new byte[BUFFERSIZE];
				int bytesRead;
				while ((bytesRead=input.read(buffer,0,BUFFERSIZE))>-1) {
					output.write(buffer,0,bytesRead);
				}
				if(eof != null) {
					output.write(eof);
				}
			} finally {
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
		try {
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
		} finally {
			inputStream.close();
		}
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
	public static void readerToWriter(Reader reader, Writer writer) throws IOException {
		if (reader!=null) {
			try {
				char[] buffer=new char[BUFFERSIZE];
				int charsRead;
				while ((charsRead=reader.read(buffer,0,BUFFERSIZE))>-1) {
					writer.write(buffer,0,charsRead);
				}
			} finally {
				reader.close();
			}
		}
	}

	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	public static String fileToString(String fileName) throws IOException {
		return fileToString(fileName, null);
	}
	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	public static String fileToString(String fileName, String endOfLineString) throws IOException {
		try (FileReader reader = new FileReader(fileName)) {
			return readerToString(reader, endOfLineString);
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
	 */
	public static String readerToString(Reader reader, String endOfLineString) throws IOException {
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
					sb.append(appendStr);
				}
				prevChar = curChar;
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	public static String streamToString(InputStream stream) throws IOException {
		return streamToString(stream, DEFAULT_INPUT_STREAM_ENCODING);
	}

	public static String streamToString(InputStream stream, String streamEncoding) throws IOException {
		return streamToString(stream, null, streamEncoding);
	}

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding) throws IOException {
		return readerToString(new InputStreamReader(stream, streamEncoding), endOfLineString);
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
	 * Does not use any separators if both or one of the strings are empty.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String a = "We";
	 *         String b = "Frank";
	 *         String separator = "Are";
	 *         String res = Misc.concatStrings(a, separator, b);
	 *         System.out.println(res); // prints "WeAreFrank"
	 *     </pre>
	 * </p>
	 * @param part1 First string
	 * @param separator Specified separator
	 * @param part2 Second string
	 * @return the concatenated string
	 */
	public static String concatStrings(String part1, String separator, String part2) {
		return concat(separator, part1, part2);
	}

	public static String concat(String separator, String... parts) {
		int i=0;
		while(i<parts.length && isEmpty(parts[i])) {
			i++;
		}
		if (i>=parts.length) {
			return null;
		}
		String result=parts[i];
		while(++i<parts.length) {
			if (isNotEmpty(parts[i])) {
				result += separator + parts[i];
			}
		}
		return result;
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
			Method getenvs = System.class.getMethod( "getenv", (Class[]) null );
			Map env = (Map) getenvs.invoke( null, (Object[]) null );
			for (Iterator it = env.keySet().iterator(); it.hasNext();) {
				String key = (String)it.next();
				String value = (String)env.get(key);
				props.setProperty(key,value);
			}
		} catch ( NoSuchMethodException e ) {
			log.fine("Caught NoSuchMethodException, just not on JDK 1.5: "+e.getMessage());
		} catch ( IllegalAccessException e ) {
			log.log(Level.FINE, "Caught IllegalAccessException, using JDK 1.4 method",e);
		} catch ( InvocationTargetException e ) {
			log.log(Level.FINE, "Caught InvocationTargetException, using JDK 1.4 method",e);
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
		if(s != null) {
			try {
				return Long.parseLong(s) * multiplier;
			}
			catch (NumberFormatException e) {
				log.log(Level.SEVERE, "[" + value + "] not in expected format", e);
			}
		}
		return defaultValue;
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
			log.fine("adding item to "+collectionDescription+" ["+item+"]");
			collection.add(item);
		}
		if (list.trim().endsWith(",")) {
			log.fine("adding item to "+collectionDescription+" <empty string>");
			collection.add("");
		}
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
				log.log(Level.SEVERE, "[" + value + "] not in expected format", e);
			}
		}
		return defaultValue;
	}




	public static String getBuildOutputDirectory() {
		String path = new File(AppConstants.class.getClassLoader().getResource("").getPath()).getPath();
		return urlDecode(path);
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

	public static String urlDecode(String input) {
		try {
			return URLDecoder.decode(input,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.warning(e.getMessage());
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

	public static boolean isEmpty(String string) {
		return string==null || string.isEmpty();
	}

	public static boolean isNotEmpty(String string) {
		return string!=null && !string.isEmpty();
	}

}
