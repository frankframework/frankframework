/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.common.io.Files;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.util.FileUtil;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der Hoorn</a> (d937275)
 *
 */
public class PdfCombiner {

	private static final Logger LOGGER = Logger.getLogger(PdfCombiner.class);
	
	private String pdfOutputlocation;

	public PdfCombiner(String pdfOutputlocation) {
		this.pdfOutputlocation = pdfOutputlocation;
	}

	/**
	 * Combines the attachments in the given cisConversionResult to the pdf of cisConversionResult.
	 * <p>If cisConversionResult does not contain attachments then just the given cisConversionResult is returned.</p>
	 * @param cisConversionResult
	 * @return
	 * @throws IOException 
	 */
	public CisConversionResult combineToSinglePdf(CisConversionResult cisConversionResultInput) throws IOException {
		
		LOGGER.debug("Start    CombineToSinglePdf input: " + cisConversionResultInput);

		// Combineer elk attachment
		CisConversionResult result = createCisConversionResultWithoutAttachments(cisConversionResultInput);
		result.setConversionOption(ConversionOption.SINGLEPDF);

		// Dit document heeft geen attachments so combineren is direkt klaar.
		if (cisConversionResultInput.getAttachments().isEmpty()) {
			if (cisConversionResultInput.getPdfResultFile() != null) {
				File singlePdf = UniqueFileGenerator.getUniqueFile(pdfOutputlocation, this.getClass().getSimpleName(), "pdf");
				Files.copy(cisConversionResultInput.getPdfResultFile(), singlePdf);
				result.setPdfResultFile(singlePdf);
			}
			LOGGER.debug("Finished no attachements result: " + result);
			return result;
		}		
		
		// Create from each attachment a single pdf.
		for (CisConversionResult cisConversionResultAttachment : cisConversionResultInput.getAttachments()) {
			result.addAttachment(combineToSinglePdf(cisConversionResultAttachment));
		}
		
		// Alle attachments zijn nu single pdf's zo voeg deze samen.
		// Create a single pdf.
		if (cisConversionResultInput.getPdfResultFile() != null) {
			File singlePdf = UniqueFileGenerator.getUniqueFile(pdfOutputlocation, this.getClass().getSimpleName(), "pdf");
			Files.copy(cisConversionResultInput.getPdfResultFile(), singlePdf);
			PdfAttachmentUtil.addAttachmentInSinglePdf(result.getAttachments(), singlePdf);
			result.setPdfResultFile(singlePdf);
		}
		
		// Clear the pdf Result file except the highest.
		for (CisConversionResult attachment : result.getAttachments()) {
			FileUtil.deleteFile(attachment.getPdfResultFile());
			attachment.setPdfResultFile(null);
		}

		LOGGER.debug("Finished CombineToSinglePdf result: " + result);
		
		return result;
	}
	
	/**
	 * Construct the builder with the given cisConversieResult. The attachments are not filled.
	 * @param cisConversionResult
	 */
	private CisConversionResult createCisConversionResultWithoutAttachments(CisConversionResult copy) {
		return CisConversionResult.createCisConversionResult(copy.getConversionOption(), copy.getMediaType(), copy.getMetaData(), copy.getDocumentName(), null, copy.getFailureReason(), null);
	}
	
}
