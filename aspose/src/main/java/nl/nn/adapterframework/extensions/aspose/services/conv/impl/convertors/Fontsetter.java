/*
   Copyright 2019 Integration Partners

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aspose.cells.IndividualFontConfigs;
import com.aspose.cells.LoadOptions;
import com.aspose.words.Document;
import com.aspose.words.FolderFontSource;
import com.aspose.words.FontSettings;
import com.aspose.words.FontSourceBase;

public class Fontsetter {
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
		FolderFontSource folderFontSource = new FolderFontSource(fontFilesLocation, false);
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

	public LoadOptions getCellsLoadOptions() {
		IndividualFontConfigs config = new IndividualFontConfigs();
		LoadOptions options = new LoadOptions();

		// Retrieve the array of environment-dependent font sources that are searched by
		// default.
		// For example this will contain a "Windows\Fonts\" source on a Windows
		// machines.
		// We add this array to a new ArrayList to make adding or removing font entries
		// much easier.
		List<com.aspose.cells.FontSourceBase> fontSources = new ArrayList<>();
		if(options.getFontConfigs() != null) {
			fontSources.addAll(Arrays.asList(options.getFontConfigs().getFontSources()));
		}

		// Add a new folder source which will instruct Aspose.Words to search the
		// following folder for fonts.
		com.aspose.cells.FolderFontSource folderFontSource = new com.aspose.cells.FolderFontSource(fontFilesLocation, false);
		// com.aspose.pdf.FolderFontSource
		// Add the custom folder which contains our fonts to the list of existing font
		// sources.
		fontSources.add(folderFontSource);

		// Convert the list of source back into a primitive array of FontSource objects.
		com.aspose.cells.FontSourceBase[] updatedFontSources = fontSources.toArray(new com.aspose.cells.FontSourceBase[fontSources.size()]);

		// Apply the new set of font sources to use.
		config.setFontSources(updatedFontSources);
		options.setFontConfigs(config);
		return options;
	}

}
