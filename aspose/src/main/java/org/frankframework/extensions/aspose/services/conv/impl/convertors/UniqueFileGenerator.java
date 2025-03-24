/*
   Copyright 2019 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Gerard van der Hoorn
 */
class UniqueFileGenerator {

	private static final AtomicInteger atomicCount = new AtomicInteger(1);

	private UniqueFileGenerator() {
		// Do not construct utility class
	}

	/**
	 * Create a unique file in the pdfOutputLocation with the given extension
	 *
	 * @param extension  is allowed to be null.
	 */
	public static File getUniqueFile(String directory, String prefix, String extension) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

		int count = atomicCount.addAndGet(1);
		String fileType;
		if (StringUtils.isEmpty(extension)) {
			fileType = "";
		} else {
			fileType = "." + extension;
		}

		// Save to disc
		String fileNamePdf = "%s_%s_%05d%s".formatted(prefix, formatter.format(LocalDateTime.now()), count, fileType);

		return new File(directory, fileNamePdf);
	}
}
