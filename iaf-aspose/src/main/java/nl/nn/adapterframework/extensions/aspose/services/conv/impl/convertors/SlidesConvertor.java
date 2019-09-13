/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.slides.InvalidPasswordException;
import com.aspose.slides.LoadOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * 
 * @author M64D844
 *
 */
public class SlidesConvertor extends AbstractConvertor {

	private static final Logger LOGGER = Logger.getLogger(SlidesConvertor.class);
	// contains mapping from MediaType to the LoadOption for the aspose word
	// conversion.
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
	void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			// mediaType should always be supported otherwise there a program error because
			// the supported media types should be part of the map
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		try (FileInputStream inputStream = new FileInputStream(file)) {
			Presentation presentation = new Presentation(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			long startTime = new Date().getTime();
			presentation.save(outputStream, SaveFormat.Pdf);
			long endTime = new Date().getTime();
			LOGGER.info("Conversion(save operation in convert method) takes  :::  " + (endTime - startTime) + " ms");
			presentation.dispose();
			InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
			result.setFileStream(inStream);
			outputStream.close();
		}
		// result.setMetaData(new MetaData(getNumberOfPages(inStream)));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
