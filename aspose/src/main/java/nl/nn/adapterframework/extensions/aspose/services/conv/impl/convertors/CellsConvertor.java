/*
   Copyright 2019, 2021 WeAreFrank!

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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Style;
import com.aspose.cells.Workbook;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

class CellsConvertor extends AbstractConvertor {

	private static final MediaType XLS_MEDIA_TYPE = new MediaType("application", "vnd.ms-excel");
	private static final MediaType XLSX_MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private static final MediaType XLS_MEDIA_TYPE_MACRO_ENABLED = new MediaType("application", "vnd.ms-excel.sheet.macroenabled.12");

	private static final Map<MediaType, String> FILE_TYPE_MAP = new HashMap<>();

	private static final Logger LOGGER = LogUtil.getLogger(CellsConvertor.class);

	static {
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE, "xls");
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE_MACRO_ENABLED, "xlsm");
		FILE_TYPE_MAP.put(XLSX_MEDIA_TYPE, "xlsx");
	}

	protected CellsConvertor(String pdfOutputLocation) {
		super(pdfOutputLocation, XLS_MEDIA_TYPE, XLS_MEDIA_TYPE_MACRO_ENABLED, XLSX_MEDIA_TYPE);
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		// Convert Excel to pdf and store in result
		try (InputStream inputStream = message.asInputStream(charset)) {
			Workbook workbook = new Workbook(inputStream);

			Style style = workbook.getDefaultStyle();
			LOGGER.debug("Default font: " + style.getFont());

			workbook.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.PDF);
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));

			// Add original file as attachment to resulting pdf file.
			PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(result.getPdfResultFile());
			pdfAttachmentUtil.addAttachmentToPdf(message, result.getDocumentName(), FILE_TYPE_MAP.get(mediaType));
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
