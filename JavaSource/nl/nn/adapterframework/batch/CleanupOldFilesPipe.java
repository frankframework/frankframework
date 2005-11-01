/*
 * $Log: CleanupOldFilesPipe.java,v $
 * Revision 1.1  2005-11-01 08:54:00  europe\m00f531
 * Initial version
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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for deleting files
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.FtpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class CleanupOldFilesPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: CleanupOldFilesPipe.java,v $  $Revision: 1.1 $ $Date: 2005-11-01 08:54:00 $";
	private String filePattern;
	private boolean subdirectories;
	private long lastModifiedDelta;
	private _FileFilter fileFilter = new _FileFilter();
	private _DirFilter dirFilter = new _DirFilter();
		
	public CleanupOldFilesPipe() {
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (StringUtils.isEmpty(filePattern)) {
			throw new ConfigurationException("Property [move2dir] is not set");
		}
	}
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			String in = (input == null) ? "" : input.toString();
			String filename = FileUtils.getFilename(null, session, in, filePattern);
			ArrayList delFiles = getFilesForDeletion(filename);
			if (delFiles != null && delFiles.size() > 0) {
				for (Iterator fileIt = delFiles.iterator(); fileIt.hasNext();) {
					File file = (File)fileIt.next();
					file.delete();
				}
			}
			return new PipeRunResult(getForward(), input);
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while deleting file(s)", e); 
		}
	}

	private ArrayList getFilesForDeletion(String filename) {
		File file = new File(filename);
		if (file.exists()) {
			ArrayList result = new ArrayList();
			if (file.isDirectory()) {
				getFilesForDeletion(result, file);
			}
			else {
				if (fileFilter.accept(file))
					result.add(file);
			}
			return result;
		}
		return null;
	}

	private void getFilesForDeletion(ArrayList result, File directory) {
		File[] files = directory.listFiles(fileFilter);
		for (int i = 0; i < files.length; i++) {
			result.add(files[i]);
		}
		
		if (subdirectories) {
			files = directory.listFiles(dirFilter);
			for (int i = 0; i < files.length; i++) {
				getFilesForDeletion(result, files[i]);
			}		
		}
	}

	private class _FileFilter implements FileFilter {
		public boolean accept(File file) {
			if (file.isFile()) {
				if ((System.currentTimeMillis() - file.lastModified()) > lastModifiedDelta) {
					return true;
				}
			}
			return false;
		}
	}

	private class _DirFilter implements FileFilter {
		public boolean accept(File file) {
			return file.isDirectory();
		}
	}


	public void setFilePattern(String string) {
		filePattern = string;
	}

	public void setLastModifiedDelta(long l) {
		lastModifiedDelta = l;
	}

	public void setSubdirectories(boolean b) {
		subdirectories = b;
	}

}
