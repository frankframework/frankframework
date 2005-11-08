/*
 * $Log: CompressPipe.java,v $
 * Revision 1.4  2005-11-08 09:31:08  europe\m00f531
 * Bug concerning filenames resolved
 *
 * Revision 1.3  2005/11/08 09:18:54  John Dekker <john.dekker@ibissource.org>
 * Bug concerning filenames resolved
 *
 * Revision 1.2  2005/10/28 09:36:12  John Dekker <john.dekker@ibissource.org>
 * Add possibility to convert result to a string or a bytearray
 *
 * Revision 1.1  2005/10/28 09:12:23  John Dekker <john.dekker@ibissource.org>
 * Pipe for compression (Zip or GZip)
 *
 */
package nl.nn.adapterframework.batch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Pipe to zip or unzip a message or file.  
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.BatchFileTransformerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageIsContent(boolean) messageIsContent}</td><td>Flag indicates whether the message is the content or the path to a file with the contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResultIsContent(boolean) resultIsContent}</td><td>Flag indicates whether the result must be written to the message or to a file (filename = message)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Required if result is a file, the firectory in which to store the result file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setfilenamePattern(String) filenamePattern}</td><td>Required if result is a file, the pattern for the result filename</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setZipEntryPattern(String) zipEntryPattern}</td><td>The pattern for the zipentry name in case a zipfile is read or written</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCompress(boolean) directory}</td><td>If true the pipe compress, otherwise it decompress</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConvert2String(boolean) convert2String}</td><td>If true result is returned as a string, otherwise as a byte array</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class CompressPipe extends FixedForwardPipe {
	private boolean messageIsContent;
	private boolean resultIsContent;
	private String outputDirectory;
	private String filenamePattern;
	private String zipEntryPattern;
	private boolean compress;
	private Object result;
	private boolean convert2String;
	
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			InputStream in = getInputStream(input, session);
			OutputStream out = getOutputStream(input, session);
			
			try {
				int readLength = 0;
				byte[] block = new byte[4096];
				while ((readLength = in.read(block)) > 0) {
					 out.write(block, 0, readLength);
				}
			}
			finally {
				out.close();
				in.close();
			}
			return new PipeRunResult(getForward(), getResultMsg());
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Unexpected exception during compression", e);
		}
	}
	
	private InputStream getInputStream(Object input, PipeLineSession session) throws IOException, ParameterException {
		InputStream in = null;
		
		if (messageIsContent) {
			if (input instanceof byte[]) {
				in = new ByteArrayInputStream((byte[])input); 
			}
			else {
				in = new ByteArrayInputStream(input.toString().getBytes()); 
			}
			if (! compress) {
				in = new GZIPInputStream(in);
			}
		}
		else {
			in = new FileInputStream((String)input);
			if (! compress) {
				ZipInputStream zipper = new ZipInputStream(in);
				String zipEntryName = getZipEntryName(input, session);
				while (! zipper.getNextEntry().getName().equals(zipEntryName));
				in = zipper;
			}
		}
		return in;
	}

	private OutputStream getOutputStream(Object input, PipeLineSession session) throws ParameterException, IOException {
		OutputStream out = null;
		if (resultIsContent) {
			out = new ByteArrayOutputStream();
			result = out; 

			if (compress) {
				out = new GZIPOutputStream(out);
			}
		}
		else {
			String outFilename = null;
			if (messageIsContent) {
				outFilename = FileUtils.getFilename(getParameterList(), session, (File)null, filenamePattern);
			}
			else {
				outFilename = FileUtils.getFilename(getParameterList(), session, new File((String)input), filenamePattern);
			}
			File outFile = new File(outputDirectory, outFilename);
			result = outFile.getAbsolutePath();
			out =  new FileOutputStream(outFile);
			
			if (compress) {
				ZipOutputStream zipper = new ZipOutputStream(out); 
				String zipEntryName = getZipEntryName(input, session);
				zipper.putNextEntry(new ZipEntry(zipEntryName));
				out = zipper;
			}
		}
		return out;
	}
	
	private Object getResultMsg() {
		if (resultIsContent) {
			if (convert2String)
				return ((ByteArrayOutputStream)result).toString();
			return ((ByteArrayOutputStream)result).toByteArray();
		}
		return result;
	}
	
	private String getZipEntryName(Object input, PipeLineSession session) throws ParameterException {
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

}
