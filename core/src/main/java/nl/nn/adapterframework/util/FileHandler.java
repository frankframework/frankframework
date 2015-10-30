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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * FileHandler, available to the Ibis developer as {@link nl.nn.adapterframework.senders.FileSender} and
 * {@link nl.nn.adapterframework.pipes.FilePipe}, allows to write to or read from a file.
 * Write will create a file in the specified directory. If a directory is not
 * specified, the fileName is expected to include the directory. If both the
 * fileName and the directory are not specified a temporary file is created as
 * specified by the {@link java.io.File.createTempFile} method using the string "ibis"
 * as a prefix and a suffix as specified bij the writeSuffix attribute. If only
 * the directory is specified, the temporary file is created the same way except
 * that the temporay file is created in the specified directory.
 * The pipe also support base64 en- and decoding.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>The charset to be used when transforming a string to a byte array and/or the other way around</td><td>The value of the system property file.encoding</td></tr>
 * <tr><td>{@link #setDirectory(String) directory}</td><td>base directory where files are stored in or read from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td><td>The name of the file to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileNameSessionKey(String) fileNameSessionKey}</td><td>The session key that contains the name of the file to use (only used if fileName is not set)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setActions(String) actions}</td><td>comma separated list of actions to be performed. Possible action values:
 * <ul>
 * <li>write: create a new file and write input to it</li>
 * <li>write_append: create a new file if it does not exist, otherwise append to existing file; then write input to it</li>
 * <li>create: create a new file, but do not write anything to it</li>
 * <li>read: read from file</li>
 * <li>delete: delete the file</li>
 * <li>read_delete: read the contents, then delete (when outputType is stream the file is deleted after the stream is read)</li>
 * <li>encode: encode base64</li>
 * <li>decode: decode base64</li>
 * <li>list: returns the files and directories in the directory that satisfy the specified filter (see {@link nl.nn.adapterframework.util.Dir2Xml dir2xml}). If a directory is not specified, the fileName is expected to include the directory</li>
 * <li>info: returns information about the file</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWriteSuffix(String) writeSuffix}</td><td>suffix of the file to be created (only used if fileName and fileNameSession are not set)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCreateDirectory(boolean) createDirectory}</td><td>when set to <code>true</code>, the directory to read from or write to is created if it does not exist</td><td>false</td></tr>
 * <tr><td>{@link #setWriteLineSeparator(boolean) writeLineSeparator}</td><td>when set to <code>true</code>, a line separator is written after the content is written</td><td>false</td></tr>
 * <tr><td>{@link #setTestCanWrite(boolean) testCanWrite}</td><td>when set to <code>true</code>, a test is performed to find out if a temporary file can be created and deleted in the specified directory (only used if directory is set and combined with the action write, write_append or create)</td><td>true</td></tr>
 * <tr><td>{@link #setSkipBOM(boolean) skipBOM}</td><td>when set to <code>true</code>, a possible Bytes Order Mark (BOM) at the start of the file is skipped (only used for the action read and encoding UFT-8)</td><td>false</td></tr>
 * <tr><td>{@link #setDeleteEmptyDirectory(boolean) deleteEmptyDirectory}</td><td>(only used when actions=delete) when set to <code>true</code>, the directory from which a file is deleted is also deleted when it contains no other files</td><td>false</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either <code>string</code>, <code>bytes</code> or <code>stream</code></td><td>"string"</td></tr>
 * <tr><td>{@link #setFileSource(String) fileSource}</td><td>(action=read) either <code>filesystem</code> or <code>classpath</code></td><td>"filesystem"</td></tr>
 * <tr><td>{@link #setStreamResultToServlet(boolean) streamResultToServlet}</td><td>(only used when outputType=stream) if set, the result is streamed to the HttpServletResponse object which is stored in session key "restListenerServletResponse"</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td></td>writeSuffix<td><i>String</i></td><td>When a parameter with name writeSuffix is present, it is used instead of the writeSuffix specified by the attribute</td></tr>
 * </table>
 * </p>
 * 
 * @author J. Dekker
 * @author Jaco de Groot (***@dynasol.nl)
 *
 */
public class FileHandler {
	protected Logger log = LogUtil.getLogger(this);

	protected static final byte[] BOM_UTF_8 = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
	
	protected String charset = System.getProperty("file.encoding");
	protected String outputType = "string";
	protected String fileSource = "filesystem";
	protected String actions;
	protected String directory;
	protected String writeSuffix;
	protected String fileName;
	protected String fileNameSessionKey;
	protected boolean createDirectory = false;
	protected boolean writeLineSeparator = false;
	protected boolean testCanWrite = true;
	protected boolean skipBOM = false;
	protected boolean deleteEmptyDirectory = false;
	protected boolean streamResultToServlet=false;

	protected List transformers;
	protected byte[] eolArray=null;
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		// translation action seperated string to Transformers
		transformers = new LinkedList();
		if (StringUtils.isEmpty(actions))
			throw new ConfigurationException(getLogPrefix(null)+"should at least define one action");
			
		StringTokenizer tok = new StringTokenizer(actions, " ,\t\n\r\f");
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			
			if ("write".equalsIgnoreCase(token))
				transformers.add(new FileWriter(false));
			else if ("write_append".equalsIgnoreCase(token))
				transformers.add(new FileWriter(true));
			else if ("create".equalsIgnoreCase(token))
				transformers.add(new FileCreater());
			else if ("read".equalsIgnoreCase(token))
				transformers.add(new FileReader());
			else if ("delete".equalsIgnoreCase(token))
				transformers.add(new FileDeleter());
			else if ("read_delete".equalsIgnoreCase(token))
				transformers.add(new FileReader(true));
			else if ("encode".equalsIgnoreCase(token))
				transformers.add(new Encoder());
			else if ("decode".equalsIgnoreCase(token))
				transformers.add(new Decoder());
			else if ("list".equalsIgnoreCase(token))
				transformers.add(new FileLister());
			else if ("info".equalsIgnoreCase(token))
				transformers.add(new FileInfoProvider());
			else
				throw new ConfigurationException(getLogPrefix(null)+"Action [" + token + "] is not supported");
		}
		
		if (transformers.size() == 0)
			throw new ConfigurationException(getLogPrefix(null)+"should at least define one action");
		if (!outputType.equalsIgnoreCase("string")
				&& !outputType.equalsIgnoreCase("bytes")
				&& !outputType.equalsIgnoreCase("stream")) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for outputType ["+outputType+"], must be 'string', 'bytes' or 'stream'");
		}
		
		// configure the transformers
		for (Iterator it = transformers.iterator(); it.hasNext(); ) {
			((TransformerAction)it.next()).configure();
		}
		eolArray = System.getProperty("line.separator").getBytes();
	}
	
	public Object handle(Object input, IPipeLineSession session) throws Exception {
		return handle(input, session, null);
	}
	
	public Object handle(Object input, IPipeLineSession session, ParameterList paramList) throws Exception {
		Object output = null;
		if (input instanceof byte[]) {
			output = (byte[])input;
		} else if (input instanceof InputStream) {
			if (transformers.get(0) instanceof TransformerActionWithInputTypeStream) {
				output = input;
			} else {
				output = Misc.streamToBytes((InputStream)input);
			}
		} else {
			output = (input == null) ? null : input.toString().getBytes(charset);
		}
		for (Iterator it = transformers.iterator(); it.hasNext(); ) {
			TransformerAction transformerAction = (TransformerAction)it.next();
			if (!it.hasNext() && "stream".equals(outputType)) {
				if (transformerAction instanceof TransformerActionWithOutputTypeStream) {
					output = ((TransformerActionWithOutputTypeStream)transformerAction).go((byte[])output, session, paramList, "stream");
				} else {
					output = new ByteArrayInputStream(transformerAction.go((byte[])output, session, paramList));
				}
			} else {
				if (output instanceof InputStream) {
					output = ((TransformerActionWithInputTypeStream)transformerAction).go((InputStream)output, session, paramList);
				} else {
					output = transformerAction.go((byte[])output, session, paramList);
				}
			}
		}
		if (output == null || "bytes".equals(outputType) || "stream".equals(outputType)) {
			if ("stream".equals(outputType) && isStreamResultToServlet()) {
				InputStream inputStream = (InputStream) output;
				HttpServletResponse response = (HttpServletResponse) session.get("restListenerServletResponse");
				String contentType = (String) session.get("contentType");
				String contentDisposition = (String) session.get("contentDisposition");
				if (StringUtils.isNotEmpty(contentType)) {
					response.setHeader("Content-Type", contentType); 
				}
				if (StringUtils.isNotEmpty(contentDisposition)) {
					response.setHeader("Content-Disposition", contentDisposition); 
				}
				OutputStream outputStream = response.getOutputStream();
				Misc.streamToStream(inputStream, outputStream);
				log.debug(getLogPrefix(session) + "copied response body input stream [" + inputStream + "] to output stream [" + outputStream + "]");
				return "";
			} else {
				return output;
			}
		} else {
			return new String((byte[])output, charset);
		}
	}
	
	/**
	 * The pipe supports several actions. All actions are implementations in
	 * inner-classes that implement the Transformer interface.
	 */
	protected interface TransformerAction {
		/* 
		 * @see nl.nn.adapterframework.core.IPipe#configure()
		 */
		void configure() throws ConfigurationException;
		/*
		 * transform the in and return the result
		 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, IPipeLineSession)
		 */
		byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception;
	}
	
	protected interface TransformerActionWithInputTypeStream extends TransformerAction {
		byte[] go(InputStream in, IPipeLineSession session, ParameterList paramList) throws Exception;
	}
	
	protected interface TransformerActionWithOutputTypeStream extends TransformerAction {
		InputStream go(byte[] in, IPipeLineSession session, ParameterList paramList, String outputType) throws Exception;
	}
	
	/**
	 * Encodes the input 
	 */
	private class Encoder implements TransformerAction {
		public void configure() {}
		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			return Base64.encodeBase64(in);
		}
	}
	
	/**
	 * Decodes the input
	 */
	private class Decoder implements TransformerAction {
		public void configure() {}
		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			return Base64.decodeBase64(in == null ? null : new String(in));
		}
	}

	private File createFile(IPipeLineSession session, ParameterList paramList) throws IOException, ParameterException {
		File tmpFile;

		String writeSuffix_work = null;
		if (paramList != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext(
					(String) null, session);
			ParameterValueList pvl = prc.getValues(paramList);
			if (pvl != null) {
				ParameterValue writeSuffixParamValue = pvl
						.getParameterValue("writeSuffix");
				if (writeSuffixParamValue != null) {
					writeSuffix_work = (String) writeSuffixParamValue
							.getValue();
				}
			}
		}
		if (writeSuffix_work == null) {
			writeSuffix_work = getWriteSuffix();
		}
		
		String name = fileName;
		if (StringUtils.isEmpty(name)) {
			name = (String)session.get(fileNameSessionKey);
		}
		if (StringUtils.isEmpty(getDirectory())) {
			if (StringUtils.isEmpty(name)) {
				tmpFile = File.createTempFile("ibis", writeSuffix_work);
			} else {
				tmpFile = new File(name);
			}
		} else {
			if (StringUtils.isEmpty(name)) {
				tmpFile = File.createTempFile("ibis", writeSuffix_work, new File(getDirectory()));
			} else {
				tmpFile = new File(getDirectory() + File.separator + name);
			}
		}
		return tmpFile;
	}

	/**
	 * Write the input to a file in the specified directory.
	 */
	private class FileWriter implements TransformerActionWithInputTypeStream {
		private boolean append = false;
		public FileWriter(boolean append) {
			this.append = append;
		}
		// create the directory structure if not exists and
		// check the permissions
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestCanWrite()) {
				if (!FileUtils.canWrite(getDirectory())) {
					throw new ConfigurationException(getLogPrefix(null)+"directory ["+ getDirectory() + "] is not a directory, or no write permission");
				}
			}
		}
		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			return go(new ByteArrayInputStream(in), session, paramList);
		}
		public byte[] go(InputStream in, IPipeLineSession session, ParameterList paramList) throws Exception {
			File tmpFile=createFile(session, paramList);
			if (!tmpFile.getParentFile().exists()) {
				if (isCreateDirectory()) {
					if (tmpFile.getParentFile().mkdirs()) {
						log.debug( getLogPrefix(session) + "created directory [" + tmpFile.getParent() +"]");
					} else {
						log.warn( getLogPrefix(session) + "directory [" + tmpFile.getParent() +"] could not be created");
					}
				} else {
					log.warn( getLogPrefix(session) + "directory [" + tmpFile.getParent() +"] does not exists");
				}
			}
			// Use tmpFile.getPath() instead of tmpFile to be WAS 5.0 / Java 1.3 compatible
			FileOutputStream fos = new FileOutputStream(tmpFile.getPath(), append);
			try {
				Misc.streamToStream(in, fos);
				if (isWriteLineSeparator()) {
					fos.write(eolArray);
				}
			} finally {
				fos.close();
			}
			
			return tmpFile.getPath().getBytes();
		}
	}

	/**
	 * create a new file.
	 */
	private class FileCreater implements TransformerAction {
		// create the directory structure if not exists and
		// check the permissions
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestCanWrite()) {
				if (!FileUtils.canWrite(getDirectory())) {
					throw new ConfigurationException(getLogPrefix(null)+"directory ["+ getDirectory() + "] is not a directory, or no write permission");
				}
			}
		}
		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			File tmpFile=createFile(session, paramList);
			if (!tmpFile.getParentFile().exists()) {
				if (isCreateDirectory()) {
					if (tmpFile.getParentFile().mkdirs()) {
						log.debug( getLogPrefix(session) + "created directory [" + tmpFile.getParent() +"]");
					} else {
						log.warn( getLogPrefix(session) + "directory [" + tmpFile.getParent() +"] could not be created");
					}
				} else {
					log.warn( getLogPrefix(session) + "directory [" + tmpFile.getParent() +"] does not exists");
				}
			}
			FileOutputStream fos = new FileOutputStream(tmpFile.getPath(), false);
			fos.close();
			return tmpFile.getPath().getBytes();
		}
	}

	/**
	 * Reads the file, which name is specified in the input, from the specified directory.
	 * The class supports the deletion of the file after reading.
	 */
	private class FileReader implements TransformerActionWithOutputTypeStream {
		private boolean deleteAfterRead;
		
		FileReader() {
			deleteAfterRead = false;
		}
		FileReader(boolean deleteAfterRead) {
			this.deleteAfterRead = deleteAfterRead;
		}
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory())) {
				File file = new File(getDirectory());
				if (!file.exists() && createDirectory) {
					if (!file.mkdirs()) {
						throw new ConfigurationException(directory + " could not be created");
					}
				}
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
			if (!fileSource.equalsIgnoreCase("filesystem") && !fileSource.equalsIgnoreCase("classpath")) {
				throw new ConfigurationException(getLogPrefix(null)+"illegal value for fileSource ["+outputType+"], must be 'filesystem' or 'classpath'");
			}
		}
		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			File file = getFile(in, session);
			FileInputStream fis = new FileInputStream(file);
			try {
				byte[] result = new byte[fis.available()];
				fis.read(result);
				if (isSkipBOM()) {
					if ((result[0] == BOM_UTF_8[0]) && (result[1] == BOM_UTF_8[1]) && (result[2] == BOM_UTF_8[2])) {
					    byte[] resultWithoutBOM = new byte[result.length-3];
					    for(int i = 3; i < result.length; ++i)
					    	resultWithoutBOM[i-3]=result[i];
					    log.debug(getLogPrefix(session) + "removed UTF-8 BOM");
					    return resultWithoutBOM;
					} else {
						return result;
					}
				} else {
					return result;
				}
			} finally {
				fis.close();
				if (deleteAfterRead)
					file.delete();
			}
		}
		public InputStream go(byte[] in, IPipeLineSession session, ParameterList paramList,
				String outputType) throws Exception {
			return new FileDeleteAfterReadInputStream(getFile(in, session), deleteAfterRead);
		}
		private File getFile(byte[] in, IPipeLineSession session) {
			File file;
			String name = (String)session.get(fileNameSessionKey);
			if (StringUtils.isEmpty(name)) {
				name = new String(in);
			}
			if (fileSource.equals("classpath")) {
				URL resource = ClassUtils.getResourceURL(this, name);
				file = new File(resource.getFile());
			} else {
				if (StringUtils.isNotEmpty(getDirectory())) {
					file = new File(getDirectory(), name);
				} else {
					file = new File(name);
				}
			}
			return file;
		}
	}

	/**
	 * Delete the file.
	 */
	private class FileDeleter implements TransformerAction {
		public void configure() throws ConfigurationException {
															
			if (StringUtils.isNotEmpty(getDirectory())) {
				File file = new File(getDirectory());
				if (! (file.exists() && file.isDirectory())) {
					throw new ConfigurationException(directory + " is not a directory");
				}
			}
			
		}
		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			File file;
			
			/* take filename from 
			 * 1) fileName attribute
			 * 2) fileNameSessionKey
			 * 3) otherwise take the pipe input  
			*/
			
			String name = fileName;
			
			if (StringUtils.isEmpty(name)) { 
				if (!(StringUtils.isEmpty(fileNameSessionKey))) { 
					name = (String)session.get(fileNameSessionKey); 
				}
			  	else {	
			  		name = new String(in); 
			  	}
			}

			/* check for directory path 
			 * if param directory not filled, 
			 * then filename's filepath.
			 */					
			if ( getDirectory() != null ) {
				file = new File(getDirectory(), name);
			} 
			else {
				file = new File( name );
			}
											
			/* if file exists, delete the file */
			if (file.exists()) {
				boolean success = file.delete();
				if (!success){
				   log.warn( getLogPrefix(session) + "could not delete file [" + file.toString() +"]");
				} 
				else {
				   log.debug(getLogPrefix(session) + "deleted file [" + file.toString() +"]");
				} 
			}
			else {
				log.warn( getLogPrefix(session) + "file [" + file.toString() +"] does not exist");
			}

			/* if parent directory is empty, delete the directory */
			if (isDeleteEmptyDirectory()) {
				File directory = file.getParentFile();
				if (directory.exists() && directory.list().length==0) {
					boolean success = directory.delete();
					if (!success){
					   log.warn( getLogPrefix(session) + "could not delete directory [" + directory.toString() +"]");
					} 
					else {
					   log.debug(getLogPrefix(session) + "deleted directory [" + directory.toString() +"]");
					} 
				} else {
					   log.debug(getLogPrefix(session) + "directory [" + directory.toString() +"] doesn't exist or is not empty");
				}
			}
			
			return in;
		}
	}

	private class FileLister implements TransformerAction {
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory())) {
				File file = new File(getDirectory());
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
		}

		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			String name = fileName;
			
			if (StringUtils.isEmpty(name)) { 
				if (!(StringUtils.isEmpty(fileNameSessionKey))) { 
					name = (String)session.get(fileNameSessionKey); 
				}
				else {	
					name = new String(in); 
				}
			}

			String dir = getDirectory();
			if (StringUtils.isEmpty(dir)) {
				File file = new File(name);
				String parent = file.getParent();
				if (parent!=null) {
					dir = parent;
					name = file.getName();
				}
			}

			Dir2Xml dx=new Dir2Xml();
			dx.setPath(dir);
			if (StringUtils.isNotEmpty(name)) { 
				dx.setWildCard(name);
			}
			String listResult=dx.getDirList();
			return listResult.getBytes();
		}
	}

	private class FileInfoProvider implements TransformerAction {
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory())) {
				File file = new File(getDirectory());
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
		}

		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			File file;
			 
			String name = (String)session.get(fileNameSessionKey);;
			
			if (StringUtils.isEmpty(name)) {
				name = new String(in);
			}
															
			if (StringUtils.isNotEmpty(getDirectory())) {
				file = new File(getDirectory(), name);
			} else {
				file = new File(name);
			}

			XmlBuilder fileXml = new XmlBuilder("file");
			XmlBuilder fullName = new XmlBuilder("fullName");
			fullName.setValue(file.getCanonicalPath());
			fileXml.addSubElement(fullName);
			XmlBuilder directory = new XmlBuilder("directory");
			String dir = file.getParent();
			if (dir!=null) {
				directory.setValue(dir);
			}
			fileXml.addSubElement(directory);
			XmlBuilder shortName = new XmlBuilder("name");
			String sname = file.getName();
			shortName.setValue(sname);
			fileXml.addSubElement(shortName);
			XmlBuilder baseName = new XmlBuilder("baseName");
			baseName.setValue(FileUtils.getBaseName(sname));
			fileXml.addSubElement(baseName);
			XmlBuilder extension = new XmlBuilder("extension");
			extension.setValue(FileUtils.getFileNameExtension(sname));
			fileXml.addSubElement(extension);
			XmlBuilder size = new XmlBuilder("size");
			long fileSize = file.length();
			size.setValue(Long.toString(fileSize));
			fileXml.addSubElement(size);
			XmlBuilder fSize = new XmlBuilder("fSize");
			fSize.setValue(Misc.toFileSize(fileSize,true));
			fileXml.addSubElement(fSize);
			Date lastModified = new Date(file.lastModified());
			String date = DateUtils.format(lastModified, DateUtils.FORMAT_DATE);
			XmlBuilder modificationDate = new XmlBuilder("modificationDate");
			modificationDate.setValue(date);
			fileXml.addSubElement(modificationDate);
			String time = DateUtils.format(lastModified, DateUtils.FORMAT_TIME_HMS);
			XmlBuilder modificationTime = new XmlBuilder("modificationTime");
			modificationTime.setValue(time);
			fileXml.addSubElement(modificationTime);
			return fileXml.toXML().getBytes();
		}
	}

	protected String getLogPrefix(IPipeLineSession session){
		StringBuilder sb = new StringBuilder();
		sb.append(ClassUtils.nameOf(this)).append(' ');
		if (this instanceof INamedObject) {
			sb.append("[").append(((INamedObject)this).getName()).append("] ");
		}
		if (session != null) {
			sb.append("msgId [").append(session.getMessageId()).append("] ");
		}
		return sb.toString();
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public void setFileSource(String fileSource) {
		this.fileSource = fileSource;
	}

	/**
	 * @param actions all the actions the pipe has to do
	 * 
	 * Possible actions are "read", "write", "write_append", "encode", "decode", "delete" and "read_delete"
	 * You can also define combinations, like "read encode write".
	 */
	public void setActions(String actions) {
		this.actions = actions;
	}
	public String getActions() {
		return actions;
	}

	/**
	 * @param directory in which the file resides or has to be created
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	public String getDirectory() {
		return directory;
	}

	/**
	 * @param suffix of the file that is written
	 */
	public void setWriteSuffix(String suffix) {
		this.writeSuffix = suffix;
	}
	public String getWriteSuffix() {
		return writeSuffix;
	}

	/**
	 * @param filename of the file that is written
	 */
	public void setFileName(String filename) {
		this.fileName = filename;
	}
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param filenameSessionKey the session key that contains the name of the file to be created
	 */
	public void setFileNameSessionKey(String filenameSessionKey) {
		this.fileNameSessionKey = filenameSessionKey;
	}
	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	public void setCreateDirectory(boolean b) {
		createDirectory = b;
	}
	public boolean isCreateDirectory() {
		return createDirectory;
	}

	public void setWriteLineSeparator(boolean b) {
		writeLineSeparator = b;
	}
	public boolean isWriteLineSeparator() {
		return writeLineSeparator;
	}

	public void setTestCanWrite(boolean b) {
		testCanWrite = b;
	}
	public boolean isTestCanWrite() {
		return testCanWrite;
	}

	public void setSkipBOM(boolean b) {
		skipBOM = b;
	}
	public boolean isSkipBOM() {
		return skipBOM;
	}

	public void setDeleteEmptyDirectory(boolean b) {
		deleteEmptyDirectory = b;
	}
	public boolean isDeleteEmptyDirectory() {
		return deleteEmptyDirectory;
	}

	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	private class FileDeleteAfterReadInputStream extends FileInputStream {
		File file;
		boolean deleteAfterRead;

		public FileDeleteAfterReadInputStream(File file, boolean deleteAfterRead) throws FileNotFoundException {
			super(file);
			this.file = file;
			this.deleteAfterRead = deleteAfterRead;
			if (deleteAfterRead) {
				file.deleteOnExit();
			}
		}

		@Override
		public int read() throws IOException {
			int i = super.read();
			if (i == -1 && deleteAfterRead) {
				file.delete();
			}
			return i;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int i = super.read(b);
			if (i == -1 && deleteAfterRead) {
				file.delete();
			}
			return i;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int i = super.read(b, off, len);
			if (i == -1 && deleteAfterRead) {
				file.delete();
			}
			return i;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (deleteAfterRead) {
				file.delete();
			}
		}
	}
}
