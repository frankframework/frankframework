package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.cells.CountryCode;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Style;
import com.aspose.cells.Workbook;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

class CellsConvertor extends AbstractConvertor {

	private static final MediaType XLS_MEDIA_TYPE = new MediaType("application", "vnd.ms-excel");
	private static final MediaType XLSX_MEDIA_TYPE = new MediaType("application",
			"vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private static final MediaType XLS_MEDIA_TYPE_MACRO_ENABLED = new MediaType("application",
			"vnd.ms-excel.sheet.macroenabled.12");

	private static final Map<MediaType, String> FILE_TYPE_MAP = new HashMap<>();

	private static final Logger LOGGER = Logger.getLogger(CellsConvertor.class);

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

			LOGGER.debug("Aangetroffen locale: " + workbook.getSettings().getLocale() + " en region: "
					+ workbook.getSettings().getRegion());

			workbook.getSettings().setLocale(Locale.GERMAN);
			workbook.getSettings().setRegion(CountryCode.GERMANY);

			LOGGER.debug("Overschreven met locale: " + workbook.getSettings().getLocale() + " en region: "
					+ workbook.getSettings().getRegion());

			Style style = workbook.getDefaultStyle();
			LOGGER.debug("Default font: " + style.getFont());

			// Convert and store in
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			long startTime = new Date().getTime();
			workbook.save(outputStream, SaveFormat.PDF);
			long endTime = new Date().getTime();
			LOGGER.info("Conversion(save operation in convert method) takes  :::  " + (endTime - startTime) + " ms");
			InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
			result.setFileStream(inStream);
			outputStream.close();
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
			PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(null, result);
			pdfAttachmentUtil.addAttachmentToPdf(result, inputStreamToAttach, result.getDocumentName(),
					FILE_TYPE_MAP.get(mediaType));
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
