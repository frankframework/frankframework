/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
import nl.nn.adapterframework.doc.IbisDoc;
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
 * 
 * <p>
 * Actions take place on the file specified by the fileName attribute (or when
 * not available the fileNameSessionKey, when fileNameSessionKey is empty too
 * the input of the pipe is used as file name). When a directory is not
 * specified, the fileName is expected to include the directory.
 * </p>
 * 
 * <p>
 * When a file needs to be created and both the fileName and the directory are
 * not specified a temporary file is created as specified by the
 * java.io.File.createTempFile method using the string "ibis" as a
 * prefix and a suffix as specified bij the writeSuffix attribute. If only
 * the directory is specified, the temporary file is created the same way except
 * that the temporary file is created in the specified directory.
 * </p>
 * 
 * <p>
 * The pipe also support base64 en- and decoding.
 * </p>
 * 
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
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

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
		if (StringUtils.isEmpty(getActions()))
			throw new ConfigurationException(getLogPrefix(null)+"should at least define one action");
			
		StringTokenizer tok = new StringTokenizer(getActions(), " ,\t\n\r\f");
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
				&& !outputType.equalsIgnoreCase("base64")
				&& !outputType.equalsIgnoreCase("stream")) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for outputType ["+outputType+"], must be 'string', 'bytes' or 'stream'");
		}
		
		// configure the transformers
		for (Iterator it = transformers.iterator(); it.hasNext(); ) {
			((TransformerAction)it.next()).configure();
		}
		eolArray = System.getProperty("line.separator").getBytes();
	}
	
