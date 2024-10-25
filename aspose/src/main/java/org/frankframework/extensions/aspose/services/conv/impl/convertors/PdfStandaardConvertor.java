/*
   Copyright 2019, 2021-2022 WeAreFrank!

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

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.http.MediaType;

import com.aspose.pdf.exceptions.InvalidPasswordException;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.stream.Message;

/**
 * Convertor for a pdf file (no conversion required).
 *
 */
public class PdfStandaardConvertor extends AbstractConvertor {

	protected PdfStandaardConvertor(CisConfiguration configuration) {
		super(configuration, new MediaType("application", "pdf"));
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		Files.copy(message.asInputStream(charset), Paths.get(result.getPdfResultFile().getCanonicalPath()));
		result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
