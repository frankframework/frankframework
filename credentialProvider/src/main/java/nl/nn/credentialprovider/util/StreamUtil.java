/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Logger;


/**
 * Functions to read and write from one stream to another.
 *
 * @author  Gerrit van Brakel
 */
public class StreamUtil {

	public static final String DEFAULT_INPUT_STREAM_ENCODING="UTF-8";
	public static final int DEFAULT_STREAM_CAPTURE_LIMIT=10000;

	// DEFAULT_CHARSET and DEFAULT_INPUT_STREAM_ENCODING must be defined before LogUtil.getLogger() is called, otherwise DEFAULT_CHARSET returns null.
	protected static Logger log = Logger.getLogger(StreamUtil.class.getName());

	public static OutputStream getOutputStream(Object target) throws IOException {
		if (target instanceof OutputStream) {
			return (OutputStream) target;
		}
		if (target instanceof String) {
			String filename=(String)target;
			if (StringUtil.isEmpty(filename)) {
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

//	public static Writer getWriter(Object target) throws IOException {
//		if (target instanceof HttpServletResponse) {
//			return ((HttpServletResponse)target).getWriter();
//		}
//		if (target instanceof Writer) {
//			return (Writer)target;
//		}
//		return null;
//	}

	public static InputStream dontClose(InputStream stream) {
		class NonClosingInputStreamFilter extends FilterInputStream {
			public NonClosingInputStreamFilter(InputStream in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				// do not close
			}
		}

		return new NonClosingInputStreamFilter(stream);
	}

	public static Reader dontClose(Reader reader) {
		class NonClosingReaderFilter extends FilterReader {
			public NonClosingReaderFilter(Reader in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				// do not close
			}
		}

		return new NonClosingReaderFilter(reader);
	}

	public static String readerToString(Reader reader, String endOfLineString) throws IOException {
		try {
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
		} finally {
			reader.close();
		}
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

	public static void copyReaderToWriter(Reader reader, Writer writer, int chunkSize, boolean resolve) throws IOException {
		if (reader!=null) {
			char buffer[]=new char[chunkSize];

			int charsRead=1;
			while (charsRead>0) {
				charsRead=reader.read(buffer,0,chunkSize);
				if (charsRead>0) {
//					if (log.isDebugEnabled()) { log.debug(new String(buffer).substring(0,bytesRead)); }
					if (resolve) {
						String resolved = StringResolver.substVars(new String (buffer,0,charsRead),null);
//						if (xmlEncode) {
//							writer.write(XmlUtils.encodeChars(resolved));
//						} else {
							writer.write(resolved);
//						}
					} else {
//						if (xmlEncode) {
//							writer.write(XmlUtils.encodeChars(buffer,0,charsRead));
//						} else {
							writer.write(buffer,0,charsRead);
//						}
					}
				} else {
					reader.close();
				}
			}
		}
	}
}
