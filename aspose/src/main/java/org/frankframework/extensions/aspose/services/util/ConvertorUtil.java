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
package org.frankframework.extensions.aspose.services.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public class ConvertorUtil {

	private static final String DEFAULT_FILENAME = "default_filename";
	private static final String EXTENSION_DELIMITER = ".";
	public static final String PDF_FILETYPE = "pdf";

	private ConvertorUtil() {

	}

	/**
	 * Creates a filename. If the file already contains a file extension it will be
	 * removed.
	 */
	public static String createTidyNameWithoutExtension(String argFilename) {
		String filename;
		if (StringUtils.isBlank(argFilename)) {
			filename = DEFAULT_FILENAME;
		} else {
			filename = argFilename;
		}
		if (filename.contains(".")) {
			// Remove the file type.
			return filename.substring(0, filename.lastIndexOf('.'));
		}

		return filename;
	}

	/**
	 * Creates a filename which always contains the given extension as file type
	 * (without period). If the file already contains a extension it will be
	 * replaced with the given extension.
	 *
	 * @param argFilename
	 * @param extension (without the period).
	 */
	public static String createTidyFilename(@Nullable String argFilename, @Nonnull String extension) {
		String extensionWithDelim = extension.startsWith(EXTENSION_DELIMITER) ? extension : EXTENSION_DELIMITER + extension;
		String filename = createTidyNameWithoutExtension(argFilename);
		if (!filename.endsWith(extensionWithDelim)) {
			filename = filename + extensionWithDelim;
		}
		return filename;
	}
}
