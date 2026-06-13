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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import com.aspose.cells.LoadOptions;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;

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
		try (InputStream inputStream = message.asInputStream(configuration.getCharset())) {
			Workbook workbook = new Workbook(inputStream, defaultLoadOptions);

			log.trace("default font: [{}]", () -> workbook.getDefaultStyle().getFont());

			MessageBuilder messageBuilder = new MessageBuilder();
			try (OutputStream stream = messageBuilder.asOutputStream()) {
				workbook.save(stream, SaveFormat.PDF);
			} finally {
				workbook.dispose();
			}

			// Add original file as attachment to resulting pdf file.
			String attachmentName = PdfAttachmentUtil.getValidFileName(message, FILE_TYPE_MAP.get(mediaType));
			return PdfAttachmentUtil.combineFiles(messageBuilder.build(), message, attachmentName);
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
