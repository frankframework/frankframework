/*
   Copyright 2019 Integration Partners

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.mime.MediaType;

import com.aspose.pdf.Document;
import com.aspose.pdf.LoadOptions;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.XpsLoadOptions;
import com.aspose.pdf.exceptions.InvalidPasswordException;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * Converts the files which are required and supported by the aspose pdf
 * library.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public class PdfConvertor extends AbstractConvertor {

	// contains mapping from MediaType to the LoadOption for the aspose word
	// conversion.
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		// The string value is defined in com.aspose.pdf.LoadOptions.
		// CIS-44: Tijdelijk gedisabled omdat html conversie op A (en P) niet goed gaat.
		// Moet nog worden geanalyseerd.
//		map.put(new MediaType("text", "html"), new HtmlLoadOptions());
//		map.put(new MediaType("application", "xhtml+xml"), new HtmlLoadOptions());
		map.put(new MediaType("application", "vnd.ms-xpsdocument"), new XpsLoadOptions());
		map.put(new MediaType("application", "x-tika-ooxml"), new XpsLoadOptions());

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	PdfConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation,
				MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet().toArray(new MediaType[MEDIA_TYPE_LOAD_FORMAT_MAPPING.size()]));
	}

	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			// mediaType should always be supported otherwise there a program error because
			// the supported media types should be part of the map
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		try (InputStream inputStream = new FileInputStream(file)) {
			Document doc = new Document(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
			doc.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.Pdf);
			doc.freeMemory();
			doc.dispose();
			doc.close();
		}
		result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
