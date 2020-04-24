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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.aspose.cells.CellsHelper;
import com.aspose.slides.FontsLoader;
import com.aspose.words.FolderFontSource;
import com.aspose.words.FontSettings;
import com.aspose.words.FontSourceBase;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * 
 * @author M64D844
 *
 */
public class AsposeLicenseLoader {

	private static final String FONTS_RESOURCE_NAME = "/font.zip";
	private static final String FONTS_RESOURCE_DIR = "/fonts/";

	private static final Logger LOGGER = LogUtil.getLogger(AsposeLicenseLoader.class);

	private static final String TRUETYPE_FONT_EXT = ".ttf";

	private String pathToExtractFonts = null;
	private String license = null;
	private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

	public AsposeLicenseLoader(String asposeLicenseLocation, String fontsDirectory) {
		license = asposeLicenseLocation;
		pathToExtractFonts = fontsDirectory;
	}

	public void loadLicense() throws Exception {
		if(StringUtils.isNotEmpty(license)) {
			loadAsposeLicense();
		}

		if (pathToExtractFonts == null) {
			pathToExtractFonts = Files.createTempDirectory("").toString();
		}

		if (Files.notExists(Paths.get(pathToExtractFonts))) {
			String userPath = pathToExtractFonts;
			pathToExtractFonts = Files.createTempDirectory("").toString();
			LOGGER.warn("Path to extract fonts does not exist:" + userPath + " Temp location will be used "
					+ pathToExtractFonts);
		}
		String pathToFonts = pathToExtractFonts + FONTS_RESOURCE_DIR;

		unpackFontsZip(pathToFonts);

		// logFonts("(Initi�le fonts)");

		loadFonts(pathToFonts);

		// logFonts("(Na load fonts)");

		loadExtraFonts(pathToFonts);

		// logFonts("(Na load extra fonts)");

		setFontDirectory(pathToFonts);
	}

	/**
	 * Zet de directory waar de fonts zich bevinden. Nodig omdat aspose op een unix
	 * bak ze anders niet kan vinden.
	 * 
	 * @param pathToFonts
	 */
	private void setFontDirectory(String pathToFonts) {
		CellsHelper.getFontDirs().add(pathToFonts);
	}

