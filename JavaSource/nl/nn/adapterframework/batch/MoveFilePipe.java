/*
 * $Log: MoveFilePipe.java,v $
 * Revision 1.5  2006-05-19 09:28:38  europe\m00i745
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.3  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.2  2005/10/24 09:59:22  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.1  2005/10/11 13:00:22  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.io.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for moving files to another directory.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.FtpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dir(String) dir}</td><td>destination directory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfAttempts(int) numberOfAttempts}</td><td>maximum number of attempts before throwing an exception</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWaitBeforeRetry(long) waitBeforeRetry}</td><td>Number of attempts</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class MoveFilePipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: MoveFilePipe.java,v $  $Revision: 1.5 $ $Date: 2006-05-19 09:28:38 $";

	private String move2dir;
	private long waitBeforeRetry = 1000;
	private int numberOfAttempts = 10;
	
	public MoveFilePipe() {
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (StringUtils.isEmpty(move2dir)) {
			throw new ConfigurationException("Property [move2dir] is not set");
		}
	}
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String orgFilename = input.toString();
		try {
			File srcFile = new File(orgFilename);
			File dstFile = new File(move2dir, srcFile.getName());

			if (FileUtils.moveFile(srcFile, dstFile, numberOfAttempts, waitBeforeRetry) == null) {
				throw new PipeRunException(this, "Error while moving file [" + orgFilename + "]"); 
			}
			 
			return new PipeRunResult(getForward(), dstFile.getAbsolutePath());
		}
		catch(PipeRunException e) {
			throw e;
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while moving file [" + orgFilename + "]", e); 
		}
	}

	public String getMove2dir() {
		return move2dir;
	}

	public void setMove2dir(String string) {
		move2dir = string;
	}

	public int getNumberOfAttempts() {
		return numberOfAttempts;
	}

	public long getWaitBeforeRetry() {
		return waitBeforeRetry;
	}

	public void setNumberOfAttempts(int i) {
		numberOfAttempts = i;
	}

	public void setWaitBeforeRetry(long l) {
		waitBeforeRetry = l;
	}

}
