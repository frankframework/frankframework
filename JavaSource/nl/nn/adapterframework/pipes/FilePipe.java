/*
 * $Log: FilePipe.java,v $
 * Revision 1.1  2004-04-26 11:51:34  a1909356#db2admin
 * Support for file en- and decoding
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.helpers.Transform;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author J. Dekker
 * @version Id
 *
 * FilePipe allows to write to or read from a file. 
 * Write will create a file in the specified directory. The name is a concatenation
 * of the correlation id, a string that guarantees the filename to be unique and the
 * specified writeSuffic.
 * The pipe also support base64 en- and decoding.
 */
public class FilePipe extends FixedForwardPipe {
	public static final String version="$Id: FilePipe.java,v 1.1 2004-04-26 11:51:34 a1909356#db2admin Exp $";
	private List transformers;
	protected String actions;
	protected String directory;
	protected String writeSuffix;

	/* 
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
			
			Transformer t = null;
			if ("write".equalsIgnoreCase(token))
				transformers.add(new FileWriter());
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
		
		// configure the transformers
		for (Iterator it = transformers.iterator(); it.hasNext(); ) {
			((Transformer)it.next()).configure();
		}
	}
	
	/* 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			byte[] inValue = (input == null) ? null : input.toString().getBytes();
			for (Iterator it = transformers.iterator(); it.hasNext(); ) {
				inValue = ((Transformer)it.next()).go(inValue, session);
			}
			return new PipeRunResult(getForward(), inValue == null ? null : new String(inValue));
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e); 
		}
	}
	
	/**
	 * The pipe supports several actions. All actions are implementations of the
	 * Transformer interface.
	 */
	private interface Transformer {
		/* 
		 * @see nl.nn.adapterframework.core.IPipe#configure()
		 */
		void configure() throws ConfigurationException;
		/*
		 * do the transformation
		 */
		byte[] go(byte[] in, PipeLineSession session) throws Exception;
	}
	
	/**
	 * Encodes the input
	 */
	private class Encoder implements Transformer {
		public BASE64Encoder encoder = new BASE64Encoder();
		public void configure() {}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			return encoder.encode(in).getBytes();
		}
	}
	
	/**
	 * Decodes the input
	 */
	private class Decoder implements Transformer {
		public BASE64Decoder decoder = new BASE64Decoder();
		public void configure() {}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			return decoder.decodeBuffer(in == null ? null : new String(in));
		}
	}

	/**
	 * Write the input to a file in the specified directory.
	 */
	private class FileWriter implements Transformer {
		// create the directory structure if not exists and
		// check the permissions
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(directory)) {
				File file = new File(directory);
				if (!file.exists()) {
					file.mkdirs();
				} 
				else if (!(file.isDirectory() && file.canWrite())) {
					throw new ConfigurationException(directory + " is not a directory, or no write permission");
				}
			}
		}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			File dirFile = new File(directory);
			File tmpFile = File.createTempFile((String)session.getMessageId(), writeSuffix, dirFile);
			FileOutputStream fos = new FileOutputStream(tmpFile);
			
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
	private class FileReader implements Transformer {
		private boolean deleteAfterRead;
		
		FileReader() {
			deleteAfterRead = false;
		}
		FileReader(boolean deleteAfterRead) {
			this.deleteAfterRead = deleteAfterRead;
		}
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(directory)) {
				File file = new File(directory);
				if (! (file.exists() && file.isDirectory() && file.canRead())) {
					throw new ConfigurationException(directory + " is not a directory, or no read permission");
				}
			}
		}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			File file = new File(directory, new String(in));
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
	private class FileDeleter implements Transformer {
		public void configure() throws ConfigurationException {
			if (StringUtils.isNotEmpty(directory)) {
				File file = new File(directory);
				if (! (file.exists() && file.isDirectory())) {
					throw new ConfigurationException(directory + " is not a directory");
				}
			}
		}
		public byte[] go(byte[] in, PipeLineSession session) throws Exception {
			File file = new File(directory, new String(in));
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

}
