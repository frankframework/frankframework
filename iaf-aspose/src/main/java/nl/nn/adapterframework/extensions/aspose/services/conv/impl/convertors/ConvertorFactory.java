/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;

/**
 * Convertor factory instantiates all convertor types and keeps them in a map.
 * 
 * @author M64D844
 *
 */
public class ConvertorFactory {

	private static final Logger LOGGER = Logger.getLogger(ConvertorFactory.class);

	private Map<MediaType, Convertor> convertorLookupMap = new HashMap<>();

	public ConvertorFactory(CisConversionService cisConversionService, String pdfOutputlocation) {
		addToConvertorLookupMap(new MailConvertor(cisConversionService, pdfOutputlocation));
		addToConvertorLookupMap(new PdfStandaardConvertor(pdfOutputlocation));
		addToConvertorLookupMap(new PdfConvertor(pdfOutputlocation));
		addToConvertorLookupMap(new PdfImageConvertor(pdfOutputlocation));
		addToConvertorLookupMap(new WordConvertor(cisConversionService, pdfOutputlocation));
		addToConvertorLookupMap(new CellsConvertor(pdfOutputlocation));
		addToConvertorLookupMap(new SlidesConvertor(pdfOutputlocation));
	}

	private void addToConvertorLookupMap(Convertor convertor) {
		for (MediaType mediaTypeSupported : convertor.getSupportedMediaTypes()) {
			Convertor oldConvertor = convertorLookupMap.put(mediaTypeSupported, convertor);
			if (oldConvertor != null) {
				LOGGER.warn("More than one convertor found for " + mediaTypeSupported);
			}
		}
	}

	/**
	 * Return <code>null</code> when no convertor is found.
	 * 
	 * @param mediaType
	 * @return
	 */
	public Convertor getConvertor(MediaType mediaType) {
		return convertorLookupMap.get(mediaType);
	}
}
