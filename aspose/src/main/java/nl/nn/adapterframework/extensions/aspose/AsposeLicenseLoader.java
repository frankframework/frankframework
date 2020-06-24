/*
   Copyright 2019-2020 WeAreFrank!

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

import java.io.InputStream;
import java.net.URL;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

public class AsposeLicenseLoader {
	enum AsposeLibrary {
		WORDS, CELLS, EMAIL, PDF, SLIDES, IMAGING;
	}

	private static Boolean WORDS_LICENSE_LOADED = null;
	private static Boolean CELLS_LICENSE_LOADED = null;
	private static Boolean EMAIL_LICENSE_LOADED = null;
	private static Boolean PDF_LICENSE_LOADED = null;
	private static Boolean SLIDES_LICENSE_LOADED = null;
	private static Boolean IMAGING_LICENSE_LOADED = null;

	private final Logger log = LogUtil.getLogger(this);
	private static AsposeLicenseLoader self = null;

	private URL license = null;

	// We only need to load the license once
	public static synchronized void loadLicenses(URL asposeLicenseLocation) throws Exception {
		if(self == null) {
			self = new AsposeLicenseLoader(asposeLicenseLocation);
		}
	}

	/**
	 * how wonderful that every component has it's very own way of licensing..
	 */
	public boolean licenseLoaded(AsposeLibrary libraryName) {
		try {
			switch (libraryName) {
			case WORDS:
				if(WORDS_LICENSE_LOADED == null) {
					com.aspose.words.License wordsLicense = new com.aspose.words.License();
					WORDS_LICENSE_LOADED = wordsLicense.isLicensed();
				}
				return WORDS_LICENSE_LOADED;
			case CELLS:
				if(CELLS_LICENSE_LOADED == null) {
					CELLS_LICENSE_LOADED = com.aspose.cells.License.isLicenseSet();
				}
				return CELLS_LICENSE_LOADED;
			case EMAIL:
				if(EMAIL_LICENSE_LOADED == null) {
					com.aspose.email.License emailLicense = new com.aspose.email.License();
					EMAIL_LICENSE_LOADED = emailLicense.isLicensed();
				}
				return EMAIL_LICENSE_LOADED;
			case PDF:
				if(PDF_LICENSE_LOADED == null) {
					PDF_LICENSE_LOADED = com.aspose.pdf.Document.isLicensed();
				}
				return PDF_LICENSE_LOADED;
			case SLIDES:
				if(SLIDES_LICENSE_LOADED == null) {
					com.aspose.slides.License slidesLicense = new com.aspose.slides.License();
					SLIDES_LICENSE_LOADED = slidesLicense.isLicensed();
				}
				return SLIDES_LICENSE_LOADED;
			case IMAGING:
				if(IMAGING_LICENSE_LOADED == null) {
					IMAGING_LICENSE_LOADED = com.aspose.imaging.License.isLicensed();
				}
				return IMAGING_LICENSE_LOADED;
	
			default:
				return false;
			}
		} catch (Throwable t) {
			log.warn("unable to load Aspose ["+libraryName.name()+"] license information", t);
			return false;
		}
	}

	private interface LicenseLoader {
		void loadLicense(InputStream licenseInputStream) throws Exception;
	}

	public AsposeLicenseLoader(URL asposeLicenseLocation) throws Exception {
		if(asposeLicenseLocation == null) {
			throw new IllegalStateException("license url must be set");
		} else {
			license = asposeLicenseLocation;
		}

		// words
		loadAsposeLicense(new LicenseLoader() {
			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.words.License asposeLicense = new com.aspose.words.License();
				asposeLicense.setLicense(licenseInputStream);
			}
		}, AsposeLibrary.WORDS);

		// cells
		loadAsposeLicense(new LicenseLoader() {
			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.cells.License asposeLicense = new com.aspose.cells.License();
				asposeLicense.setLicense(licenseInputStream);
			}
		}, AsposeLibrary.CELLS);

		// email
		loadAsposeLicense(new LicenseLoader() {
			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.email.License asposeLicense = new com.aspose.email.License();
				asposeLicense.setLicense(licenseInputStream);
			}
		}, AsposeLibrary.EMAIL);

		// pdf
		loadAsposeLicense(new LicenseLoader() {
			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.pdf.License asposeLicense = new com.aspose.pdf.License();
				asposeLicense.setLicense(licenseInputStream);
			}
		}, AsposeLibrary.PDF);

		// slides
		loadAsposeLicense(new LicenseLoader() {
			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.slides.License asposeLicense = new com.aspose.slides.License();
				asposeLicense.setLicense(licenseInputStream);
			}
		}, AsposeLibrary.SLIDES);

		// imaging
		loadAsposeLicense(new LicenseLoader() {
			@Override
			public void loadLicense(InputStream licenseInputStream) throws Exception {
				com.aspose.imaging.License asposeLicense = new com.aspose.imaging.License();
				asposeLicense.setLicense(licenseInputStream);
			}
		}, AsposeLibrary.IMAGING);

	}

	private void loadAsposeLicense(LicenseLoader licenseLoader, AsposeLibrary library) throws Exception {
		if(!licenseLoaded(library)) {
			log.debug("loading Aspose ["+library.name()+"] license");

			try (InputStream inputStream = license.openStream()) {
				licenseLoader.loadLicense(inputStream);
				log.info("loaded Aspose [" + library.name() + "] license");

				switch (library) {
				case WORDS:
					WORDS_LICENSE_LOADED = true;
					break;
				case CELLS:
					CELLS_LICENSE_LOADED = true;
					break;
				case EMAIL:
					EMAIL_LICENSE_LOADED = true;
					break;
				case IMAGING:
					IMAGING_LICENSE_LOADED = true;
					break;
				case PDF:
					PDF_LICENSE_LOADED = true;
					break;
				case SLIDES:
					SLIDES_LICENSE_LOADED = true;
					break;
				}
			} catch (Exception e) {
				log.error("failed to load Aspose [" + library.name() + "] license", e);
				throw e;
			}
		}
	}
}
