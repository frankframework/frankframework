/*
 * $Log: Result2Filewriter.java,v $
 * Revision 1.3  2005-10-24 09:59:23  europe\m00f531
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Resulthandler that writes the transformed record to a file
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.Result2Filewriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory in which the resultfile must be stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilenamePattern(String) filenamePattern}</td><td>Name of the file is created using the MessageFormat. Params: 1=inputfilename, 2=extension of file, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterFinalize(String) move2dirAfterFinalize}</td><td>Directory to which the created file must be moved after finalization (is optional)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultResultHandler(boolean) defaultResultHandler}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class Result2Filewriter extends AbstractResultHandler {
	public static final String version = "$RCSfile: Result2Filewriter.java,v $  $Revision: 1.3 $ $Date: 2005-10-24 09:59:23 $";
	private static Logger log = Logger.getLogger(Result2Filewriter.class);
	
	private String outputDirectory;
	private String move2dirAfterFinalize;
	private String filenamePattern;
	private boolean defaultResultHandler;
	private Map openWriters;
	
	public Result2Filewriter() {
		openWriters = Collections.synchronizedMap(new HashMap());
	}
	
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

	private void write(PipeLineSession session, String inputFilename, String[] lines) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, inputFilename, true);
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
	
	public void writePrefix(PipeLineSession session, String inputFilename, boolean mustPrefix, boolean hasPreviousRecord) throws Exception {
		String[] prefix = prefix(mustPrefix, hasPreviousRecord);
		if (prefix != null) {
			write(session, inputFilename, prefix);
		}
	}

	public void writeSuffix(PipeLineSession session, String inputFilename) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, inputFilename, false);
		if (bw != null && ! StringUtils.isEmpty(getSuffix())) {
			write(session, inputFilename, getSuffix());
		}
	}

	private BufferedWriter getBufferedWriter(PipeLineSession session, String inputFilename, boolean openIfNotOpen) throws IOException, ParameterException {
		FileOutput fo = (FileOutput)openWriters.get(inputFilename);
		if (fo != null) {
			return fo.writer;
		}
		
		if (! openIfNotOpen) {
			return null ;
		}
		
		String outputFilename = FileUtils.getFilename(null, session, new File(inputFilename), filenamePattern);
		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists() && outputFile.isFile()) {
			log.warn("Outputfile " + outputFilename + " exists in " + outputDirectory);
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false));
		openWriters.put(inputFilename, new FileOutput(bw, outputFile));
		return bw;		
	}
	
	public boolean isDefault() {
		return defaultResultHandler;
	}

	public void setDefault(boolean isDefault) {
		this.defaultResultHandler = isDefault;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public String getFilenamePattern() {
		return filenamePattern;
	}

	public void setOutputDirectory(String string) {
		outputDirectory = string;
	}

	public void setFilenamePattern(String string) {
		filenamePattern = string;
	}

	public String getMove2dirAfterFinalize() {
		return move2dirAfterFinalize;
	}

	public void setMove2dirAfterFinalize(String string) {
		move2dirAfterFinalize = string;
	}

	private class FileOutput {
		private BufferedWriter writer;
		private File file;
		private FileOutput(BufferedWriter writer, File file) {
			this.writer = writer;
			this.file = file;
		}
	}
}
