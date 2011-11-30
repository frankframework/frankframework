/*
 * $Log: Result2Filewriter.java,v $
 * Revision 1.19  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.17  2010/01/27 13:34:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added documentation for attribute blockByRecordType
 *
 * Revision 1.16  2007/09/24 14:55:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.15  2007/09/24 13:02:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.14  2007/09/19 13:01:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added openDocument() and closeDocument()
 *
 * Revision 1.13  2007/09/17 08:24:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.12  2007/09/11 11:51:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.11  2007/09/05 13:02:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2007/08/03 08:39:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;


/**
 * Resulthandler that writes the transformed record to a file.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.Result2Filewriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record. <br/>N.B. If a suffix is set without a prefix, it is only used at the end of processing (i.e. at the end of the file) as a final close</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&lt;#name#&gt;</td></tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&lt;/#name#&gt;</td></tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>#name#</td></tr>
 * <tr><td>{@link #setBlockByRecordType(boolean) blockByRecordType}</td><td>when set <code>true</code>(default), every group of records, as indicated by {@link IRecordHandler.isNewRecordType RecordHandler.newRecordType} is handled as a block.</td><td>true</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory in which the resultfile must be stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilenamePattern(String) filenamePattern}</td><td>Name of the file is created using the MessageFormat. Params: 1=inputfilename, 2=extension of file, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterFinalize(String) move2dirAfterFinalize}</td><td>Directory to which the created file must be moved after finalization (is optional)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 */
public class Result2Filewriter extends ResultWriter {
	
	private String outputDirectory;
	private String move2dirAfterFinalize;
	private String filenamePattern;
	
	private Map openFiles = Collections.synchronizedMap(new HashMap());
	
	public Result2Filewriter() {
		super();
		setOnOpenDocument("");
		setOnCloseDocument("");
	}
	
	protected Writer createWriter(PipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		log.debug("create writer ["+streamId+"]");
		String outputFilename = FileUtils.getFilename(null, session, new File(streamId), getFilenamePattern());
		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists() && outputFile.isFile()) {
			log.warn("Outputfile " + outputFilename + " exists in " + outputDirectory);
		}
		openFiles.put(streamId,outputFile);
		return new FileWriter(outputFile, false);
	}

	public void closeDocument(PipeLineSession session, String streamId, ParameterResolutionContext prc) {
		File outputFile = (File)openFiles.remove(streamId);
	}

	public Object finalizeResult(PipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception {
		log.debug("finalizeResult ["+streamId+"]");
		super.finalizeResult(session,streamId, error, prc);
		super.closeDocument(session,streamId, prc);
		
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
