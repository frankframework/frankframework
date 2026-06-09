/*
   Copyright 2019, 2021-2026 WeAreFrank!

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import com.aspose.cells.LoadOptions;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.aspose.pdf.Document;
import com.aspose.pdf.FileSpecification;
import com.aspose.pdf.PageMode;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.SerializableFileReference;
import org.frankframework.util.TemporaryDirectoryUtils;

@Log4j2
class CellsConvertor extends AbstractConvertor {

	private static final MediaType XLS_MEDIA_TYPE = new MediaType("application", "vnd.ms-excel");
	private static final MediaType XLSX_MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private static final MediaType XLS_MEDIA_TYPE_MACRO_ENABLED = new MediaType("application", "vnd.ms-excel.sheet.macroenabled.12");

	private static final Map<MediaType, String> FILE_TYPE_MAP = new HashMap<>();

	static {
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE, "xls");
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE_MACRO_ENABLED, "xlsm");
		FILE_TYPE_MAP.put(XLSX_MEDIA_TYPE, "xlsx");
	}

	private final LoadOptions defaultLoadOptions;

	protected CellsConvertor(CisConfiguration configuration) {
		super(configuration, XLS_MEDIA_TYPE, XLS_MEDIA_TYPE_MACRO_ENABLED, XLSX_MEDIA_TYPE);
		defaultLoadOptions = new FontManager(configuration.getFontsDirectory()).getCellsLoadOptions();
	}

	@Override
	public Message convert(MediaType mediaType, Message message) throws Exception {
		// Convert Excel to pdf and store in result
		Path tempDir = TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY);
		Path tempFile = Files.createTempFile(tempDir, "msg", "pdf");

		try (InputStream inputStream = message.asInputStream(configuration.getCharset())) {
			Workbook workbook = new Workbook(inputStream, defaultLoadOptions);

			log.trace("default font: [{}]", () -> workbook.getDefaultStyle().getFont());

			try (OutputStream stream = Files.newOutputStream(tempFile)) {
				workbook.save(stream, SaveFormat.PDF);
			} finally {
				workbook.dispose();
			}

			// Add original file as attachment to resulting pdf file.
			String attachmentName = getValidFileName(message, FILE_TYPE_MAP.get(mediaType));
			return addMessageAsAttachment(tempFile, message, attachmentName);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	private String getValidFileName(Message input, String extension) {
		String name = (String) input.getContext().get(MessageContext.METADATA_NAME);
		if (StringUtils.isBlank(name)) {
			return "default_filename."+extension;
		}
		String result = StringUtils.substringBeforeLast(name, ".") + "." + extension;
		if (!result.equals(name)) {
			log.debug("updated filename to a valid filename from [{}] to [{}]", name, result);
		}
		return result;
	}

	private Message addMessageAsAttachment(Path tempFile, Message input, String attachmentName) throws IOException {
		try (Document pdfDoc = new Document(Files.newInputStream(tempFile))) {
			pdfDoc.setPageMode(PageMode.UseAttachments);

			try (InputStream attachIs = input.asInputStream()) {
				pdfDoc.getEmbeddedFiles().add(new FileSpecification(attachIs, attachmentName));
			}

			MessageBuilder messageBuilder = new MessageBuilder();
			try (OutputStream out = messageBuilder.asOutputStream()) {
				pdfDoc.save(out);
			}

			Message result = messageBuilder.build();
			result.getContext().withMimeType(PDF_MIMETYPE);
			return result;
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
