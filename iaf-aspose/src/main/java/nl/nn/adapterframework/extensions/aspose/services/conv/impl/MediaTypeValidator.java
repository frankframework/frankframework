package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.tika.Tika;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.mime.MediaType;

/**
 * Specific class to detect media type used by CisConversionServiceImpl
 *
 */
class MediaTypeValidator {

	private Tika tika;

	private String pdfOutputlocation;

	/**
	 * Package default access because it specific for the conversion.
	 */
	public MediaTypeValidator(String pdfOutputlocation) {
		// Create only once. Tika seems to be thread safe
		// (see
		// http://stackoverflow.com/questions/10190980/spring-tika-integration-is-my-approach-thread-safe)
		tika = new Tika();
		this.pdfOutputlocation = pdfOutputlocation;
	}

	/**
	 * Detects media type from input stream
	 * 
	 * @param inputStream
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public MediaType getMediaType(InputStream inputStream, String filename) throws IOException {
		// Create every time as TemporaryResources is not thread-safe
		TemporaryResources tmp = new TemporaryResources();
		tmp.setTemporaryFileDirectory(Paths.get(pdfOutputlocation));
		try (TikaInputStream tis = TikaInputStream.get(inputStream, tmp)) {
			String type = tika.detect(tis, filename);
			return MediaType.parse(type);
		}
	}

}