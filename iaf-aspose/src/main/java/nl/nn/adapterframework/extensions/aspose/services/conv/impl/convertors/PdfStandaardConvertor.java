package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.tika.mime.MediaType;

import com.aspose.pdf.exceptions.InvalidPasswordException;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.util.Misc;

/**
 * @author M64D844
 *
 */
public class PdfStandaardConvertor extends AbstractConvertor {

	PdfStandaardConvertor(String pdfOutputLocation) {
		super(pdfOutputLocation, new MediaType("application", "pdf"));
	}

	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			byte[] array = Misc.streamToBytes(inputStream);
			InputStream inStream = new ByteArrayInputStream(array);
			result.setFileStream(inStream);
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
