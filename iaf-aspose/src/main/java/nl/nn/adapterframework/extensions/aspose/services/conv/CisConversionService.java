/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.io.InputStream;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public interface CisConversionService {

	/**
	 * This will try to convert the given inputStream to a pdf.
	 * <p>
	 * The given document stream is <em>not</em> closed by this method.
	 * 
	 * @param inputStream
	 * @param filename
	 *            (without the path). Is used to detect mediatype and inform the
	 *            user of the name of the file. Is allowed to be null.
	 * @return
	 * @throws CisConversionException
	 *             when a failure occurs.
	 */
	CisConversionResult convertToPdf(InputStream inputStream, String filename, ConversionOption conversionOption);

	/**
	 * This will try to convert the given inputStream to a pdf.
	 * <p>
	 * The given document stream is <em>not</em> closed by this method.
	 * 
	 * @param inputStream
	 * @return
	 * @throws CisConversionException
	 *             when a failure occurs.
	 */
	CisConversionResult convertToPdf(InputStream inputStream, ConversionOption conversionOption);

	/**
	 * Combines the given files in cisConversionResult to a single pdf.
	 * 
	 * @param cisConversionResult
	 * @return
	 */
	CisConversionResult combineToSinglePdf(CisConversionResult cisConversionResult);

	String getFontsDirectory();
}
