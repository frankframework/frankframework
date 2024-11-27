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
package org.frankframework.extensions.aspose;

import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

import com.aspose.words.License;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AsposeLicenseLoader {
	enum AsposeLibrary {
		WORDS, CELLS, EMAIL, PDF, SLIDES, IMAGING
	}

	private static final Map<AsposeLibrary, LicenseWrapper> loadedLicenses = new EnumMap<>(AsposeLibrary.class);

	static {
		loadedLicenses.put(AsposeLibrary.WORDS, new WordsLicenseWrapper());
		loadedLicenses.put(AsposeLibrary.CELLS, new CellsLicenseWrapper());
		loadedLicenses.put(AsposeLibrary.EMAIL, new EmailLicenseWrapper());
		loadedLicenses.put(AsposeLibrary.PDF, new PdfLicenseWrapper());
		loadedLicenses.put(AsposeLibrary.SLIDES, new SlidesLicenseWrapper());
		loadedLicenses.put(AsposeLibrary.IMAGING, new ImagingLicenseWrapper());
	}

	private static AsposeLicenseLoader self = null;

	private final URL license;

	// We only need to load the license once
	public static synchronized void loadLicenses(URL asposeLicenseLocation) throws Exception {
		if(self == null) {
			self = new AsposeLicenseLoader(asposeLicenseLocation);
		}
	}

	private interface LicenseWrapper {
		void loadLicense(InputStream licenseInputStream) throws Exception;
		boolean isLicenseLoaded();
	}

	public AsposeLicenseLoader(URL asposeLicenseLocation) throws Exception {
		if(asposeLicenseLocation == null) {
			throw new IllegalStateException("license url must be set");
		} else {
			license = asposeLicenseLocation;
		}

		for(AsposeLibrary library: AsposeLibrary.values()) {
			loadAsposeLicense(library);
		}
	}

	private void loadAsposeLicense(AsposeLibrary library) throws Exception {
		LicenseWrapper licenseWrapper = loadedLicenses.get(library);
		if(licenseWrapper.isLicenseLoaded()) {
			log.debug("loading Aspose [{}] license", library::name);

			try (InputStream inputStream = license.openStream()) {
				licenseWrapper.loadLicense(inputStream);
				log.info("loaded Aspose [{}] license", library::name);
			} catch (Exception e) {
				log.error("failed to load Aspose [{}] license", library.name(), e);
				throw e;
			}
		}
	}

	private static class WordsLicenseWrapper implements LicenseWrapper {

		private License asposeLicense;

		@Override
		public void loadLicense(InputStream licenseInputStream) throws Exception {
			try {
				asposeLicense = new License();
				asposeLicense.setLicense(licenseInputStream);
			} catch (Exception e) {
				asposeLicense = null;
				throw e;
			}
		}

		@Override
		public boolean isLicenseLoaded() {
			return asposeLicense != null;
		}
	}

	private static class CellsLicenseWrapper implements LicenseWrapper {
		@Override
		public void loadLicense(InputStream licenseInputStream) throws Exception {
			com.aspose.cells.License asposeLicense = new com.aspose.cells.License();
			asposeLicense.setLicense(licenseInputStream);
		}

		@Override
		public boolean isLicenseLoaded() {
			return com.aspose.cells.License.isLicenseSet();
		}
	}

	private static class EmailLicenseWrapper implements LicenseWrapper {

		private com.aspose.email.License asposeLicense;

		@Override
		public void loadLicense(InputStream licenseInputStream) throws Exception {
			try {
				asposeLicense = new com.aspose.email.License();
				asposeLicense.setLicense(licenseInputStream);
			} catch (Exception e) {
				asposeLicense = null;
				throw e;
			}
		}

		@Override
		public boolean isLicenseLoaded() {
			return asposeLicense != null;
		}
	}

	private static class PdfLicenseWrapper implements LicenseWrapper {

		private com.aspose.pdf.License asposeLicense;

		@Override
		public void loadLicense(InputStream licenseInputStream) throws Exception {
			try {
				asposeLicense = new com.aspose.pdf.License();
				asposeLicense.setLicense(licenseInputStream);
			} catch (Exception e) {
				asposeLicense = null;
				throw e;
			}
		}

		@Override
		public boolean isLicenseLoaded() {
			return asposeLicense != null;
		}
	}

	private static class SlidesLicenseWrapper implements LicenseWrapper {

		private final com.aspose.slides.License asposeLicense = new com.aspose.slides.License();

		@Override
		public void loadLicense(InputStream licenseInputStream) throws Exception {
			asposeLicense.setLicense(licenseInputStream);
		}

		@Override
		public boolean isLicenseLoaded() {
			return asposeLicense.isLicensed();
		}
	}

	private static class ImagingLicenseWrapper implements LicenseWrapper {

		private com.aspose.imaging.License asposeLicense;

		@Override
		public void loadLicense(InputStream licenseInputStream) throws Exception {
			try {
				asposeLicense = new com.aspose.imaging.License();
				asposeLicense.setLicense(licenseInputStream);
			} catch (Exception e) {
				asposeLicense = null;
				throw e;
			}
		}

		@Override
		public boolean isLicenseLoaded() {
			return asposeLicense != null;
		}
	}
}
