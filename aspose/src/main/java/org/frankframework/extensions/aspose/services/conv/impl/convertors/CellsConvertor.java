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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import com.aspose.cells.LoadOptions;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Style;
import com.aspose.cells.Workbook;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.stream.Message;

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
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		if(!message.isRepeatable()) {
			message.preserve(); // required for attaching the original Excel to produced PDF
		}
		// Convert Excel to pdf and store in result
		Workbook workbook = null;
		try (InputStream inputStream = message.asInputStream(charset)) {
			workbook = new Workbook(inputStream, defaultLoadOptions);

			Style style = workbook.getDefaultStyle();
			log.debug("default font: [{}]", style.getFont());

			workbook.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.PDF);
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));

			// Add original file as attachment to resulting pdf file.
			PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(result.getPdfResultFile());
			pdfAttachmentUtil.addAttachmentToPdf(message, result.getDocumentName(), FILE_TYPE_MAP.get(mediaType));
		} finally {
			if (workbook != null) {
				workbook.dispose();
			}
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
