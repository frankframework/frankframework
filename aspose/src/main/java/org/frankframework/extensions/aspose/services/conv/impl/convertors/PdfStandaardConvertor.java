/*
   Copyright 2019-2026 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.http.MediaType;

import com.aspose.pdf.Document;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.exceptions.InvalidPasswordException;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;

/**
 * Converter for a pdf file (no conversion required).
 *
 */
public class PdfStandaardConvertor extends AbstractConvertor {

	protected PdfStandaardConvertor(CisConfiguration configuration) {
		super(configuration, new MediaType("application", "pdf"));
	}

	/**
	 * Read the PDF to ensure it's validity and get the amount of pages.
	 */
	@Override
	public Message convert(MediaType mediaType, Message message) throws IOException {
		MessageBuilder messageBuilder = new MessageBuilder();
		try (InputStream inputStream = message.asInputStream(configuration.getCharset());
				Document doc = new Document(inputStream)) {

			int numberOfPages = doc.getPages().size();
			try (OutputStream stream = messageBuilder.asOutputStream()) {
				doc.save(stream, SaveFormat.Pdf);
			}

			Message result = messageBuilder.build();
			result.getContext().withMimeType(PDF_MIMETYPE).with("Pdf.Pages", numberOfPages);
			return result;
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}
}
