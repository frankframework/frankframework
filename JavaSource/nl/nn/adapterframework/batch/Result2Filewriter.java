/*
 * $Log: Result2Filewriter.java,v $
 * Revision 1.2  2005-10-17 11:46:35  europe\m00f531
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
	public static final String version = "$RCSfile: Result2Filewriter.java,v $  $Revision: 1.2 $ $Date: 2005-10-17 11:46:35 $";
	private static Logger log = Logger.getLogger(Result2Filewriter.class);
	
	private String outputDirectory;
	private String move2dirAfterFinalize;
	private String filenamePattern;
	private boolean defaultResultHandler;
	private Map openWriters;
	
	public Result2Filewriter() {
		openWriters = Collections.synchronizedMap(new HashMap());
	}
	
	public void handleResult(PipeLineSession session, String inputFilename, String recordKey, Object result) throws IOException {
		if (result instanceof String) {
			write(inputFilename, (String)result);
		}
		else if (result instanceof String[]) {
			write(inputFilename, (String[])result);
		}
	}
	
	private void write(String inputFilename, String line) throws IOException {
		BufferedWriter bw = getBufferedWriter(inputFilename, true);
		bw.write(line);
		bw.newLine();
	}

	private void write(String inputFilename, String[] lines) throws IOException {
		BufferedWriter bw = getBufferedWriter(inputFilename, true);
		for (int i = 0; i < lines.length; i++) {
			bw.write(lines[i]);
			bw.newLine();
		}
	}
	
	public Object finalizeResult(PipeLineSession session, String inputFilename, boolean error) throws IOException {
		BufferedWriter bw = (BufferedWriter)openWriters.remove(inputFilename);
		if (bw != null) {
			bw.close();
		}
		else {
			return null;
		}
		
		String outputFile = FileUtils.getFilename(new File(inputFilename), filenamePattern, false);
		File file = new File(outputDirectory, outputFile);
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
	
	public void writePrefix(PipeLineSession session, String inputFilename, boolean mustPrefix, boolean hasPreviousRecord) throws IOException {
		String[] prefix = prefix(mustPrefix, hasPreviousRecord);
		if (prefix != null) {
			write(inputFilename, prefix);
		}
	}

	public void writeSuffix(PipeLineSession session, String inputFilename) throws Exception {
		BufferedWriter bw = getBufferedWriter(inputFilename, false);
		if (bw != null && ! StringUtils.isEmpty(getSuffix())) {
			write(inputFilename, getSuffix());
		}
	}

	private BufferedWriter getBufferedWriter(String inputFilename, boolean openIfNotOpen) throws IOException {
		BufferedWriter bw = null;
		bw = (BufferedWriter)openWriters.get(inputFilename);
		if (bw != null) {
			return bw;
		}
		
		if (! openIfNotOpen) {
			return null ;
		}
		
		String outputFilename = FileUtils.getFilename(new File(inputFilename), filenamePattern, true);
		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists() && outputFile.isFile()) {
			log.warn("Outputfile " + outputFilename + " exists in " + outputDirectory);
		}
		bw = new BufferedWriter(new FileWriter(outputFile, false));
		openWriters.put(inputFilename, bw);
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

}
