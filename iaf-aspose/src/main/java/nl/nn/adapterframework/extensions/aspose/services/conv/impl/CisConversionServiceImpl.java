/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionException;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.Convertor;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.ConvertorFactory;
import nl.nn.adapterframework.extensions.aspose.services.util.DateUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.FileUtil;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public class CisConversionServiceImpl implements CisConversionService {

	private static final Logger LOGGER = Logger.getLogger(CisConversionServiceImpl.class);

	// location of converted pdf files
	private String pdfOutputlocation = "";

	private ConvertorFactory convertorFactory;

	private MediaTypeValidator mediaTypeValidator;

	private String fontsDirectory;

	private static AtomicInteger atomicCount = new AtomicInteger(1);

	public CisConversionServiceImpl(String pdfOutputLocation, String fontsDirectory) {
		this.pdfOutputlocation = pdfOutputLocation;
		this.setFontsDirectory(fontsDirectory);
		convertorFactory = new ConvertorFactory(this, pdfOutputlocation);
		mediaTypeValidator = new MediaTypeValidator(pdfOutputlocation);

	}

	@Override
	public CisConversionResult convertToPdf(InputStream inputStream, ConversionOption conversionOption) throws IOException {
		return convertToPdf(inputStream, null, conversionOption);
	}

	@Override
	public CisConversionResult convertToPdf(InputStream inputStream, String filename,
			ConversionOption conversionOption) throws IOException {

		// InputStream should always be available.
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream == null");
		}

		File tmpFile = null;

		try {

			tmpFile = getUniqueFile();
			// Create tmp file to prevent processes consuming inputstream
			Files.copy(inputStream, tmpFile.toPath());
			return convertToPdf(tmpFile, filename, conversionOption);

		} catch (IOException e) {
			LOGGER.error("Fout bij conversie van bestand " + filename + "naar PDF", e);
			throw createCisConversionException(e);
		} finally {
			// delete previously created temporary file
			FileUtil.deleteFile(tmpFile);
			tmpFile = null;
		}
	}

	private CisConversionResult convertToPdf(File file, String filename, ConversionOption conversionOption) {

		CisConversionResult result = null;
		MediaType mediaType = getMediaType(file, filename);

		if (isPasswordProtected(mediaType)) {

			result = CisConversionResult.createPasswordFailureResult(filename, conversionOption, mediaType);

		} else {
			// Get the convertor for the given mediatype.
			Convertor convertor = convertorFactory.getConvertor(mediaType);

			if (convertor == null) {
				// Conversion not supported.

				String message = "Omzetten naar PDF mislukt! Reden: bestandstype wordt niet ondersteund (mediaType: "
						+ mediaType + ")";
				result = createFailureResult(filename, conversionOption, mediaType, message);

			} else {

				long startTime = System.currentTimeMillis();
				// Convertor found, convert the file
				result = convertor.convertToPdf(mediaType, filename, file, conversionOption);

				LOGGER.info(String.format("Convert (in %d msec): mediatype: %s, filename: %s, attachmentoptions: %s",
						System.currentTimeMillis() - startTime, mediaType, filename, conversionOption));
			}
		}
		return result;
	}

	private CisConversionResult createFailureResult(String filename, ConversionOption conversionOption,
			MediaType mediaType, String message) {
		CisConversionResult result;
		StringBuilder msg = new StringBuilder();
		if (filename != null) {
			msg.append(filename);
		}
		msg.append(" " + message);

		LOGGER.info("Conversion not supported: " + msg.toString());

		result = CisConversionResult.createFailureResult(conversionOption, mediaType, filename, msg.toString());
		return result;
	}

	private boolean isPasswordProtected(MediaType mediaType) {
		return ("x-tika-ooxml-protected".equals(mediaType.getSubtype()));
	}

	private CisConversionException createCisConversionException(Exception e) {
		String tijdstip = DateUtil.getDateFormatSecondsHuman().format(new Date());
		LOGGER.warn("Conversion failed! (Tijdstip: " + tijdstip + ")", e);
		StringBuilder msg = new StringBuilder();
		msg.append(
				"Het omzetten naar pdf is mislukt door een technische fout. Neem contact op met de functioneel beheerder.");
		msg.append("(Tijdstip: ");
		msg.append(tijdstip);
		msg.append(")");
		return new CisConversionException(msg.toString(), e);
	}

	private MediaType getMediaType(File file, String filename) {

		MediaType mediaType = null;
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
			mediaType = mediaTypeValidator.getMediaType(inputStream, filename);
			LOGGER.debug("Mediatype received: " + mediaType);
		} catch (IOException e) {
			throw new CisConversionException(
					"Het omzetten naar pdf is mislukt. Neem contact op met de functioneel beheerder", e);
		}

		return mediaType;
	}

	/**
	 * Create a unique file in the pdfOutputLocation with the given extension
	 * 
	 * @param extension
	 *            is allowed to be null.
	 * @return
	 */
	private File getUniqueFile() {

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		int count = atomicCount.addAndGet(1);

		// Save to disc
		String fileNamePdf = String.format("%s_%s_%05d%s", this.getClass().getSimpleName(), format.format(new Date()),
				count, ".bin");
		return new File(pdfOutputlocation, fileNamePdf);

	}

	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputlocation = pdfOutputLocation;
	}

	@Override
	public String getFontsDirectory() {
		return fontsDirectory;
	}

	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}

}
