/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.InputStream;
import java.util.List;

import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 */
public interface Convertor {

	/**
	 * Returns the supported media types.
	 * 
	 * @return
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * Converts the given inputStream to a pdf. MediaType is the detected media type
	 * of the inputstream. The convertor should support the given mediatype
	 * (otherwise it programming error).
	 * 
	 * @param mediaType
	 * @param filename
	 *            (without the path). Is used to detect mediatype and inform the
	 *            user of the name of the file. Is allowed to be null.
	 * @param inputStream
	 * @param conversionOption
	 * @return the result.
	 */
	CisConversionResult convertToPdf(MediaType mediaType, String filename, InputStream inputStream,
			ConversionOption conversionOption);

}
