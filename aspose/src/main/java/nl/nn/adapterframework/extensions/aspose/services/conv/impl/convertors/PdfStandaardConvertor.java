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

import org.apache.tika.mime.MediaType;

import com.aspose.pdf.exceptions.InvalidPasswordException;
import com.google.common.io.Files;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * Convertor for a pdf file (no conversion required).
 *
 */
public class PdfStandaardConvertor extends AbstractConvertor {

	PdfStandaardConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation, new MediaType("application", "pdf"));
	}

	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {
		Files.copy(file, result.getPdfResultFile());
		result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
