/*
 * $Log: StreamUtil.java,v $
 * Revision 1.1  2007-07-17 11:03:40  europe\L190409
 * first version, copied from SRP
 *
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Functions to read and write from one stream to another.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class StreamUtil {
	public static final String DEFAULT_INPUT_STREAM_ENCODING="UTF-8";
	
	public static String readerToString(Reader reader, String endOfLineString) throws IOException {
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
				sb.append(appendStr);
			}
			prevChar = curChar;
		}
		return sb.toString();
	}

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding)
		throws IOException {
		return readerToString(new InputStreamReader(stream,streamEncoding), endOfLineString);
	}

	public static byte[] streamToByteArray(InputStream servletinputstream, int contentLength) throws IOException {
		byte[] result=null;
		if(contentLength > 0) {
			result = new byte[contentLength];
			int position = 0;
			do {
				int bytesRead = servletinputstream.read(result, position, contentLength - position);
				if(bytesRead <= 0) {
					throw new IOException("post body contains less bytes ["+position+"] than specified by content-length ["+contentLength+"]");
				}
				position += bytesRead;
			} while(contentLength - position > 0);
		}
		return result;
	}

	public static void copyStream(InputStream in, OutputStream out, int chunkSize) throws IOException {
		if (in!=null) {
			byte buffer[]=new byte[chunkSize]; 
					
			int bytesRead=1;
			while (bytesRead>0) {
				bytesRead=in.read(buffer,0,chunkSize);
				if (bytesRead>0) {
//					if (log.isDebugEnabled()) { log.debug(new String(buffer).substring(0,bytesRead)); }
					out.write(buffer,0,bytesRead);
				} else {
					in.close();
				}
			}
		}
	}

	public static void copyReaderToWriter(Reader reader, Writer writer, int chunkSize, boolean resolve, boolean xmlEncode) throws IOException {
		if (reader!=null) {
			char buffer[]=new char[chunkSize]; 
					
			int charsRead=1;
			while (charsRead>0) {
				charsRead=reader.read(buffer,0,chunkSize);
				if (charsRead>0) {
//					if (log.isDebugEnabled()) { log.debug(new String(buffer).substring(0,bytesRead)); }
					if (resolve) {
						String resolved = StringResolver.substVars(new String (buffer,0,charsRead),null);
						if (xmlEncode) {
							writer.write(XmlUtils.encodeChars(resolved));
						} else {
							writer.write(resolved);
						}
					} else {
						if (xmlEncode) {
							writer.write(XmlUtils.encodeChars(buffer,0,charsRead));
						} else {
							writer.write(buffer,0,charsRead);
						}
					}
				} else {
					reader.close();
				}
			}
		}
	}


}
