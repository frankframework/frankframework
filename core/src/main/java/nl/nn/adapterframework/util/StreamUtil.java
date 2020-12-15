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
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.function.Function;
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
	
	public static InputStream dontClose(InputStream stream) {
		class NonClosingInputStreamFilter extends FilterInputStream {
			public NonClosingInputStreamFilter(InputStream in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				// do not close
			}
		};
		
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
		};
		
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

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding) throws IOException {
		return readerToString(StreamUtil.getCharsetDetectingInputStreamReader(stream,streamEncoding), endOfLineString);
	}

	public static byte[] streamToByteArray(InputStream inputStream, boolean skipBOM) throws IOException {
		BOMInputStream bOMInputStream = new BOMInputStream(inputStream, !skipBOM, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
		return Misc.streamToBytes(bOMInputStream);
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

	public static InputStream closeOnClose(InputStream stream, AutoCloseable resource) {
		class ResourceClosingInputStreamFilter extends FilterInputStream {
			public ResourceClosingInputStreamFilter(InputStream in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				try (AutoCloseable closeable = resource) {
					super.close();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		};

		return new ResourceClosingInputStreamFilter(stream);
	}

	public static InputStream watch(InputStream stream, Runnable onClose, Runnable onException) {
		return watch(stream, onClose, (e) -> { if (onException!=null) onException.run(); return e; }); 
	}
	
	public static InputStream watch(InputStream stream, Runnable onClose, Function<IOException,IOException> onException) {
		class WatchedInputStream extends FilterInputStream {
			public WatchedInputStream(InputStream in) {
				super(in);
			}
			
			private IOException handleException(IOException e) {
				if (onException!=null) {
					IOException r = onException.apply(e);
					if (r!=null) {
						return r;
					}
				}
				return e;
			}
			
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} catch (IOException e) {
					throw handleException(e);
				}
				if (onClose!=null) {
					onClose.run();
				}
			}

			@Override
			public int read() throws IOException {
				try {
					return super.read();
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public int read(byte[] b) throws IOException {
				try {
					return super.read(b);
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				try {
					return super.read(b, off, len);
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public long skip(long n) throws IOException {
				try {
					return super.skip(n);
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public int available() throws IOException {
				try {
					return super.available();
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public synchronized void reset() throws IOException {
				try {
					super.reset();
				} catch (IOException e) {
					throw handleException(e);
				}
			}

		};

		return new WatchedInputStream(stream);
	}


	public static Reader closeOnClose(Reader reader, AutoCloseable resource) {
		class ResourceClosingReaderFilter extends FilterReader {
			public ResourceClosingReaderFilter(Reader in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				try (AutoCloseable closeable = resource) {
					super.close();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		};

		return new ResourceClosingReaderFilter(reader);
	}
	
	public static OutputStream closeOnClose(OutputStream stream, AutoCloseable resource) {
		class ResourceClosingOutputStreamFilter extends FilterOutputStream {
			public ResourceClosingOutputStreamFilter(OutputStream out) {
				super(out);
			}
			@Override
			public void close() throws IOException {
				try (AutoCloseable closeable = resource) {
					super.close();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		};

		return new ResourceClosingOutputStreamFilter(stream);
	}

}
