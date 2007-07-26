/*
 * $Log: Result2Filewriter.java,v $
 * Revision 1.9  2007-07-26 16:12:06  europe\L190409
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


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
public class Result2Filewriter extends AbstractResultHandler {
	public static final String version = "$RCSfile: Result2Filewriter.java,v $  $Revision: 1.9 $ $Date: 2007-07-26 16:12:06 $";
	private static Logger log = LogUtil.getLogger(Result2Filewriter.class);
	
	private String outputDirectory;
	private String move2dirAfterFinalize;
	private String filenamePattern;
	private boolean defaultResultHandler;
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	
	public void handleResult(PipeLineSession session, String inputFilename, String recordKey, Object result) throws Exception {
		if (result instanceof String) {
			write(session, inputFilename, (String)result);
		}
		else if (result instanceof String[]) {
			write(session, inputFilename, (String[])result);
		}
	}
	
	private void write(PipeLineSession session, String inputFilename, String line) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, inputFilename, true);
		bw.write(line);
		bw.newLine();
	}

	private void write(PipeLineSession session, String streamId, String[] lines) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, streamId, true);
		for (int i = 0; i < lines.length; i++) {
			bw.write(lines[i]);
			bw.newLine();
		}
	}
	
	public Object finalizeResult(PipeLineSession session, String inputFilename, boolean error) throws IOException {
		FileOutput fo = (FileOutput)openWriters.remove(inputFilename);
		if (fo != null) {
			BufferedWriter bw = fo.writer;
			bw.close();
		}
		else {
			return null;
		}
		
		File file = fo.file;
		if (error) {
			file.delete();
			return null;
		}
		else {
			if (! StringUtils.isEmpty(move2dirAfterFinalize)) {
				File movedFile = new File(move2dirAfterFinalize, file.getName());
				if (movedFile.exists()) {
					log.warn("File " + movedFile.getAbsolutePath() + " exists, file gets overwritten");
					movedFile.delete();
				}
				file.renameTo(movedFile);
				return movedFile.getAbsolutePath();
			}
			return file.getAbsolutePath();
		}
	}
	
	public void writePrefix(PipeLineSession session, String streamId, boolean mustPrefix, boolean hasPreviousRecord) throws Exception {
		String[] prefix = prefix(mustPrefix, hasPreviousRecord);
		if (prefix != null) {
			write(session, streamId, prefix);
		}
	}

	public void writeSuffix(PipeLineSession session, String streamId) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, streamId, false);
		if (bw != null && ! StringUtils.isEmpty(getSuffix())) {
			write(session, streamId, getSuffix());
		}
	}

	private class FileOutput {
		private BufferedWriter writer;
		private File file;
		private FileOutput(BufferedWriter writer, File file) {
			this.writer = writer;
			this.file = file;
		}
	}

	private BufferedWriter getBufferedWriter(PipeLineSession session, String streamId, boolean openIfNotOpen) throws IOException, ParameterException {
		FileOutput fo = (FileOutput)openWriters.get(streamId);
		if (fo != null) {
			return fo.writer;
		}
		
		if (! openIfNotOpen) {
			return null ;
		}
		
		String outputFilename = FileUtils.getFilename(null, session, new File(streamId), filenamePattern);
		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists() && outputFile.isFile()) {
			log.warn("Outputfile " + outputFilename + " exists in " + outputDirectory);
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false));
		openWriters.put(streamId, new FileOutput(bw, outputFile));
		return bw;		
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

	public void setDefault(boolean isDefault) {
		this.defaultResultHandler = isDefault;
	}
	public boolean isDefault() {
		return defaultResultHandler;
	}



}
