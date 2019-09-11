package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aspose.words.Document;
import com.aspose.words.FolderFontSource;
import com.aspose.words.FontSettings;
import com.aspose.words.FontSourceBase;

public class Fontsetter {
	private static final String FONTS_RESOURCE_DIR = "/fonts/";
	private String fontFilesLocation = null;

	public Fontsetter(String pdfOutputLocation) {
		fontFilesLocation = pdfOutputLocation;
	}

	/**
	 * Set Fontsettings for this particular document. Cf.
	 * https://forum.aspose.com/t/fonts-not-found-on-unix-java-words-or-email-pdf/175052
	 * 
	 * @param doc
	 */
	public void setFontSettings(Document doc) {
		String pathToFonts = fontFilesLocation + FONTS_RESOURCE_DIR;
		FontSettings fontSettings = new FontSettings();

		// Retrieve the array of environment-dependent font sources that are searched by
		// default.
		// For example this will contain a "Windows\Fonts\" source on a Windows
		// machines.
		// We add this array to a new ArrayList to make adding or removing font entries
		// much easier.
		List<FontSourceBase> fontSources = new ArrayList<>(
				Arrays.asList(FontSettings.getDefaultInstance().getFontsSources()));

		// Add a new folder source which will instruct Aspose.Words to search the
		// following folder for fonts.
		FolderFontSource folderFontSource = new FolderFontSource(pathToFonts, false);
		// com.aspose.pdf.FolderFontSource
		// Add the custom folder which contains our fonts to the list of existing font
		// sources.
		fontSources.add(folderFontSource);

		// Convert the list of source back into a primitive array of FontSource objects.
		FontSourceBase[] updatedFontSources = fontSources.toArray(new FontSourceBase[fontSources.size()]);

		// Apply the new set of font sources to use.
		fontSettings.setFontsSources(updatedFontSources);
		doc.setFontSettings(fontSettings);
	}

}
