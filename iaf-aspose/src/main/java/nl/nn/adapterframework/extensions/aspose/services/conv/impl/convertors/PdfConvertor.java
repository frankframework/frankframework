/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.mime.MediaType;

import com.aspose.pdf.Document;
import com.aspose.pdf.HtmlLoadOptions;
import com.aspose.pdf.LoadOptions;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.XpsLoadOptions;
import com.aspose.pdf.exceptions.InvalidPasswordException;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.MetaData;

/**
 * Converts the files which are required and supported by the aspose pdf library.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 *
 */
public class PdfConvertor extends AbstractConvertor {

	// contains mapping from MediaType to the LoadOption for the aspose word conversion.
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		// The string value is defined in com.aspose.pdf.LoadOptions.
		//		CIS-44: Tijdelijk gedisabled omdat html conversie op A (en P) niet goed gaat. Moet nog worden geanalyseerd.
		map.put(new MediaType("text", "html"), new HtmlLoadOptions());
		map.put(new MediaType("application", "xhtml+xml"), new HtmlLoadOptions());
		map.put(new MediaType("application", "vnd.ms-xpsdocument"), new XpsLoadOptions());
		map.put(new MediaType("application", "x-tika-ooxml"), new XpsLoadOptions());

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	PdfConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation,
				MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet().toArray(new MediaType[MEDIA_TYPE_LOAD_FORMAT_MAPPING.size()]));
	}

	@Override
	void convert(MediaType mediaType, InputStream inputStream, File fileDest, CisConversionResult result,
			ConversionOption conversionOption) throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			// mediaType should always be supported otherwise there a program error because the supported media types should be part of the map
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		HtmlLoadOptions load = new HtmlLoadOptions("C:/Users/alisihab/Desktop/PDFconversionTestFiles/");
		load.getPageInfo().setLandscape(true);
		Document doc = new Document(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		doc.save(outputStream, SaveFormat.Pdf);
		doc.freeMemory();
		doc.dispose();
		doc.close();
		InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
//		result.setMetaData(new MetaData(getNumberOfPages(inStream)));
		result.setFileStream(inStream);
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
