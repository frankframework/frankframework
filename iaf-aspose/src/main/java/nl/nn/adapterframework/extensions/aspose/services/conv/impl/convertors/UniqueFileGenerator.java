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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import nl.nn.adapterframework.extensions.aspose.services.util.StringsUtil;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
class UniqueFileGenerator {

	private static AtomicInteger atomicCount = new AtomicInteger(1);

	private UniqueFileGenerator() {
		
	}
	
	/**
	 * Create a unique file in the pdfOutputLocation with the given extension
	 * 
	 * @param extension
	 *            is allowed to be null.
	 * @return
	 */
	public static File getUniqueFile(String directory, String prefix, String extension) {

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		int count = atomicCount.addAndGet(1);
		String fileType;
		if (StringsUtil.isBlank(extension)) {
			fileType = "";
		} else {
			fileType = "." + extension;
		}

		// Save to disc
		String fileNamePdf = String.format("%s_%s_%05d%s", prefix, format.format(new Date()), count, fileType);
		return new File(directory, fileNamePdf);

	}

}
