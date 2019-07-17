/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.extensions.aspose.services.util.FileUtil;

/**
 * @author Gerard van der Hoorn
 */
//@Singleton
//@Startup
public class CleanupTmpDirStartup {

	private static final Logger LOGGER = Logger.getLogger(CleanupTmpDirStartup.class);

	//location of converted pdf files
	private String pdfOutputlocation = "C:/aspose/2pdf";;

	//	@PostConstruct
	public void cleanupTmpDirAtStartup() {
		LOGGER.debug("Cleanup tmpdir at startup!");
		System.out.println("CleanupTmpDirStartup.cleanupTmpDirAtStartup()");
		File pdfOutputLocationDir = new File(pdfOutputlocation);

		// Check if directory exists otherwise create it.
		if (!pdfOutputLocationDir.exists()) {
			// It does not exist create it.
			if (!pdfOutputLocationDir.mkdir()) {
				// Creation failed.
				String msg = "Could not created directory (pdfOutputlocation) : " + pdfOutputLocationDir;
				LOGGER.fatal(msg);
			} else {
				LOGGER.info(
						"Created pdfOutput directory because it didn't exist (location: " + pdfOutputLocationDir + ")");
			}
		}

		// Delete any (old) files. 
		if (pdfOutputLocationDir.exists()) {
			try {
				for (String filename : pdfOutputLocationDir.list()) {
					LOGGER.info("File deleted from tmpDir: " + filename);
				}
				FileUtil.deleteDirectoryContents(pdfOutputLocationDir);
			} catch (IOException e) {
				LOGGER.warn("Cleanup tmpdir at startup failed!", e);
				throw new RuntimeException(e);
			}
		}
	}

}
