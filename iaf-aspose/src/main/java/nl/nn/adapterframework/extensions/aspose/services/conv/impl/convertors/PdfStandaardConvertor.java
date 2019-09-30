package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;

import org.apache.tika.mime.MediaType;

import com.aspose.pdf.exceptions.InvalidPasswordException;
import com.google.common.io.Files;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * Convertor for a pdf file (no conversion required).
 *
 */
public class PdfStandaardConvertor extends AbstractConvertor {

	PdfStandaardConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation, new MediaType("application", "pdf"));
	}

	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {
		Files.copy(file, result.getPdfResultFile());
		result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