//	public Object handle(Object input, IPipeLineSession session) throws Exception {
//		return handle(input, session, null);
//	}
	
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
		if (output == null || "bytes".equals(outputType) || "base64".equals(outputType) || "stream".equals(outputType)) {
			if ("stream".equals(outputType) && isStreamResultToServlet()) {
				InputStream inputStream = (InputStream) output;
				HttpServletResponse response = (HttpServletResponse) session.get(IPipeLineSession.HTTP_RESPONSE_KEY);
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
				if ("base64".equals(outputType)) {
					return new String(Base64.encodeBase64((byte[]) output),
							charset);
				} else {
					return output;
				}
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

	private String getEffectiveFileName(byte[] in, IPipeLineSession session) {
		String name = getFileName();
		if (StringUtils.isEmpty(name)) {
			name = (String)session.get(fileNameSessionKey);
		}
		if (in != null && StringUtils.isEmpty(name)) {
			name = new String(in);
		}
		return name;
	}

	private Object getEffectiveFile(byte[] in, IPipeLineSession session)
			throws IOException {
		String name = getEffectiveFileName(in, session);
		if (fileSource.equals("classpath")) {
			return ClassUtils.getResourceURL(classLoader, name);
		} else {
			if (StringUtils.isNotEmpty(getDirectory())) {
				return new File(getDirectory(), name);
			} else {
				return new File(name);
			}
		}
	}

	private File createFile(byte[] in, IPipeLineSession session, ParameterList paramList) throws IOException, ParameterException {
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
		
		String name = getEffectiveFileName(null, session);
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
			File tmpFile=createFile(null, session, paramList);
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
			File tmpFile=createFile(in, session, paramList);
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
				if (!file.exists() && isCreateDirectory()) {
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
			if (deleteAfterRead && fileSource.equalsIgnoreCase("classpath")) {
				throw new ConfigurationException(getLogPrefix(null)+"read_delete not allowed in combination with fileSource ["+outputType+"]");
			}
		}

		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			InputStream inputStream =
					getSkipBomAndDeleteFileAfterReadInputStream(in, session);
			try {
				byte[] result = new byte[inputStream.available()];
				inputStream.read(result);
				return result;
			} finally {
				inputStream.close();
			}
		}

		public InputStream go(byte[] in, IPipeLineSession session, ParameterList paramList,
				String outputType) throws Exception {
			return getSkipBomAndDeleteFileAfterReadInputStream(in, session);
		}

		private InputStream getSkipBomAndDeleteFileAfterReadInputStream(
				byte[] in, IPipeLineSession session) throws IOException {
			InputStream inputStream;
			File file = null;
			Object object = getEffectiveFile(in, session);
			if (object instanceof File) {
				file = (File)object;
				inputStream = new FileInputStream(file);
			} else {
				inputStream = ((URL)object).openStream();
			}
			return new SkipBomAndDeleteFileAfterReadInputStream(inputStream,
					file, deleteAfterRead, session);
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
			if (fileSource.equalsIgnoreCase("classpath")) {
				throw new ConfigurationException(getLogPrefix(null)+"delete not allowed in combination with fileSource ["+outputType+"]");
			}
		}

		public byte[] go(byte[] in, IPipeLineSession session, ParameterList paramList) throws Exception {
			// Can only return URL in case fileSource is classpath (which should
			// have given a configuration warning before this method is called).
			File file = (File)getEffectiveFile(in, session);
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
			String name = getEffectiveFileName(in, session);

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
			File file = null;
			Object object = getEffectiveFile(in, session);
			if (object instanceof File) {
				file = (File)object;
			} else {
				URL url = (URL)object;
				String fileName = url.getFile();
				if (StringUtils.isEmpty(fileName)) {
					throw new Exception("File not available on the filesystem");
				}
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

	@IbisDoc({"the charset to be used when transforming a string to a byte array and/or the other way around", "the value of the system property file.encoding"})
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@IbisDoc({"either <code>string</code>, <code>bytes</code>, <code>stream</code> or <code>base64</code>", "string"})
	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	@IbisDoc({"either <code>filesystem</code> or <code>classpath</code> (classpath will only work for actions 'read' and 'info' and for 'info' only when resources are available as a file (i.e. doesn't work for resources in jar files and war files which are deployed without being extracted by the application server))", "filesystem"})
	public void setFileSource(String fileSource) {
		this.fileSource = fileSource;
	}

	/**
	 * Sets actions the pipe has to do. Possible actions are "read", "write", "write_append", "encode", "decode", "delete" and "read_delete"
	 * You can also define combinations, like "read encode write".
	 */
	@IbisDoc({"comma separated list of actions to be performed. Possible action values: <ul> <li>write: create a new file and write input to it</li> <li>write_append: create a new file if it does not exist, otherwise append to existing file; then write input to it</li> <li>create: create a new file, but do not write anything to it</li> <li>read: read from file</li> <li>delete: delete the file</li><li>read_delete: read the contents, then delete (when outputType is stream the file is deleted after the stream is read)</li> <li>encode: encode base64</li> <li>decode: decode base64</li> <li>list: returns the files and directories in the directory that satisfy the specified filter (see {@link nl.nn.adapterframework.util.Dir2Xml dir2xml}). If a directory is not specified, the fileName is expected to include the directory</li> <li>info: returns information about the file</li> </ul>", ""})
	public void setActions(String actions) {
		this.actions = actions;
	}
	public String getActions() {
		return actions;
	}

	/**
	 * Sets the directory in which the file resides or has to be created
	 */
	@IbisDoc({"base directory where files are stored in or read from", ""})
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	public String getDirectory() {
		return directory;
	}

	/**
	 * Sets suffix of the file that is written
	 */
	@IbisDoc({"suffix of the file to be created (only used if filename and filenamesession are not set)", ""})
	public void setWriteSuffix(String suffix) {
		this.writeSuffix = suffix;
	}
	public String getWriteSuffix() {
		return writeSuffix;
	}

	/**
	 * Sets filename of the file that is written
	 */
	@IbisDoc({"the name of the file to use", ""})
	public void setFileName(String filename) {
		this.fileName = filename;
	}
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets filenameSessionKey the session key that contains the name of the file to be created
	 */
	@IbisDoc({"the session key that contains the name of the file to use (only used if filename is not set)", ""})
	public void setFileNameSessionKey(String filenameSessionKey) {
		this.fileNameSessionKey = filenameSessionKey;
	}
	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	@IbisDoc({"when set to <code>true</code>, the directory to read from or write to is created if it does not exist", "false"})
	public void setCreateDirectory(boolean b) {
		createDirectory = b;
	}
	public boolean isCreateDirectory() {
		return createDirectory;
	}

	@IbisDoc({"when set to <code>true</code>, a line separator is written after the content is written", "false"})
	public void setWriteLineSeparator(boolean b) {
		writeLineSeparator = b;
	}
	public boolean isWriteLineSeparator() {
		return writeLineSeparator;
	}

	@IbisDoc({"when set to <code>true</code>, a test is performed to find out if a temporary file can be created and deleted in the specified directory (only used if directory is set and combined with the action write, write_append or create)", "true"})
	public void setTestCanWrite(boolean b) {
		testCanWrite = b;
	}
	public boolean isTestCanWrite() {
		return testCanWrite;
	}

	@IbisDoc({"when set to <code>true</code>, a possible bytes order mark (bom) at the start of the file is skipped (only used for the action read and encoding uft-8)", "false"})
	public void setSkipBOM(boolean b) {
		skipBOM = b;
	}
	public boolean isSkipBOM() {
		return skipBOM;
	}

	@IbisDoc({"(only used when actions=delete) when set to <code>true</code>, the directory from which a file is deleted is also deleted when it contains no other files", "false"})
	public void setDeleteEmptyDirectory(boolean b) {
		deleteEmptyDirectory = b;
	}
	public boolean isDeleteEmptyDirectory() {
		return deleteEmptyDirectory;
	}

	@IbisDoc({"(only used when outputtype=stream) if set, the result is streamed to the httpservletresponse object", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	private class SkipBomAndDeleteFileAfterReadInputStream extends BufferedInputStream {
		private File file;
		private boolean deleteAfterRead;
		private IPipeLineSession session;
		private boolean firstByteRead = false;

		public SkipBomAndDeleteFileAfterReadInputStream(InputStream inputStream,
				File file, boolean deleteAfterRead, IPipeLineSession session)
				throws FileNotFoundException {
			super(inputStream);
			this.file = file;
			this.deleteAfterRead = deleteAfterRead;
			this.session = session;
			if (deleteAfterRead) {
				if (file == null) {
					// This should not happen. A configuration warning for
					// read_delete in combination with classpath should have
					// occurred already.
					throw new FileNotFoundException("No file object provided");
				} else {
					file.deleteOnExit();
				}
			}
		}

		@Override
		public int read() throws IOException {
			skipBOM();
			int i = super.read();
			if (i == -1 && deleteAfterRead) {
				file.delete();
			}
			return i;
		}

		@Override
		public int read(byte[] b) throws IOException {
			skipBOM();
			int i = super.read(b);
			if (i == -1 && deleteAfterRead) {
				file.delete();
			}
			return i;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			skipBOM();
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

		private void skipBOM() throws IOException {
			if (!firstByteRead && isSkipBOM()) {
				firstByteRead = true;
				super.mark(3);
				byte i = (byte)super.read();
				if (i == BOM_UTF_8[0]) {
					i = (byte)super.read();
					if (i == BOM_UTF_8[1]) {
						i = (byte)super.read();
						if (i == BOM_UTF_8[2]) {
							log.debug(getLogPrefix(session) + "removed UTF-8 BOM");
							return;
						}
					}
				}
				super.reset();
			}
		}
	}
}