	private void unpackFontsZip(String fontsDir) throws IOException {

		try (InputStream inputStream = new BufferedInputStream(
				this.getClass().getResource(FONTS_RESOURCE_NAME).openStream())) {

			Path fontsDirPath = Paths.get(fontsDir);
			if (!Files.exists(fontsDirPath)) {
				fontsDirPath = Files.createDirectory(fontsDirPath);
			}

			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				while (true) {
					ZipEntry entry = zipInputStream.getNextEntry();
					if (entry == null) {
						break;
					}
					Path target = fontsDirPath.resolve(entry.getName());
					if (Files.notExists(target)) {
						Files.copy(zipInputStream, target);
					}
					zipInputStream.closeEntry();
				}
			}

		} catch (IOException e) {
			LOGGER.error("Unpacking fonts failed!", e);
			throw e;
		}
	}

	private void loadAsposeLicense() throws Exception {

		// words
		loadAsposeLicense(new LicenseLoader() {

			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.words.License asposeLicense = new com.aspose.words.License();
				if (!asposeLicense.isLicensed()) {
					asposeLicense.setLicense(licenseInputStream);
					LOGGER.info("Aspose loading License for words...");
				}
			}
		}, "words");

		// cells
		if (!com.aspose.cells.License.isLicenseSet()) {
			loadAsposeLicense(new LicenseLoader() {

				@Override
				public void loadLicense(InputStream licenseInputStream) throws Exception {
					com.aspose.cells.License asposeLicense = new com.aspose.cells.License();
					asposeLicense.setLicense(licenseInputStream);
					LOGGER.info("Aspose loading License for cells...");
				}
			}, "cells");
		}
		// email
		loadAsposeLicense(new LicenseLoader() {

			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.email.License asposeLicense = new com.aspose.email.License();
				if (!asposeLicense.isLicensed()) {
					asposeLicense.setLicense(licenseInputStream);
					LOGGER.info("Aspose loading License for email...");
				}

			}
		}, "email");

		// pdf
		if (!com.aspose.pdf.Document.isLicensed()) {
			loadAsposeLicense(new LicenseLoader() {

				@Override
				public void loadLicense(InputStream licenseInputStream) throws Exception {
					com.aspose.pdf.License asposeLicense = new com.aspose.pdf.License();
					asposeLicense.setLicense(licenseInputStream);
					LOGGER.info("Aspose loading License for pdf...");
				}
			}, "pdf");
		}
		//
		// slides
		loadAsposeLicense(new LicenseLoader() {

			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.slides.License asposeLicense = new com.aspose.slides.License();
				if (!asposeLicense.isLicensed()) {
					asposeLicense.setLicense(licenseInputStream);
					LOGGER.info("Aspose loading License for slides...");
				}
			}
		}, "slides");

		// imaging
		if (!com.aspose.imaging.License.isLicensed()) {
			loadAsposeLicense(new LicenseLoader() {

				@Override
				public void loadLicense(InputStream licenseInputStream) throws Exception {
					com.aspose.imaging.License asposeLicense = new com.aspose.imaging.License();
					asposeLicense.setLicense(licenseInputStream);
					LOGGER.info("Aspose loading License for imaging...");
				}

			}, "imaging");
		}

	}

	private void loadAsposeLicense(LicenseLoader licenseLoader, String asposeLibrayName) throws Exception {

		try (InputStream inputStream = ClassUtils.urlToStream(ClassUtils.getResourceURL(this, license), 10000)) {
			licenseLoader.loadLicense(inputStream);
			LOGGER.info("Aspose " + asposeLibrayName + " license loaded!");
		} catch (Exception e) {
			String message = "Loading Aspose " + asposeLibrayName + " license failed!";
			throw new Exception(message, e);
		}
	}

	private void loadExtraFonts(String pathToFonts) {
		loadExtraFontsForWord(pathToFonts);
		loadExtraFontsSlides(pathToFonts);
	}

	private void loadExtraFontsForWord(String pathToFonts) {
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
		FontSettings.getDefaultInstance().setFontsSources(updatedFontSources);
	}

	private void loadExtraFontsSlides(String pathToFonts) {
		// Load the custom font directory fonts
		final String[] fontDir = new String[] { pathToFonts };
		FontsLoader.loadExternalFonts(fontDir);
	}

//	private void logFonts(String msg) {
//		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		Font[] fontFamilies = ge.getAllFonts();
//
//		LOGGER.info("--------- fonts --------- start --------- " + msg);
//
//		int count = 1;
//		for (Font font : fontFamilies) {
//			LOGGER.info(count++ + " font: " + font.getName() + " fontFamily: " + font.getFamily());
//		}
//
//		LOGGER.info("--------- fonts ---------  end  --------- " + msg);
//	}

	private void loadFonts(String pathToFonts) throws IOException {
		List<Font> fonts = Arrays.asList(ge.getAllFonts());
		for (String filename : new File(pathToFonts).list()) {
			File fontFile = new File(pathToFonts, filename);

			Font newFont = createFont(fontFile, fontFile.getName());
			LOGGER.debug("Register font " + newFont);

			if (!fonts.contains(newFont) && !ge.registerFont(newFont)) {
				LOGGER.warn("Font not registered!" + newFont.getFontName());
			}
		}
		ge.preferProportionalFonts();
	}

	/**
	 * Get the font. When retrieving the font fails it is logged and
	 * <code>null</code> is returned.
	 * 
	 * @param fontFile
	 * @return the font or <code>null</code>.
	 */
	private Font createFont(File fontFile, String name) {
		if (!name.toLowerCase().endsWith(TRUETYPE_FONT_EXT)) {
			throw new IllegalArgumentException(
					"Unexpected extension! (file: " + name + " expected extension: " + TRUETYPE_FONT_EXT + ")");
		}
		Font result = null;
		try {
			result = Font.createFont(Font.TRUETYPE_FONT, fontFile);
		} catch (FontFormatException | IOException e) {
			LOGGER.error("Loading font failed! (file: " + name + ")", e);
		}
		return result;
	}

	private interface LicenseLoader {
		void loadLicense(InputStream licenseInputStream) throws Exception;
	}

	public String getPathToExtractFonts() {
		return pathToExtractFonts;
	}

	public void setPathToExtractFonts(String pathToExtractFonts) {
		this.pathToExtractFonts = pathToExtractFonts;
	}
}
