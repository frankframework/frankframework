/*
 * $Log: UnzipPipe.java,v $
 * Revision 1.1  2008-06-26 12:51:49  europe\L190409
 * fisrt version
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;

/**
 * Assumes input to be a ZIP archive, and unzips it to a directory.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedResult</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setDirectory(String) directory}</td>        <td>directory to extract the archive to</td><td>&nbsp;</td></tr>
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
 * @version Id
 * @since   4.9
 * @author  Gerrit van Brakel
 */
public class UnzipPipe extends FixedForwardPipe {
	public static final String version="$RCSfile: UnzipPipe.java,v $ $Revision: 1.1 $ $Date: 2008-06-26 12:51:49 $";
	
    private String directory;
    
    public void configure() throws ConfigurationException {
    	super.configure();
    	if (StringUtils.isEmpty(getDirectory())) {
    		throw new ConfigurationException(getLogPrefix(null)+"directory must be specified");
    	}
    	File dir= new File(getDirectory());
    	if (!dir.exists()) {
			throw new ConfigurationException(getLogPrefix(null)+"directory ["+getDirectory()+"] does not exist");
    	}
		if (!dir.isDirectory()) {
			throw new ConfigurationException(getLogPrefix(null)+"directory ["+getDirectory()+"] is not a directory");
		}
    }
    
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		InputStream in;
		if (input instanceof InputStream) {
			in=(InputStream)input;
		} else {
			String filename=(String)input;
			try {
				in=new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				throw new PipeRunException(this, "could not find file ["+filename+"]",e);
			}
		}
		ZipInputStream zis = new ZipInputStream(in);
		try {
			while (zis.available()>0) {
				ZipEntry ze=zis.getNextEntry();
				String filename=getDirectory()+ze.getName();
				FileOutputStream fos = new FileOutputStream(filename);
				Misc.streamToStream(zis,fos);
				zis.closeEntry();				
			}
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot unzip",e);
		}
		return new PipeRunResult(getForward(),getDirectory());
	}

	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}

}
