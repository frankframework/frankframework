package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.tika.mime.MediaType;

import com.aspose.words.Document;
import com.aspose.words.IncorrectPasswordException;
import com.aspose.words.LoadFormat;
import com.aspose.words.LoadOptions;
import com.aspose.words.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.MetaData;

/**
 * Converts the files which are required and supported by the aspose words library.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 */
class WordConvertor extends AbstractConvertor {

	// contains mapping from MediaType to the LoadOption for the aspose word conversion.
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		// Mapping to loadOptions
		map.put(new MediaType("application", "msword"), null);
		map.put(new MediaType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document"), null);
		map.put(new MediaType("application", "vnd.ms-word.document.macroenabled.12"), null);

		// The string value is defined in com.aspose.words.LoadFormat.
		map.put(new MediaType("text", "plain"), new LoadOptions(LoadFormat.fromName("TEXT"), null, null));
		map.put(new MediaType("text", "x-log"), new LoadOptions(LoadFormat.fromName("TEXT"), null, null));
		map.put(new MediaType("text", "csv"), new LoadOptions(LoadFormat.fromName("TEXT"), null, null));

		// The string value is defined in com.aspose.words.LoadFormat.
		map.put(new MediaType("application", "rtf"), new LoadOptions(LoadFormat.fromName("RTF"), null, null));

		map.put(new MediaType("application", "xml"), new LoadOptions(LoadFormat.fromName("TEXT"), null, null));
		//		map.put(new MediaType("text", "html"), new HtmlLoadOptions());
		//		map.put(new MediaType("application", "xhtml+xml"), new HtmlLoadOptions());
		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	WordConvertor(String pdfOutputLocation) {
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
		//		HtmlLoadOptions load = new HtmlLoadOptions("C:/Users/alisihab/Desktop/PDFconversionTestFiles/");

		Document doc = new Document(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
		new Fontsetter().setFontSettings(doc);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		doc.save(outputStream, SaveFormat.PDF);
		InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
		result.setFileStream(inStream);
//		result.setMetaData(new MetaData(getNumberOfPages(inStream)));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof IncorrectPasswordException;
	}

}
