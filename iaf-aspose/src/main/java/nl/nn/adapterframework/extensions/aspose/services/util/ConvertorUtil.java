/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.util;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 *
 */
public class ConvertorUtil {

	private static final String DEFAULT_FILENAME = "default_filename";
	private static final String EXTENSION_DELIMITER = ".";
	public static final String PDF_FILETYPE = "pdf";

	private ConvertorUtil() {

	}

	/**
	 * Creates a filename.
	 * If the file already contains a file extension it will be removed.
	 * @param filename
	 * @return
	 */
	public static String createTidyNameWithoutExtension(String argFilename) {
		String filename = argFilename;
		if (filename == null) {
			filename = DEFAULT_FILENAME;
		}
		if (filename.contains(".")) {
			// Remove the file type.
			filename = filename.substring(0, filename.lastIndexOf("."));
		}

		return filename;
	}

	/**
	 * Creates a filename which always contains pdf as file type.
	 * If the file already contains a extension it will be replaced with pdf.
	 * @param filename
	 * @return
	 */
	public static String createTidyPdfFilename(String argFilename) {
		return createTidyFilename(argFilename, PDF_FILETYPE);
	}

	/**
	 * Creates a filename which always contains the given extension as file type (without period).
	 * If the file already contains a extension it will be replaced with the given extension.
	 * @param filename
	 * @param extension (without the period).
	 * @return
	 */
	public static String createTidyFilename(String argFilename, String extension) {
		String extensionWithDelim = EXTENSION_DELIMITER + extension;
		String filename = createTidyNameWithoutExtension(argFilename);
		if (!filename.endsWith(extensionWithDelim)) {
			filename = filename + extensionWithDelim;
		}
		return filename;
	}

}
