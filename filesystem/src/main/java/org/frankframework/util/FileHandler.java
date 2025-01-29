/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package org.frankframework.util;

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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IPipe;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FilePipe;
import org.frankframework.senders.FileSender;
import org.frankframework.stream.Message;


/**
 * FileHandler, available as {@link FileSender} and {@link FilePipe}, allows to write to or read from a file.
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
@Log4j2
@Deprecated(forRemoval = true, since = "7.8")
public class FileHandler implements IScopeProvider {
	protected static final byte[] BOM_UTF_8 = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	protected String charset = System.getProperty("file.encoding");
	protected String outputType = "string";
	protected String fileSource = "filesystem";
	protected String actions;
	protected String directory;
	protected String writeSuffix;
	protected String filename;
	protected String filenameSessionKey;
	protected boolean createDirectory = false;
	protected boolean writeLineSeparator = false;
	protected boolean testExists = true;
	protected boolean testCanWrite = true;
	protected boolean skipBOM = false;
	protected boolean deleteEmptyDirectory = false;
	protected boolean streamResultToServlet=false;

	protected List<TransformerAction> transformers;
	protected byte[] eolArray=null;

	/**
	 * @see IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		// translation action separated string to Transformers
		transformers = new ArrayList<>();
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

		if (transformers.isEmpty())
			throw new ConfigurationException(getLogPrefix(null)+"should at least define one action");
		if (!"string".equalsIgnoreCase(outputType)
				&& !"bytes".equalsIgnoreCase(outputType)
				&& !"base64".equalsIgnoreCase(outputType)
				&& !"stream".equalsIgnoreCase(outputType)) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for outputType ["+outputType+"], must be 'string', 'bytes' or 'stream'");
		}

		// configure the transformers
		for (TransformerAction transformer : transformers) {
			transformer.configure();
		}
		eolArray = System.getProperty("line.separator").getBytes();
	}

	public Object handle(Message input, PipeLineSession session, ParameterList paramList) throws Exception {
		Object output = null;
		if (input!=null) {
			if (input.isRequestOfType(byte[].class)) {
				output = input.asByteArray();
			} else if (input.isRequestOfType(InputStream.class)) {
				if (transformers.get(0) instanceof TransformerActionWithInputTypeStream) {
					output = input.asInputStream();
				} else {
					output = input.asByteArray();
				}
			} else {
				output = input.asByteArray(charset);
			}
		}
		for (Iterator<TransformerAction> it = transformers.iterator(); it.hasNext(); ) {
			TransformerAction transformerAction = it.next();
			if (!it.hasNext() && "stream".equals(outputType)) {
				if (transformerAction instanceof TransformerActionWithOutputTypeStream stream) {
					output = stream.go((byte[])output, session, paramList, "stream");
				} else {
					output = new ByteArrayInputStream(transformerAction.go((byte[])output, session, paramList));
				}
			} else {
				if (output instanceof InputStream stream) {
					output = ((TransformerActionWithInputTypeStream)transformerAction).go(stream, session, paramList);
				} else {
					output = transformerAction.go((byte[])output, session, paramList);
				}
			}
		}
		if (output == null || "bytes".equals(outputType) || "base64".equals(outputType) || "stream".equals(outputType)) {
			if ("stream".equals(outputType) && isStreamResultToServlet()) {
				InputStream inputStream = (InputStream) output;
				HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
				String contentType = session.getString("contentType");
				String contentDisposition = session.getString("contentDisposition");
				if (StringUtils.isNotEmpty(contentType)) {
					response.setHeader("Content-Type", contentType);
				}
				if (StringUtils.isNotEmpty(contentDisposition)) {
					response.setHeader("Content-Disposition", contentDisposition);
				}
				OutputStream outputStream = response.getOutputStream();
				StreamUtil.streamToStream(inputStream, outputStream);
				log.debug("{}copied response body input stream [{}] to output stream [{}]", getLogPrefix(session), inputStream, outputStream);
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
		 * @see org.frankframework.core.IPipe#configure()
		 */
		void configure() throws ConfigurationException;
		/*
		 * transform the in and return the result
		 * @see org.frankframework.core.IPipe#doPipe(Object, PipeLineSession)
		 */
		byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception;
	}

	protected interface TransformerActionWithInputTypeStream extends TransformerAction {
		byte[] go(InputStream in, PipeLineSession session, ParameterList paramList) throws Exception;
	}

	protected interface TransformerActionWithOutputTypeStream extends TransformerAction {
		InputStream go(byte[] in, PipeLineSession session, ParameterList paramList, String outputType) throws Exception;
	}

	/**
	 * Encodes the input
	 */
	private class Encoder implements TransformerAction {
		@Override
		public void configure() {}
		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
			return Base64.encodeBase64(in);
		}
	}

	/**
	 * Decodes the input
	 */
	private class Decoder implements TransformerAction {
		@Override
		public void configure() {}
		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
			return Base64.decodeBase64(in == null ? null : new String(in));
		}
	}

	private String getEffectiveFileName(byte[] in, PipeLineSession session) {
		String name = getFilename();
		if (StringUtils.isEmpty(name)) {
			name = session.getString(filenameSessionKey);
		}
		if (in != null && StringUtils.isEmpty(name)) {
			name = new String(in);
		}
		return name;
	}

	private Object getEffectiveFile(byte[] in, PipeLineSession session) {
		String name = getEffectiveFileName(in, session);
		if ("classpath".equals(fileSource)) {
			return ClassLoaderUtils.getResourceURL(this, name);
		} else {
			if (StringUtils.isNotEmpty(getDirectory())) {
				return new File(getDirectory(), name);
			} else {
				return new File(name);
			}
		}
	}

	private File createFile(PipeLineSession session, ParameterList paramList) throws IOException, ParameterException {
		File tmpFile;

		String writeSuffix_work = null;
		if (paramList != null) {
			ParameterValueList pvl = paramList.getValues(null, session);
			ParameterValue writeSuffixParamValue = pvl.get("writeSuffix");
			if (writeSuffixParamValue != null) {
				writeSuffix_work = (String) writeSuffixParamValue.getValue();
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
		@Override
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestCanWrite()) {
				if (!canWrite(getDirectory())) {
					throw new ConfigurationException(getLogPrefix(null)+"directory ["+ getDirectory() + "] is not a directory, or no write permission");
				}
			}
		}
		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
			return go(new ByteArrayInputStream(in), session, paramList);
		}
		@Override
		public byte[] go(InputStream in, PipeLineSession session, ParameterList paramList) throws Exception {
			File tmpFile=createFile(session, paramList);
			if (!tmpFile.getParentFile().exists()) {
				if (isCreateDirectory()) {
					if (tmpFile.getParentFile().mkdirs()) {
						log.debug("{}created directory [{}]", getLogPrefix(session), tmpFile.getParent());
					} else {
						log.warn("{}directory [{}] could not be created", getLogPrefix(session), tmpFile.getParent());
					}
				} else {
					log.warn("{}directory [{}] does not exists", getLogPrefix(session), tmpFile.getParent());
				}
			}
			// Use tmpFile.getPath() instead of tmpFile to be WAS 5.0 / Java 1.3 compatible
			try(FileOutputStream fos = new FileOutputStream(tmpFile.getPath(), append)){
				StreamUtil.streamToStream(in, fos, isWriteLineSeparator() ? eolArray : null);
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
		@Override
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestCanWrite()) {
				if (!canWrite(getDirectory())) {
					throw new ConfigurationException(getLogPrefix(null)+"directory ["+ getDirectory() + "] is not a directory, or no write permission");
				}
			}
		}
		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
			File tmpFile=createFile(session, paramList);
			if (!tmpFile.getParentFile().exists()) {
				if (isCreateDirectory()) {
					if (tmpFile.getParentFile().mkdirs()) {
						log.debug("{}created directory [{}]", getLogPrefix(session), tmpFile.getParent());
					} else {
						log.warn("{}directory [{}] could not be created", getLogPrefix(session), tmpFile.getParent());
					}
				} else {
					log.warn("{}directory [{}] does not exists", getLogPrefix(session), tmpFile.getParent());
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
		private final boolean deleteAfterRead;

		FileReader() {
			deleteAfterRead = false;
		}

		FileReader(boolean deleteAfterRead) {
			this.deleteAfterRead = deleteAfterRead;
		}

		@Override
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory())) {
				File file = new File(getDirectory());
				if (!file.exists() && isCreateDirectory()) {
					if (!file.mkdirs()) {
						throw new ConfigurationException(directory + " could not be created");
					}
				}
				if (isTestExists() && ! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
			if (!"filesystem".equalsIgnoreCase(fileSource) && !"classpath".equalsIgnoreCase(fileSource)) {
				throw new ConfigurationException(getLogPrefix(null)+"illegal value for fileSource ["+outputType+"], must be 'filesystem' or 'classpath'");
			}
			if (deleteAfterRead && "classpath".equalsIgnoreCase(fileSource)) {
				throw new ConfigurationException(getLogPrefix(null)+"read_delete not allowed in combination with fileSource ["+outputType+"]");
			}
		}

		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
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

		@Override
		public InputStream go(byte[] in, PipeLineSession session, ParameterList paramList,
							  String outputType) throws Exception {
			return getSkipBomAndDeleteFileAfterReadInputStream(in, session);
		}

		private InputStream getSkipBomAndDeleteFileAfterReadInputStream(
				byte[] in, PipeLineSession session) throws IOException {
			InputStream inputStream;
			File file = null;
			Object object = getEffectiveFile(in, session);
			if (object instanceof File file1) {
				file = file1;
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

		@Override
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestExists()) {
				File file = new File(getDirectory());
				if (! (file.exists() && file.isDirectory())) {
					throw new ConfigurationException(directory + " is not a directory");
				}
			}
			if ("classpath".equalsIgnoreCase(fileSource)) {
				throw new ConfigurationException(getLogPrefix(null)+"delete not allowed in combination with fileSource ["+outputType+"]");
			}
		}

		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
			// Can only return URL in case fileSource is classpath (which should
			// have given a configuration warning before this method is called).
			File file = (File)getEffectiveFile(in, session);
			/* if file exists, delete the file */
			if (file.exists()) {
				boolean success = file.delete();
				if (!success){
					log.warn("{}could not delete file [{}]", getLogPrefix(session), file);
				}
				else {
					log.debug("{}deleted file [{}]", getLogPrefix(session), file);
				}
			}
			else {
				log.warn("{}file [{}] does not exist", getLogPrefix(session), file);
			}
			/* if parent directory is empty, delete the directory */
			if (isDeleteEmptyDirectory()) {
				File directory = file.getParentFile();
				if (directory.exists() && directory.list().length==0) {
					boolean success = directory.delete();
					if (!success){
						log.warn("{}could not delete directory [{}]", getLogPrefix(session), directory);
					}
					else {
						log.debug("{}deleted directory [{}]", getLogPrefix(session), directory);
					}
				} else {
					log.debug("{}directory [{}] doesn't exist or is not empty", getLogPrefix(session), directory);
				}
			}
			return in;
		}

	}

	private class FileLister implements TransformerAction {

		@Override
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestExists()) {
				File file = new File(getDirectory());
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
		}

		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
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

		@Override
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory()) && isTestExists()) {
				File file = new File(getDirectory());
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
		}

		@Override
		public byte[] go(byte[] in, PipeLineSession session, ParameterList paramList) throws Exception {
			File file = null;
			Object object = getEffectiveFile(in, session);
			if (object instanceof File file1) {
				file = file1;
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
			String date = DateFormatUtils.format(lastModified, DateFormatUtils.SHORT_DATE_FORMATTER);
			XmlBuilder modificationDate = new XmlBuilder("modificationDate");
			modificationDate.setValue(date);
			fileXml.addSubElement(modificationDate);
			String time = DateFormatUtils.format(lastModified, DateFormatUtils.TIME_HMS_FORMATTER);
			XmlBuilder modificationTime = new XmlBuilder("modificationTime");
			modificationTime.setValue(time);
			fileXml.addSubElement(modificationTime);
			return fileXml.asXmlString().getBytes();
		}

	}

	protected String getLogPrefix(PipeLineSession session){
		StringBuilder sb = new StringBuilder();
		sb.append(ClassUtils.nameOf(this)).append(' ');
		if (session != null) {
			sb.append("msgId [").append(session.getMessageId()).append("] ");
		}
		return sb.toString();
	}

	/**
	 * the charset to be used when transforming a string to a byte array and/or the other way around
	 * @ff.default the value of the system property file.encoding
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * either <code>string</code>, <code>bytes</code>, <code>stream</code> or <code>base64</code>
	 * @ff.default string
	 */
	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	/**
	 * either <code>filesystem</code> or <code>classpath</code> (classpath will only work for actions 'read' and 'info' and for 'info' only when resources are available as a file (i.e. doesn't work for resources in jar files and war files which are deployed without being extracted by the application server))
	 * @ff.default filesystem
	 */
	public void setFileSource(String fileSource) {
		this.fileSource = fileSource;
	}

	/**
	 * Sets actions the pipe has to perform. Possible action values:
	 * <ul>
	 *   <li>write: create a new file and write input to it</li>
	 *   <li>write_append: create a new file if it does not exist, otherwise append to existing file; then write input to it</li>
	 *   <li>create: create a new file, but do not write anything to it</li>
	 *   <li>read: read from file</li>
	 *   <li>delete: delete the file</li>
	 *   <li>read_delete: read the contents, then delete (when outputType is stream the file is deleted after the stream is read)</li>
	 *   <li>encode: encode base64</li> <li>decode: decode base64</li>
	 *   <li>list: returns the files and directories in the directory that satisfy the specified filter (see {@link Dir2Xml}). If a directory is not specified, the fileName is expected to include the directory</li>
	 *   <li>info: returns information about the file</li>
	 * </ul> */
	public void setActions(String actions) {
		this.actions = actions;
	}
	public String getActions() {
		return actions;
	}

	/**
	 * Sets the directory in which the file resides or has to be created
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	public String getDirectory() {
		return directory;
	}

	/** Sets suffix of the file that is written (only used if filename and filenamesession are not set) */
	public void setWriteSuffix(String suffix) {
		this.writeSuffix = suffix;
	}
	public String getWriteSuffix() {
		return writeSuffix;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileName' is replaced with 'filename'")
	public void setFileName(String filename) {
		setFilename(filename);
	}

	/**
	 * Sets filename of the file that is written
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileNameSessionKey' is replaced with 'filenameSessionKey'")
	public void setFileNameSessionKey(String filenameSessionKey) {
		setFilenameSessionKey(filenameSessionKey);
	}

	/** Sets filenameSessionKey the session key that contains the name of the file to be created (only used if filename is not set) */
	public void setFilenameSessionKey(String filenameSessionKey) {
		this.filenameSessionKey = filenameSessionKey;
	}
	public String getFilenameSessionKey() {
		return filenameSessionKey;
	}

	/**
	 * test if the specified directory exists at configure()
	 * @ff.default true
	 */
	public void setTestExists(boolean testExists) {
		this.testExists = testExists;
	}
	public boolean isTestExists() {
		return testExists;
	}

	/**
	 * when set to <code>true</code>, the directory to read from or write to is created if it does not exist
	 * @ff.default false
	 */
	public void setCreateDirectory(boolean b) {
		createDirectory = b;
	}
	public boolean isCreateDirectory() {
		return createDirectory;
	}

	/**
	 * when set to <code>true</code>, a line separator is written after the content is written
	 * @ff.default false
	 */
	public void setWriteLineSeparator(boolean b) {
		writeLineSeparator = b;
	}
	public boolean isWriteLineSeparator() {
		return writeLineSeparator;
	}

	/**
	 * when set to <code>true</code>, a test is performed to find out if a temporary file can be created and deleted in the specified directory (only used if directory is set and combined with the action write, write_append or create)
	 * @ff.default true
	 */
	public void setTestCanWrite(boolean b) {
		testCanWrite = b;
	}
	public boolean isTestCanWrite() {
		return testCanWrite;
	}

	/**
	 * when set to <code>true</code>, a possible bytes order mark (bom) at the start of the file is skipped (only used for the action read and encoding uft-8)
	 * @ff.default false
	 */
	public void setSkipBOM(boolean b) {
		skipBOM = b;
	}
	public boolean isSkipBOM() {
		return skipBOM;
	}

	/**
	 * (only used when actions=delete) when set to <code>true</code>, the directory from which a file is deleted is also deleted when it contains no other files
	 * @ff.default false
	 */
	public void setDeleteEmptyDirectory(boolean b) {
		deleteEmptyDirectory = b;
	}
	public boolean isDeleteEmptyDirectory() {
		return deleteEmptyDirectory;
	}

	/**
	 * (only used when outputtype=stream) if set, the result is streamed to the httpservletresponse object
	 * @ff.default false
	 */
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	private static boolean canWrite(String directory) {
		try {
			File file = new File(directory);
			if (!file.exists()) {
				file.mkdirs();
			}
			if (!file.isDirectory()) {
				log.debug("Directory [{}] is not a directory", directory);
				return false;
			}
			File tmpFile = File.createTempFile("ibis", null, file);
			try {
				Files.delete(tmpFile.toPath());
			} catch (Exception t) {
				log.warn("Exception while deleting temporary file [{}] in directory [{}]", tmpFile.getName(), directory, t);
			}
			return true;
		} catch (IOException e) {
			log.debug("Exception while creating a temporary file in directory [{}]", directory, e);
			return false;
		} catch (SecurityException e) {
			log.debug("Exception while testing if the application is allowed to write to directory [{}]", directory, e);
			return false;
		}
	}

	private class SkipBomAndDeleteFileAfterReadInputStream extends BufferedInputStream {
		private final File file;
		private final boolean deleteAfterRead;
		private final PipeLineSession session;
		private boolean firstByteRead = false;

		public SkipBomAndDeleteFileAfterReadInputStream(InputStream inputStream, File file, boolean deleteAfterRead, PipeLineSession session)
				throws FileNotFoundException {
			super(inputStream);
			this.file = file;
			this.deleteAfterRead = deleteAfterRead;
			this.session = session;
			if (deleteAfterRead && (file == null)) {
				// This should not happen. A configuration warning for
				// read_delete in combination with classpath should have
				// occurred already.
				throw new FileNotFoundException("No file object provided");
			}
		}

		@Override
		public int read() throws IOException {
			skipBOM();
			int i = super.read();
			if (i == -1 && deleteAfterRead) {
				Files.delete(file.toPath());
			}
			return i;
		}

		@Override
		public int read(byte[] b) throws IOException {
			skipBOM();
			int i = super.read(b);
			if (i == -1 && deleteAfterRead) {
				Files.delete(file.toPath());
			}
			return i;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			skipBOM();
			int i = super.read(b, off, len);
			if (i == -1 && deleteAfterRead) {
				Files.delete(file.toPath());
			}
			return i;
		}

		@Override
		public void close() throws IOException {
			super.close();
			if (deleteAfterRead) {
				Files.delete(file.toPath());
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
							log.debug("{} removed UTF-8 BOM", ()->getLogPrefix(session));
							return;
						}
					}
				}
				super.reset();
			}
		}
	}
}
