package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.tika.Tika;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.mime.MediaType;

/**
 * Specific class used by the {@link CisConversionServiceImpl} class.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
class MediaTypeValidator {

	private Tika tika;

	private TemporaryResources tmp;

	/**
	 * Package default access because it specific for the conversion.
	 */
	MediaTypeValidator(String pdfOutputlocation) {
		// Create only once. Tika seems to be thread safe
		// (see
		// http://stackoverflow.com/questions/10190980/spring-tika-integration-is-my-approach-thread-safe)
		tika = new Tika();
		tmp = new TemporaryResources();
		tmp.setTemporaryFileDirectory(Paths.get(pdfOutputlocation));
	}

	/**
	 * The stream will be reset back to the point as received used the marked and
	 * reset method are supported by the given inputstream.
	 * 
	 * @param inputStream
	 * @return returns the media type. If media type could not be detected
	 *         <code>null</code> is returned.
	 * @throws IOException
	 */
	MediaType getMediaType(InputStream inputStream, String filename) throws IOException {
		try (TikaInputStream tis = TikaInputStream.get(inputStream, tmp)) {
			String type = tika.detect(tis, filename);
			System.out.println(type);
			return MediaType.parse(type);
		}
	}

}