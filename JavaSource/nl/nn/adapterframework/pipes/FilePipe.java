/*
 * $Log: FilePipe.java,v $
 * Revision 1.11  2006-08-22 12:53:45  europe\L190409
 * added fileName and fileNameSessionKey attributes
 *
 * Revision 1.10  2006/05/04 06:47:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * handles correctly incoming byte[]
 *
 * Revision 1.9  2005/12/08 08:00:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.8  2005/12/07 16:09:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified handling for filename
 *
 * Revision 1.7  2004/08/23 13:44:13  unknown <unknown@ibissource.org>
 * Add config checks
 *
 * Revision 1.5  2004/04/27 11:03:35  unknown <unknown@ibissource.org>
 * Renamed internal Transformer interface to prevent naming confusions
 *
 * Revision 1.3  2004/04/26 13:06:53  unknown <unknown@ibissource.org>
 * Support for file en- and decoding
 *
 * Revision 1.2  2004/04/26 13:04:50  unknown <unknown@ibissource.org>
 * Support for file en- and decoding
 *
 * Revision 1.1  2004/04/26 11:51:34  unknown <unknown@ibissource.org>
 * Support for file en- and decoding
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


/**
 * FilePipe allows to write to or read from a file. 
 * Write will create a file in the specified directory. The name is a concatenation
 * of the correlation id, a string that guarantees the filename to be unique and the
 * specified writeSuffic.
 * The pipe also support base64 en- and decoding.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirectory(String) directory}</td><td>base directory where files are stored in or read from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td><td>The name of the file to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileNameSessionKey(String) fileNameSessionKey}</td><td>The session key that contains the name of the file to use (only used if fileName is not set)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setActions(String) actions}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setWriteSuffix(String) writeSuffix}</td><td>suffix of the file to be created (only used if fileName and fileNameSession are not set)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author J. Dekker
 * @version Id
 *
 */
public class FilePipe extends FixedForwardPipe {
	public static final String version="$RCSfile: FilePipe.java,v $ $Revision: 1.11 $ $Date: 2006-08-22 12:53:45 $";
	private List transformers;
	protected String actions;
	protected String directory;
	protected String writeSuffix;
	protected String fileName;
	protected String fileNameSessionKey;

	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		// translation action seperated string to Transformers		
		transformers = new LinkedList();
		if (StringUtils.isEmpty(actions))
			throw new ConfigurationException("should at least define one action");
			
		StringTokenizer tok = new StringTokenizer(actions, " ,\t\n\r\f");
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			
			if ("write".equalsIgnoreCase(token))
				transformers.add(new FileWriter(false));
			else if ("write_append".equalsIgnoreCase(token))
				transformers.add(new FileWriter(true));
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
			else
				throw new ConfigurationException("Action " + token + " is not supported");
		}
		
		if (transformers.size() == 0)
			throw new ConfigurationException("should at least define one action");
		
		// configure the transformers
		for (Iterator it = transformers.iterator(); it.hasNext(); ) {
			((TransformerAction)it.next()).configure();
		}
	}
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			byte[] inValue = null;
			if (input instanceof byte[]) {
				inValue = (byte [])input;
			}
			else {
				inValue = (input == null) ? null : input.toString().getBytes();
			}
				
			for (Iterator it = transformers.iterator(); it.hasNext(); ) {
				inValue = ((TransformerAction)it.next()).go(inValue, session);
			}
			return new PipeRunResult(getForward(), inValue == null ? null : new String(inValue));
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e); 
		}
	}
	
	/**
	 * The pipe supports several actions. All actions are implementations in
	 * inner-classes that implement the Transformer interface.
	 */
	private interface TransformerAction {
		/* 
		 * @see nl.nn.adapterframework.core.IPipe#configure()
		 */
		void configure() throws ConfigurationException;
		/*
		 * transform the in and return the result
		 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
		 */
		byte[] go(byte[] in, PipeLineSession session) throws Exception;
	}
	
	/**
	 * Encodes the input 
	 */
	private class Encoder implements TransformerAction {
		public BASE64Encoder encoder = new BASE64Encoder();
		public void configure() {}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			return encoder.encode(in).getBytes();
		}
	}
	
	/**
	 * Decodes the input
	 */
	private class Decoder implements TransformerAction {
		public BASE64Decoder decoder = new BASE64Decoder();
		public void configure() {}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			return decoder.decodeBuffer(in == null ? null : new String(in));
		}
	}

	/**
	 * Write the input to a file in the specified directory.
	 */
	private class FileWriter implements TransformerAction {
		private boolean append = false;
		public FileWriter(boolean append) {
			this.append = append;
		}
		// create the directory structure if not exists and
		// check the permissions
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(getDirectory())) {
				File file = new File(getDirectory());
				if (!file.exists()) {
					file.mkdirs();
				} 
				else if (!(file.isDirectory() && file.canWrite())) {
					throw new ConfigurationException(directory + " is not a directory, or no write permission");
				}
			}
			else {
				throw new ConfigurationException("directory is not specified");
			}
		}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			File tmpFile;
			File dirFile = new File(getDirectory());
			
			if (StringUtils.isEmpty(fileName)) {
				if (StringUtils.isEmpty(fileNameSessionKey)) {
				tmpFile = File.createTempFile("ibis", writeSuffix, dirFile);
			} else {
					tmpFile = new File(getDirectory()+File.separator+session.get(fileNameSessionKey));
				}
			} else {
				tmpFile = new File(getDirectory()+File.separator+fileName);
			}
			// Use tmpFile.getPath() instead of tmpFile to be WAS 5.0 / Java 1.3 compatible
			FileOutputStream fos = new FileOutputStream(tmpFile.getPath(), append);
			
			try {
				fos.write(in);
			}
			finally {
				fos.close();
			}
			
			return tmpFile.getName().getBytes();
		}
	}

	/**
	 * Reads the file, which name is specified in the input, from the specified directory.
	 * The class supports the deletion of the file after reading.
	 */
	private class FileReader implements TransformerAction {
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
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
			else {
				throw new ConfigurationException("directory is not specified");
			}
		}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			File file = new File(getDirectory(), new String(in));
			FileInputStream fis = new FileInputStream(file);
			
			try {
				byte[] result = new byte[fis.available()];
				fis.read(result);
				return result;
			}
			finally {
				fis.close();

				if (deleteAfterRead)
					file.delete();					
			}
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
			else {
				throw new ConfigurationException("directory is not specified");
			}
		}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			File file;
			if (getFileName() == null) {
				file = new File(getDirectory(), new String(in));
			} else {
				file = new File(getDirectory(), getFileName());
			}
			file.delete();
			return in;
		}
	}

	/**
	 * @return all the actions the pipe has to do
	 */
	public String getActions() {
		return actions;
	}

	/**
	 * @return the directory in which the file resides or has to be created
	 */
	public String getDirectory() {
		return directory;
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

	/**
	 * @param directory in which the file resides or has to be created
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * @return suffix of the file that is written
	 */
	public String getWriteSuffix() {
		return writeSuffix;
	}

	/**
	 * @param suffix of the file that is written
	 */
	public void setWriteSuffix(String suffix) {
		this.writeSuffix = suffix;
	}

	/**
	 * @return suffix of the file that is written
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param suffix of the file that is written
	 */
	public void setFileName(String filename) {
		this.fileName = filename;
	}

	/**
	 * @return the session key that contains the name of the file to be created
	 */
	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	/**
	 * @param the session key that contains the name of the file to be created
	 */
	public void setFileNameSessionKey(String filenameSessionKey) {
		this.fileNameSessionKey = filenameSessionKey;
	}
}
