/*
   Copyright 2019, 2021-2022 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

import java.util.Set;

import org.springframework.http.MediaType;

import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.stream.Message;

public interface Convertor {

	/**
	 * Returns the supported media types.
	 */
	Set<MediaType> getSupportedMediaTypes();

	/**
	 * Converts the given file to a pdf. MediaType is the detected media type of the
	 * file. The convertor should support the given mediatype (otherwise it gives
	 * programming error).
	 * @param charset
	 *
	 */
	CisConversionResult convertToPdf(MediaType mediaType, String filename, Message message, ConversionOption conversionOption, String charset);

}
