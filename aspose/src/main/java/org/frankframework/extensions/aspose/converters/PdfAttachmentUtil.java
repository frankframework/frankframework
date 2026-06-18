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
package org.frankframework.extensions.aspose.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import com.aspose.pdf.Document;
import com.aspose.pdf.FileSpecification;
import com.aspose.pdf.PageMode;
import com.aspose.pdf.SaveFormat;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.stream.MessageContext;

/**
 * This class will combine separate PDF files into a single PDF with attachments.
 */
@Log4j2
public class PdfAttachmentUtil {
	public static final String DEFAULT_FILENAME = "default_filename";
	private static final MimeType PDF_MIMETYPE = new MediaType("application", "pdf");

	private PdfAttachmentUtil() {
		// NO OP
	}

	/**
	 * Silly code due to Aspose limitations...
	 * https://forum.aspose.com/t/filespecification-from-stream-modified-is-unknown/68301/16
	 */
	@NonNull
	public static Message combineFiles(@NonNull Message parent, @NonNull Message attachment, String attachmentName) throws IOException {
		try (InputStream is = parent.asInputStream(); Document pdfDoc = new Document(is)) {
			pdfDoc.setPageMode(PageMode.UseAttachments);

			String location = (String) attachment.getContext().get(MessageContext.METADATA_LOCATION);
			if (location != null) {
				pdfDoc.getEmbeddedFiles().add(new FileSpecification(location, attachmentName));
			} else {
				// When using an InputStream, it's streaming, but no size is present.
				// When using a String (filepath) it reads from disk, the name is scrambled but the size is correct...
				// In both cases the description field is correct.
				try (InputStream attachIs = attachment.asInputStream()) {
					pdfDoc.getEmbeddedFiles().add(new FileSpecification(attachIs, attachmentName));
				}
			}

			MessageBuilder messageBuilder = new MessageBuilder();
			try (OutputStream out = messageBuilder.asOutputStream()) {
				pdfDoc.save(out, SaveFormat.Pdf);
			}

			pdfDoc.freeMemory();

			Message result = messageBuilder.build();
			result.getContext().withMimeType(PDF_MIMETYPE);

			return result;
		}
	}

	public static String getValidFileName(Message input, String extension) {
		return getValidFileName(input, DEFAULT_FILENAME, extension);
	}

	// Not sure why we need to get the name trim the extension only to add it again, but for now leave it as is...
	public static String getValidFileName(Message input, String fallbackName, String extension) {
		String name = (String) input.getContext().get(MessageContext.METADATA_NAME);
		if (StringUtils.isBlank(name)) {
			return StringUtils.substringBeforeLast(fallbackName, ".") + "." + extension;
		}

		String result = StringUtils.substringBeforeLast(name, ".") + "." + extension;
		if (!result.equals(name)) {
			log.debug("updated filename to a valid filename from [{}] to [{}]", name, result);
		}

		return result;
	}
}
