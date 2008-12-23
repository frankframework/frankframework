/*
 * $Log: BatchFileTransformerPipe.java,v $
 * Revision 1.16  2008-12-23 12:50:25  m168309
 * added storeOriginalBlock attribute
 *
 * Revision 1.15  2008/02/19 09:23:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.14  2008/02/15 16:05:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added default manager and flow, for simple configurations
 *
 * Revision 1.13  2008/02/15 13:57:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attributes numberOfBackups, overwrite and delete
 *
 * Revision 1.12  2007/09/11 11:51:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.11  2007/09/04 09:34:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * check type of input message
 *
 * Revision 1.10  2007/09/04 07:57:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix bug in rename after transform
 *
 * Revision 1.9  2007/07/26 16:07:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2007/07/24 16:10:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * new style BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Pipe for transforming a (batch)file with records. Records in the file must be separated
 * with new line characters.
 * You can use the &lt;child&gt; tag to register RecordHandlers, RecordHandlerManagers, ResultHandlers
 * and RecordHandlingFlow elements. This is deprecated, however. Since 4.7 one should use &lt;manager&gt;,
 * &lt;recordHandler&gt;, &lt;resultHandler&gt; and &lt;flow&gt;
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.BatchFileTransformerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterTransform(String) move2dirAfterTransform}</td><td>Directory in which the transformed file(s) is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterError(String) move2dirAfterError}</td><td>Directory to which the inputfile is moved in case an error occurs</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(int) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>5</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>when set <code>true</code>, the destination file will be deleted if it already exists</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>, the file processed will deleted after being processed, and not stored</td><td>false</td></tr>
 * <tr><td>{@link #setStoreOriginalBlock(boolean) storeOriginalBlock}</td><td>when set <code>true</code> the original block is stored under the session key originalBlock</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.RecordHandlingFlow manager/flow}</td><td>Element that contains the handlers for a specific record type, to be assigned to the manager</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IRecordHandler recordHandler}</td><td>Handler for transforming records of a specific type</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.batch.IResultHandler resultHandler}</td><td>Handler for processing transformed records</td></tr>
 * </table>
 * </p>
 * 
 * 
 * For files containing only a single type of lines, a simpler configuration without managers and flows
 * can be specified. A single recordHandler with key="*" and (optional) a single resultHandler need to be specified.
 * Each line will be handled by this recordHandler and resultHandler.
 * 
 * @author  John Dekker
 * @version Id
 */
public class BatchFileTransformerPipe extends StreamTransformerPipe {
	public static final String version = "$RCSfile: BatchFileTransformerPipe.java,v $  $Revision: 1.16 $ $Date: 2008-12-23 12:50:25 $";

	private String move2dirAfterTransform;
	private String move2dirAfterError;
	private int numberOfBackups = 5;
	private boolean overwrite = false;
	private boolean delete = false;


	protected String getStreamId(Object input, PipeLineSession session) throws PipeRunException {
		return ((File)input).getName();
	}
	protected Reader getReader(String streamId, Object input, PipeLineSession session) throws PipeRunException {
		try {
			return new FileReader(((File)input));
		} catch (FileNotFoundException e) {
			throw new PipeRunException(this,"cannot find file ["+streamId+"]",e);
		}
	}
	

	/**
	 * Open a reader for the file named according the input messsage and 
	 * transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this,"got null input instead of String containing filename");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(this,"expected String containing filename as input, got ["+ClassUtils.nameOf(input)+"], value ["+input+"]");
		}
		String filename	= input.toString();
		File file = new File(filename);

		try {
			PipeRunResult result = super.doPipe(file,session);
			try {
				FileUtils.moveFileAfterProcessing(file, getMove2dirAfterTransform(), isDelete(),isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				log.error(getLogPrefix(session),e);
			}
			return result;
		} catch (PipeRunException e) {
			try {
				FileUtils.moveFileAfterProcessing(file, getMove2dirAfterError(), isDelete(),isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e2) {
				log.error(getLogPrefix(session)+"Could not move file after exception ["+e2+"]");
			}
			throw e;
		}
	}

	
	/**
	 * @param readyDir directory where input file is moved to in case of a succesful transformation
	 */
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}
	public String getMove2dirAfterTransform() {
		return move2dirAfterTransform;
	}

	/**
	 * @param errorDir directory where input file is moved to in case of an error
	 */
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}
	public String getMove2dirAfterError() {
		return move2dirAfterError;
	}


	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	public void setOverwrite(boolean b) {
		overwrite = b;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	public void setDelete(boolean b) {
		delete = b;
	}
	public boolean isDelete() {
		return delete;
	}

}
