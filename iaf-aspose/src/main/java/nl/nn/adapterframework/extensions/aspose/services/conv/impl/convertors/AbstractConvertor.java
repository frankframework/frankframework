package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertorUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.DateUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.FileUtil;

abstract class AbstractConvertor implements Convertor {

	private static final Logger LOGGER = Logger.getLogger(AbstractConvertor.class);

	private List<MediaType> supportedMediaTypes;

	private String pdfOutputlocation;

	private static AtomicInteger atomicCount = new AtomicInteger(1);

	AbstractConvertor(String pdfOutputlocation, MediaType... args) {
		this.pdfOutputlocation = pdfOutputlocation;
		supportedMediaTypes = Arrays.asList(args);
	}

	/**
	 * Converts the the inputstream to the given file the builder object can also be
	 * updated (metaData set and any attachements added).
	 * 
	 * @param mediaType
	 * @param conversionOption
	 */
	abstract void convert(MediaType mediaType, InputStream inputStream, File fileDest, CisConversionResult builder,
			ConversionOption conversionOption) throws Exception;

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMediaTypes;
	}

	void checkForSupportedMediaType(MediaType mediaType) {

		boolean supported = false;

		for (MediaType mediaTypeSupported : getSupportedMediaTypes()) {
			if (mediaTypeSupported.equals(mediaType)) {
				supported = true;
				break;
			}
		}

		// Create informative message.
		if (!supported) {
			StringBuilder builder = new StringBuilder();
			builder.append("This convertor '");
			builder.append(this.getClass().getSimpleName());
			builder.append("' only supports ");

			boolean first = true;
			for (MediaType mediaTypeSupported : getSupportedMediaTypes()) {
				if (!first) {
					builder.append(" or");
				}
				first = false;
				builder.append(" '");
				builder.append(mediaTypeSupported.toString());
				builder.append("'");
			}
			builder.append("! (received '");
			builder.append(mediaType);
			builder.append("')");
			throw new IllegalArgumentException(builder.toString());
		}

	}

	/**
	 * Should not be overloaded by the concrete classes.
	 */
	@Override
	public final CisConversionResult convertToPdf(MediaType mediaType, String filename, InputStream inputStream,
			ConversionOption conversionOption) {

		checkForSupportedMediaType(mediaType);

		CisConversionResult result = new CisConversionResult();
		File file = null;

		try {
			// Save to disc
			file = UniqueFileGenerator.getUniqueFile(pdfOutputlocation, this.getClass().getSimpleName(), "pdf");

			result.setConversionOption(conversionOption);
			result.setMediaType(mediaType);
			result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(filename));
			result.setPdfResultFile(file);

			LOGGER.debug("Convert to file...        " + file.getName());
			convert(mediaType, inputStream, file, result, conversionOption);
			LOGGER.debug("Convert to file finished. " + file.getName());

		} catch (Exception e) {

			// Delete file if it exists.
			deleteFile(file);

			if (isPasswordException(e)) {
				result = CisConversionResult.createPasswordFailureResult(filename, conversionOption, mediaType, null);
			} else {
				result.setFailureReason(createTechnishefoutMsg(e));
			}

			// Clear the file to state that the conversion has failed.
			result.setPdfResultFile(null);
		}
		return result;
	}

	// protected Integer getNumberOfPages(InputStream inputStream) {
	// Integer result = null;
	//
	// if (inputStream != null) {
	// try {
	// BufferedInputStream bufferedInputStream = new
	// BufferedInputStream(inputStream);
	// // Open document
	// Document doc = new Document(bufferedInputStream);
	//
	// result = doc.getPages().size();
	//// doc.close();
	// } catch (Exception e) {
	// LOGGER.warn("Het bepalen van het aantal paginas niet mogelijk", e);
	// }
	// }
	//
	// return result;
	// }

	protected String getPdfOutputlocation() {
		return pdfOutputlocation;
	}

	protected String createTechnishefoutMsg(Exception e) {
		String tijdstip = DateUtil.getDateFormatSecondsHuman().format(new Date());
		LOGGER.warn("Conversion in " + this.getClass().getSimpleName() + " failed! (Tijdstip: " + tijdstip + ")", e);
		StringBuilder msg = new StringBuilder();
		msg.append(
				"Het omzetten naar pdf is mislukt door een technische fout. Neem contact op met de functioneel beheerder.");
		msg.append("(Tijdstip: ");
		msg.append(tijdstip);
		msg.append(")");
		return msg.toString();
	}

	protected void deleteFile(File file) {
		FileUtil.deleteFile(file);
	}

	/**
	 * Create a unique file in the pdfOutputLocation with the given extension
	 * 
	 * @param extension
	 *            is allowed to be null.
	 * @return
	 */
	protected File getUniqueFile() {

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		int count = atomicCount.addAndGet(1);

		// Save to disc
		String fileNamePdf = String.format("conv_%s_%s_%05d%s", this.getClass().getSimpleName(),
				format.format(new Date()), count, ".bin");
		return new File(pdfOutputlocation, fileNamePdf);
	}

	protected abstract boolean isPasswordException(Exception e);

}
