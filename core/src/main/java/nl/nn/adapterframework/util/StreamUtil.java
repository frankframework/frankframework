/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;

/**
 * Functions to read and write from one stream to another.
 * 
 * @author  Gerrit van Brakel
 */
public class StreamUtil {
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
	public static final String DEFAULT_INPUT_STREAM_ENCODING=DEFAULT_CHARSET.displayName();
	
	protected static final byte[] BOM_UTF_8 = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
	
	public static OutputStream getOutputStream(Object target) throws IOException {
		if (target instanceof OutputStream) {
			return (OutputStream) target;
		} 
		if (target instanceof String) {
			String filename=(String)target;
			if (StringUtils.isEmpty(filename)) {
				throw new IOException("target string cannot be empty but must contain a filename");
			}
			try {
				return new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				FileNotFoundException fnfe = new FileNotFoundException("cannot create file ["+filename+"]");
				fnfe.initCause(e);
				throw fnfe;					
			}
		}
		return null;
	}

	public static Writer getWriter(Object target) throws IOException {
		if (target instanceof HttpServletResponse) {
			return ((HttpServletResponse)target).getWriter();
		}
		if (target instanceof Writer) {
			return (Writer)target;
		}
		return null;
	}
	
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

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding) throws IOException {
		return readerToString(StreamUtil.getCharsetDetectingInputStreamReader(stream,streamEncoding), endOfLineString);
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

	public static byte[] streamToByteArray(InputStream inputStream, boolean skipBOM) throws IOException {
		byte[] result = Misc.streamToBytes(inputStream);
		if (skipBOM) {
			//log.debug("checking BOM");
			if ((result[0] == BOM_UTF_8[0]) && (result[1] == BOM_UTF_8[1]) && (result[2] == BOM_UTF_8[2])) {
			    byte[] resultWithoutBOM = new byte[result.length-3];
			    for(int i = 3; i < result.length; ++i)
			    	resultWithoutBOM[i-3]=result[i];
			    //log.debug("removed UTF-8 BOM");
			    return resultWithoutBOM;
			}
		}
		//log.debug("no UTF-8 BOM found");
		return result;
	}
	
	/**
	 * Return a Reader that reads the InputStream in the character set specified by the BOM. If no BOM is found, the default character set UTF-8 is used.
	 */
	public static Reader getCharsetDetectingInputStreamReader(InputStream inputStream) throws IOException {
		return getCharsetDetectingInputStreamReader(inputStream, DEFAULT_INPUT_STREAM_ENCODING);
	}
	
	/**
	 * Return a Reader that reads the InputStream in the character set specified by the BOM. If no BOM is found, a default character set is used.
	 */
	public static Reader getCharsetDetectingInputStreamReader(InputStream inputStream, String defaultCharset) throws IOException {
		BOMInputStream bOMInputStream = new BOMInputStream(inputStream,ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
		ByteOrderMark bom = bOMInputStream.getBOM();
		String charsetName = bom == null ? defaultCharset : bom.getCharsetName();

		if(StringUtils.isEmpty(charsetName)) {
			charsetName = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}

		return new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);
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

	public static ZipOutputStream openZipDownload(HttpServletResponse response, String filename) throws IOException {
		OutputStream out = response.getOutputStream();
		response.setContentType("application/x-zip-compressed");
		response.setHeader("Content-Disposition","attachment; filename=\""+filename+"\"");
		ZipOutputStream zipOutputStream = new ZipOutputStream(out);
		return zipOutputStream;
	}

}
