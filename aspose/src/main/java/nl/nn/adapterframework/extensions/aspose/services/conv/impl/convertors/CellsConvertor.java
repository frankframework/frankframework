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
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Style;
import com.aspose.cells.Workbook;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.util.LogUtil;

class CellsConvertor extends AbstractConvertor {

	private static final MediaType XLS_MEDIA_TYPE = new MediaType("application", "vnd.ms-excel");
	private static final MediaType XLSX_MEDIA_TYPE = new MediaType("application",
			"vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private static final MediaType XLS_MEDIA_TYPE_MACRO_ENABLED = new MediaType("application",
			"vnd.ms-excel.sheet.macroenabled.12");

	private static final Map<MediaType, String> FILE_TYPE_MAP = new HashMap<>();

	private static final Logger LOGGER = LogUtil.getLogger(CellsConvertor.class);

	static {
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE, "xls");
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE_MACRO_ENABLED, "xlsm");
		FILE_TYPE_MAP.put(XLSX_MEDIA_TYPE, "xlsx");
	}

	protected CellsConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation, XLS_MEDIA_TYPE, XLS_MEDIA_TYPE_MACRO_ENABLED, XLSX_MEDIA_TYPE);
	}

	/**
	 * Convert the document to PDF (as is done in all other converters)
	 */
	private void convertOrg(File file, CisConversionResult result) throws Exception {

		try (FileInputStream inputStream = new FileInputStream(file)) {
			Workbook workbook = new Workbook(inputStream);

			Style style = workbook.getDefaultStyle();
			LOGGER.debug("Default font: " + style.getFont());

			workbook.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.PDF);
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
		}
	}

	/**
	 * {@inheritDoc} Convert to PDF and attach the original to it.
	 */
	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {
		// Convert Excel to pdf and store in result
		convertOrg(file, result);
		// Add original file as attachment to resulting pdf file.
		try (FileInputStream inputStreamToAttach = new FileInputStream(file)) {
			PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(result.getPdfResultFile());
			pdfAttachmentUtil.addAttachmentToPdf(inputStreamToAttach, result.getDocumentName(),
					FILE_TYPE_MAP.get(mediaType));
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
