package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
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
import nl.nn.adapterframework.extensions.aspose.services.util.FileUtil;

class CellsConvertor extends AbstractConvertor {

	private final static MediaType XLS_MEDIA_TYPE = new MediaType("application", "vnd.ms-excel");
	private final static MediaType XLSX_MEDIA_TYPE = new MediaType("application",
			"vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	private final static MediaType XLS_MEDIA_TYPE_MACRO_ENABLED = new MediaType("application",
			"vnd.ms-excel.sheet.macroenabled.12");

	private final static Map<MediaType, String> FILE_TYPE_MAP = new HashMap<>();

	private static final Logger LOGGER = Logger.getLogger(CellsConvertor.class);

	static {
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE, "xls");
		FILE_TYPE_MAP.put(XLS_MEDIA_TYPE_MACRO_ENABLED, "xlsm");
		FILE_TYPE_MAP.put(XLSX_MEDIA_TYPE, "xlsx");
	}

	CellsConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation, XLS_MEDIA_TYPE, XLS_MEDIA_TYPE_MACRO_ENABLED, XLSX_MEDIA_TYPE);
	}

	/**
	 * Convert the document to PDF (as is done in all other converters)
	 */
	void convertOrg(InputStream inputStream, File fileDest, CisConversionResult result) throws Exception {
		Workbook workbook = new Workbook(inputStream);

		LOGGER.debug("Aangetroffen locale:  " + workbook.getSettings().getLocale() + " en region: "
				+ workbook.getSettings().getRegion());

		workbook.getSettings().setLocale(Locale.GERMAN);
		workbook.getSettings().setRegion(CountryCode.GERMANY);

		LOGGER.debug("Overschreven met locale:  " + workbook.getSettings().getLocale() + " en region: "
				+ workbook.getSettings().getRegion());

		Style style = workbook.getDefaultStyle();
		LOGGER.debug("Default font:  " + style.getFont());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.save(outputStream, SaveFormat.PDF);
		InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
		result.setFileStream(inStream);
		// result.setMetaData(new MetaData(getNumberOfPages(inStream)));
	}

	/**
	 * {@inheritDoc} Convert to PDF and attach the original to it.
	 */
	@Override
	void convert(MediaType mediaType, InputStream inputStream, File fileDest, CisConversionResult result,
			ConversionOption conversionOption) throws Exception {

		File tmpFile = getUniqueFile();

		try {
			Files.copy(inputStream, tmpFile.toPath());

			try (InputStream localInputStream = new BufferedInputStream(new FileInputStream(tmpFile))) {
				convertOrg(localInputStream, fileDest, result);
			}

			// Add the original as attachment to the pdf.
			PdfAttachmentUtil.addAttachmentToPdf(fileDest, tmpFile, result.getDocumentName(),
					FILE_TYPE_MAP.get(mediaType));

		} finally {
			FileUtil.deleteFile(tmpFile);
		}

	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return "Please provide password for the Workbook file.".equals(e.getMessage());
	}

}
