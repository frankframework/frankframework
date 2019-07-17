package nl.nn.adapterframework.extensions.aspose.pipe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.AsposeLicenseLoader;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.CisConversionServiceImpl;
import nl.nn.adapterframework.pipes.FixedForwardPipe;

public class PdfPipe extends FixedForwardPipe {

	private static final Logger LOGGER = Logger.getLogger(PdfPipe.class);

	private boolean saveSeparate = false;
	private CisConversionService cisConversionService;
	private String pdfOutputLocation = null;
	private String license = null;
	private AsposeLicenseLoader loader;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(pdfOutputLocation)) {
			LOGGER.debug(
					"PDF output location is not set. Temporary directory will be created to store converted files.");
			try {
				pdfOutputLocation = Files.createTempDirectory("Pdf").toString();
				LOGGER.debug("Temporary directory path : " + pdfOutputLocation);
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
			// throw new ConfigurationException("Pdf output location cannot be null. Please
			// specify a location.");
		}

		File outputLocation = new File(pdfOutputLocation);
		if (!outputLocation.exists()) {
			throw new ConfigurationException(
					"Pdf output location does not exists. Please specify an existing location ");
		}

		if (!outputLocation.isDirectory()) {
			throw new ConfigurationException("Pdf output location is not directory. Please specify a diretory");
		}

		try {
			loader = new AsposeLicenseLoader(license);
			loader.loadLicense();
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}

		cisConversionService = new CisConversionServiceImpl(pdfOutputLocation);
	}

	@Override
	public void start() throws PipeStartException {
		super.start();

	}

	@Override
	public void stop() {
		// TODO cleanup iets
		super.stop();
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		InputStream binaryInputStream = null;

		if (input instanceof InputStream) {
			binaryInputStream = (InputStream) input;
		} else if (input instanceof byte[]) {
			binaryInputStream = new ByteArrayInputStream((byte[]) input);
		} else {
			// TODO: do something here (ask about string)
		}

		CisConversionResult cisConversionResult = cisConversionService.convertToPdf(binaryInputStream,
				saveSeparate ? ConversionOption.SEPERATEPDF : ConversionOption.SINGLEPDF);
		System.err.println(cisConversionResult.toString());
		if (cisConversionResult.getFailureReason() != null) {

		}
		session.put("result", cisConversionResult);
		session.put("CONVERSION_OPTION", cisConversionResult.getConversionOption().name());
		session.put("MEDIA_TYPE", cisConversionResult.getMediaType().toString());
		session.put("DOCUMENT_NAME", cisConversionResult.getDocumentName());
		session.put("AMOUNT_OF_PAGES", null);
		session.put("FAILURE_REASON", cisConversionResult.getFailureReason());
		session.put("PARENT_ID", "3");
		session.put("FILE_CONTENT", cisConversionResult.getFileStream());

		return new PipeRunResult(getForward(), "");
	}

	public boolean isSaveSeparate() {
		return saveSeparate;
	}

	public void setSaveSeparate(boolean saveSeparate) {
		this.saveSeparate = saveSeparate;
	}

	public String getPdfOutputLocation() {
		return pdfOutputLocation;
	}

	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputLocation = pdfOutputLocation;
	}
}
