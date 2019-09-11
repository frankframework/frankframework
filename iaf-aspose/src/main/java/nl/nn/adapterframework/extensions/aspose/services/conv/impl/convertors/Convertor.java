/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.util.List;

import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * 
 * @author M64D844
 *
 */
public interface Convertor {

	/**
	 * Returns the supported media types.
	 * 
	 * @return
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * Converts the given file to a pdf. MediaType is the detected media type of the
	 * file. The convertor should support the given mediatype (otherwise it gives
	 * programming error).
	 * 
	 * @param mediaType
	 * @param filename
	 * @param file
	 * @param conversionOption
	 * @return
	 */
	CisConversionResult convertToPdf(MediaType mediaType, String filename, File file,
			ConversionOption conversionOption);

}
