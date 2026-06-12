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
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;

import com.aspose.pdf.Document;
import com.aspose.pdf.FileSpecification;
import com.aspose.pdf.PageMode;
import com.aspose.pdf.SaveFormat;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;

/**
 * This class will combine separate PDF files into a single PDF with attachments.
 */
@Log4j2
public class PdfAttachmentUtil {

	@NonNull
	public static Message combineFiles(@NonNull Message parent, @NonNull Message attachment, String fileName) throws IOException {
		try (Document pdfDoc = new Document(parent.asInputStream())) {
			pdfDoc.setPageMode(PageMode.UseAttachments);

			pdfDoc.getEmbeddedFiles().add(new FileSpecification(attachment.asInputStream(), fileName));
			MessageBuilder messageBuilder = new MessageBuilder();
			try (OutputStream out = messageBuilder.asOutputStream()) {
				pdfDoc.save(out, SaveFormat.Pdf);
			}

			pdfDoc.freeMemory();

			return messageBuilder.build();
		}
	}

}
