/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.tika.mime.MediaType;

import com.aspose.slides.InvalidPasswordException;
import com.aspose.slides.LoadOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.MetaData;

/**
 * Converts the files which are required and supported by the aspose slides library.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 *
 */
public class SlidesConvertor extends AbstractConvertor {

	// contains mapping from MediaType to the LoadOption for the aspose word conversion.
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		// The string value is defined in com.aspose.slides.LoadOptions.
		map.put(new MediaType("application", "vnd.ms-powerpoint"), new LoadOptions());
		map.put(new MediaType("application", "vnd.openxmlformats-officedocument.presentationml.presentation"),
				new LoadOptions());

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	SlidesConvertor(String pdfOutputLocation) {
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

		Presentation presentation = new Presentation(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		presentation.save(outputStream, SaveFormat.Pdf);
		presentation.dispose();
		InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
		result.setFileStream(inStream);
//		result.setMetaData(new MetaData(getNumberOfPages(inStream)));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
