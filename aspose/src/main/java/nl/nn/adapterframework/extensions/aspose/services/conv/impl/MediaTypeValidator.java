/*
   Copyright 2019, 2021-2025 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.io.IOException;

import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.springframework.http.MediaType;

import nl.nn.adapterframework.stream.Message;

/**
 * Specific class to detect media type used by CisConversionServiceImpl
 *
 */
class MediaTypeValidator {

	private Tika tika;

	/**
	 * Package default access because it specific for the conversion.
	 */
	public MediaTypeValidator() {
		// Create only once. Tika seems to be thread safe
		// (see http://stackoverflow.com/questions/10190980/spring-tika-integration-is-my-approach-thread-safe)
		tika = new Tika();
	}

	/**
	 * Detects media type from input stream
	 */
	public MediaType getMediaType(Message message, String filename) throws IOException {
		try (TikaInputStream tis = TikaInputStream.get(message.asInputStream())) {
			String type = tika.detect(tis, filename);
			return MediaType.valueOf(type);
		}
	}

}