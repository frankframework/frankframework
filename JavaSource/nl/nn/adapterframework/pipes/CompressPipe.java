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
package nl.nn.adapterframework.pipes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe to zip or unzip a message or file.  
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.BatchFileTransformerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageIsContent(boolean) messageIsContent}</td><td>Flag indicates whether the message is the content or the path to a file with the contents. For multiple files use ';' as delimiter</td><td>false</td></tr>
 * <tr><td>{@link #setResultIsContent(boolean) resultIsContent}</td><td>Flag indicates whether the result must be written to the message or to a file (filename = message)</td><td>false</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Required if result is a file, the directory in which to store the result file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilenamePattern(String) filenamePattern}</td><td>Required if result is a file, the pattern for the result filename</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setZipEntryPattern(String) zipEntryPattern}</td><td>The pattern for the zipentry name in case a zipfile is read or written</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCompress(boolean) compress}</td><td>If <code>true</code> the pipe compresses, otherwise it decompress</td><td>false</td></tr>
 * <tr><td>{@link #setConvert2String(boolean) convert2String}</td><td>If <code>true</code> result is returned as a string, otherwise as a byte array</td><td>false</td></tr>
 * <tr><td>{@link #setFileFormat(String) fileFormat}</td><td>When set to gz, the GZIP format is used. When set to another value, the ZIP format is used. If not set and direction is compress, the resultIsContent specifies the output format used (resultIsContent="true" -> GZIP format, resultIsContent="false" -> ZIP format) If not set and direction is decompress, the messageIsContent specifies the output format used (messageIsContent="true" -> GZIP format, messageIsContent="false" -> ZIP format)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>When no problems encountered</td></tr>
 * <tr><td>"exception"</td><td>When problems encountered. The result passed to the next pipe is the exception that was caught formatted by the ErrorMessageFormatter class.</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 * @author: Jaco de Groot (***@dynasol.nl)
 */
public class CompressPipe extends FixedForwardPipe {

	private final static String EXCEPTIONFORWARD = "exception";

	private boolean messageIsContent;
	private boolean resultIsContent;
	private String outputDirectory;
	private String filenamePattern;
	private String zipEntryPattern;
	private boolean compress;
	private boolean convert2String;
	private String fileFormat;
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			Object result;
			InputStream in;
			OutputStream out;
			boolean zipMultipleFiles = false;
			if (messageIsContent) {
				if (input instanceof byte[]) {
					in = new ByteArrayInputStream((byte[])input); 
				} else {
					in = new ByteArrayInputStream(input.toString().getBytes()); 
				}
			} else {
				if (compress && StringUtils.contains((String)input,";")) {
					zipMultipleFiles = true;
					in = null;
				} else {
					in = new FileInputStream((String)input);
				}
			}
			if (resultIsContent) {
				out = new ByteArrayOutputStream();
				result = out; 
			} else {
				String outFilename = null;
				if (messageIsContent) {
					outFilename = FileUtils.getFilename(getParameterList(), session, (File)null, filenamePattern);
				} else {
					outFilename = FileUtils.getFilename(getParameterList(), session, new File((String)input), filenamePattern);
				}
				File outFile = new File(outputDirectory, outFilename);
				result = outFile.getAbsolutePath();
				out =  new FileOutputStream(outFile);
			}
			if (zipMultipleFiles) {
				ZipOutputStream zipper = new ZipOutputStream(out); 
				StringTokenizer st = new StringTokenizer((String)input, ";");
				while (st.hasMoreElements()) {
					String fn = st.nextToken();
					String zipEntryName = getZipEntryName(fn, session);
					zipper.putNextEntry(new ZipEntry(zipEntryName));
					in  = new FileInputStream(fn);
					try {
						int readLength = 0;
						byte[] block = new byte[4096];
						while ((readLength = in.read(block)) > 0) {
							 zipper.write(block, 0, readLength);
						}
					} finally {
						in.close();
						zipper.closeEntry();
					}
				}
				zipper.close();
				out = zipper;
			} else {
				if (compress) {
					if ("gz".equals(fileFormat) || fileFormat == null && resultIsContent) {
						out = new GZIPOutputStream(out);
					} else {
						ZipOutputStream zipper = new ZipOutputStream(out); 
						String zipEntryName = getZipEntryName(input, session);
						zipper.putNextEntry(new ZipEntry(zipEntryName));
						out = zipper;
					}
				} else {
					if ("gz".equals(fileFormat) || fileFormat == null && messageIsContent) {
						in = new GZIPInputStream(in);
					} else {
						ZipInputStream zipper = new ZipInputStream(in);
						String zipEntryName = getZipEntryName(input, session);
						if (zipEntryName.equals("")) {
							// Use first entry found
							zipper.getNextEntry();
						} else {
							// Position the stream at the specified entry
							ZipEntry zipEntry = zipper.getNextEntry();
							while (zipEntry != null && !zipEntry.getName().equals(zipEntryName)) {
								zipEntry = zipper.getNextEntry();
							}
						}
						in = zipper;
					}
				}
				try {
					int readLength = 0;
					byte[] block = new byte[4096];
					while ((readLength = in.read(block)) > 0) {
						 out.write(block, 0, readLength);
					}
				} finally {
					out.close();
					in.close();
				}
			}
			return new PipeRunResult(getForward(), getResultMsg(result));
		} catch(Exception e) {
			PipeForward exceptionForward = findForward(EXCEPTIONFORWARD);
			if (exceptionForward!=null) {
				log.warn(getLogPrefix(session) + "exception occured, forwarded to ["+exceptionForward.getPath()+"]", e);
				String originalMessage;
				if (input instanceof String) {
					originalMessage = (String)input; 
				} else {
					originalMessage = "Object of type " + input.getClass().getName(); 
				}
				String resultmsg=new ErrorMessageFormatter().format(getLogPrefix(session),e,this,originalMessage,session.getMessageId(),0);
				return new PipeRunResult(exceptionForward,resultmsg);
			}
			throw new PipeRunException(this, "Unexpected exception during compression", e);
		}
	}
	
	private Object getResultMsg(Object result) {
		if (resultIsContent) {
			if (convert2String)
				return ((ByteArrayOutputStream)result).toString();
			return ((ByteArrayOutputStream)result).toByteArray();
		}
		return result;
	}
	
	private String getZipEntryName(Object input, IPipeLineSession session) throws ParameterException {
		if (messageIsContent) {
			return FileUtils.getFilename(getParameterList(), session, (File)null, zipEntryPattern);
		}
		return FileUtils.getFilename(getParameterList(), session, new File((String)input), zipEntryPattern);
	}

	public boolean isCompress() {
		return compress;
	}

	public String getFilenamePattern() {
		return filenamePattern;
	}

	public boolean isMessageIsContent() {
		return messageIsContent;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public boolean isResultIsContent() {
		return resultIsContent;
	}

	public void setCompress(boolean b) {
		compress = b;
	}

	public void setFilenamePattern(String string) {
		filenamePattern = string;
	}

	public void setMessageIsContent(boolean b) {
		messageIsContent = b;
	}

	public void setOutputDirectory(String string) {
		outputDirectory = string;
	}

	public void setResultIsContent(boolean b) {
		resultIsContent = b;
	}

	public String getZipEntryPattern() {
		return zipEntryPattern;
	}

	public void setZipEntryPattern(String string) {
		zipEntryPattern = string;
	}

	public boolean isConvert2String() {
		return convert2String;
	}

	public void setConvert2String(boolean b) {
		convert2String = b;
	}

	public void setFileFormat(String string) {
		fileFormat = string;
	}

}
