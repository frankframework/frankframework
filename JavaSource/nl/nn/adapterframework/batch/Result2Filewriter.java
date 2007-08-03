/*
 * $Log: Result2Filewriter.java,v $
 * Revision 1.10  2007-08-03 08:39:41  europe\L190409
 * moved basic functionality to baseclass
 *
 * Revision 1.9  2007/07/26 16:12:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.8  2007/07/24 08:00:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change inputFilename to streamId
 *
 * Revision 1.7  2007/02/12 13:37:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.6  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.4  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.3  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.2  2005/10/17 11:46:35  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;


/**
 * Resulthandler that writes the transformed record to a file.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.Result2Filewriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory in which the resultfile must be stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilenamePattern(String) filenamePattern}</td><td>Name of the file is created using the MessageFormat. Params: 1=inputfilename, 2=extension of file, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterFinalize(String) move2dirAfterFinalize}</td><td>Directory to which the created file must be moved after finalization (is optional)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultResultHandler(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 * @version Id
 */
public class Result2Filewriter extends ResultWriter {
	public static final String version = "$RCSfile: Result2Filewriter.java,v $  $Revision: 1.10 $ $Date: 2007-08-03 08:39:41 $";
	
	private String outputDirectory;
	private String move2dirAfterFinalize;
	private String filenamePattern;
	
	private Map openFiles = Collections.synchronizedMap(new HashMap());
	
	protected Writer createWriter(PipeLineSession session, String streamId) throws Exception {
		String outputFilename = FileUtils.getFilename(null, session, new File(streamId), getFilenamePattern());
		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists() && outputFile.isFile()) {
			log.warn("Outputfile " + outputFilename + " exists in " + outputDirectory);
		}
		openFiles.put(streamId,outputFile);
		return new FileWriter(outputFile, false);
	}
	
	public Object finalizeResult(PipeLineSession session, String streamId, boolean error) throws IOException {
		super.finalizeResult(session,streamId, error);
		
		File file = (File)openFiles.get(streamId);
		if (file==null) {
			return null;
		}
		if (error) {
			file.delete();
			return null;
		}
		else {
			if (!StringUtils.isEmpty(getMove2dirAfterFinalize())) {
				File movedFile = new File(getMove2dirAfterFinalize(), file.getName());
				if (movedFile.exists()) {
					log.warn("File [" + movedFile.getAbsolutePath() + "] exists, file gets overwritten");
					movedFile.delete();
				}
				file.renameTo(movedFile);
				return movedFile.getAbsolutePath();
			}
			return file.getAbsolutePath();
		}
	}

	
	public void setMove2dirAfterFinalize(String string) {
		move2dirAfterFinalize = string;
	}
	public String getMove2dirAfterFinalize() {
		return move2dirAfterFinalize;
	}

	public void setFilenamePattern(String string) {
		filenamePattern = string;
	}
	public String getFilenamePattern() {
		return filenamePattern;
	}

	public void setOutputDirectory(String string) {
		outputDirectory = string;
	}
	public String getOutputDirectory() {
		return outputDirectory;
	}
}
