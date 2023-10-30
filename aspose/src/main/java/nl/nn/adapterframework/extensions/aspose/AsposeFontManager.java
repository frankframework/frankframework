/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import com.aspose.cells.FontConfigs;
import com.aspose.slides.FontsLoader;
import com.aspose.words.FolderFontSource;
import com.aspose.words.FontSettings;
import com.aspose.words.FontSourceBase;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.FilenameUtils;

@Log4j2
public class AsposeFontManager {

	private static final String FONTS_RESOURCE_NAME = "/fonts.zip"; //lots of commonly used fonts
	private static final String FONTS_RESOURCE_DIR = "/fonts/";
	private static final String TRUETYPE_FONT_EXT = ".ttf";

	private File fontDirectory = null;
	private final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

	public AsposeFontManager() {
		this(null);
	}

	public AsposeFontManager(String fontsDirectory) {
		File rootDirectory = null;
		if (StringUtils.isNotEmpty(fontsDirectory)) {
			rootDirectory = new File(fontsDirectory);

			if (!rootDirectory.exists()) {
				log.warn("fontDirectory ["+fontsDirectory+"] does not exist, using ibis.tmpdir instead");
			} else {
				fontDirectory = new File(rootDirectory, FONTS_RESOURCE_DIR);
			}
		}

		// If an invalid directory was provided, fall back to the ibis default temp directory
		if (fontDirectory == null) {
			String tmpdir = FileUtils.getTempDirectory();
			fontDirectory = new File(tmpdir, FONTS_RESOURCE_DIR);
		}

		// If this fonts (sub-)directory does not exist, try to create it
		if(!fontDirectory.exists()) {
			fontDirectory.mkdirs();
		}
	}

	public void load() throws IOException {
		load(false);
	}

	public void load(boolean unzipArchive) throws IOException {
		if(unzipArchive) {
			unpackDefaultFontArchive();
		}

		loadFonts(); //load all the newly unpacked fonts into the systems GraphicsEnvironment

		loadFontsForWord();
		loadFontsForSlides();
		loadFontsForCells();
	}

	/** unpack the fonts.zip archive in the supplied font directory. Does not override existing files */
	public void unpackDefaultFontArchive() throws IOException {
		URL fontsUrl = ClassLoaderUtils.getResourceURL(FONTS_RESOURCE_NAME);
		if(fontsUrl == null) {
			throw new IllegalStateException("font archive ["+FONTS_RESOURCE_NAME+"] cannot be found");
		}

		try (InputStream inputStream = new BufferedInputStream(fontsUrl.openStream())) {
			Path fontsDirPath = Paths.get(fontDirectory.toURI());

			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				while (true) {
					ZipEntry entry = zipInputStream.getNextEntry();
					if (entry == null) {
						break;
					}

					String filename = FilenameUtils.normalize(entry.getName(), true);
					if(filename != null) {
						Path target = fontsDirPath.resolve(filename);
						if (Files.notExists(target)) {
							Files.copy(zipInputStream, target);
						}
					}
					zipInputStream.closeEntry();
				}
			}

		} catch (IOException e) {
			log.error("unpacking fonts [{}] to directory [{}] failed!", FONTS_RESOURCE_NAME, fontDirectory, e);
			throw e;
		}
	}

	private void loadFontsForWord() {
		// Retrieve the array of environment-dependent font sources that are searched by default.
		// For example this will contain a "Windows\Fonts\" source on a Windows machines.
		// We add this array to a new ArrayList to make adding or removing font entries much easier.
		List<FontSourceBase> fontSources = new ArrayList<>(
				Arrays.asList(FontSettings.getDefaultInstance().getFontsSources()));

		// Add a new folder source which will instruct Aspose.Words to search the
		// following folder for fonts.
		FolderFontSource folderFontSource = new FolderFontSource(getFontsPath(), false);
		// com.aspose.pdf.FolderFontSource
		// Add the custom folder which contains our fonts to the list of existing font sources.
		fontSources.add(folderFontSource);

		// Convert the list of source back into a primitive array of FontSource objects.
		FontSourceBase[] updatedFontSources = fontSources.toArray(new FontSourceBase[fontSources.size()]);

		// Apply the new set of font sources to use.
		FontSettings.getDefaultInstance().setFontsSources(updatedFontSources);
	}

	private void loadFontsForSlides() {
		//We have to explicitly set the font directory for unix systems
		final String[] fontDirectories = new String[] { getFontsPath() };
		FontsLoader.loadExternalFonts(fontDirectories);
	}

	private void loadFontsForCells() {
		//We have to explicitly set the font directory for unix systems
		FontConfigs.setFontFolder(getFontsPath(), false);
	}

	private void loadFonts() {
		List<Font> fonts = Arrays.asList(ge.getAllFonts());
		for (String filename : fontDirectory.list()) {
			File fontFile = new File(fontDirectory, filename);

			Font newFont = createFont(fontFile);
			if(newFont != null) {
				if(fonts.contains(newFont)) {
					log.debug("skipping font [{}], already registered", newFont::getFontName);
				} else if (!ge.registerFont(newFont)) {
					log.warn("unable to register font [{}] filename [{}]", newFont::getFontName, fontFile::getName);
				} else {
					log.debug("registered font [{}] filename [{}]", newFont::getFontName, fontFile::getName);
				}
			}
		}

		ge.preferProportionalFonts();
	}

	/**
	 * Get the font. When retrieving the font fails it is logged and
	 * <code>null</code> is returned.
	 *
	 * @param fontFile File location of the font to be loaded
	 * @return the font or <code>null</code>.
	 */
	private Font createFont(File fontFile) {
		String name = fontFile.getName();
		if (!name.toLowerCase().endsWith(TRUETYPE_FONT_EXT)) {
			throw new IllegalArgumentException("Unexpected extension! (file: " + name + " expected extension: " + TRUETYPE_FONT_EXT + ")");
		}
		try {
			return Font.createFont(Font.TRUETYPE_FONT, fontFile);
		} catch (FontFormatException | IOException e) {
			log.warn("unable to load font [{}]", name, e);
		}
		return null;
	}

	public String getFontsPath() {
		return fontDirectory.getPath();
	}
}
