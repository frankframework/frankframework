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
import java.util.List;

import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;

/**
 * 
 * @author M64D844
 *
 */
public interface Convertor {

	/**
	 * Returns the supported media types.
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * Converts the given file to a pdf. MediaType is the detected media type of the
	 * file. The convertor should support the given mediatype (otherwise it gives
	 * programming error).
	 * 
	 * @param mediaType
	 * @param filename
	 * @param file
	 * @param conversionOption
	 */
	CisConversionResult convertToPdf(MediaType mediaType, String filename, File file, ConversionOption conversionOption);

}
