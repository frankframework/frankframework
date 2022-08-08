/*
   Copyright 2019, 2021 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.io.IOException;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.stream.Message;
/**
 * @author
 * 	Gerard van der Hoorn
 */
public interface CisConversionService {

	/**
	 * This will try to convert the given inputStream to a pdf.
	 * <p>
	 * The given document stream is <em>not</em> closed by this method.
	 *
	 * @param input
	 * @param filename
	 *            (without the path). Is used to detect mediatype and inform the
	 *            user of the name of the file. Is allowed to be null.
	 * @throws IOException
	 * @throws CisConversionException
	 *             when a failure occurs.
	 */
	CisConversionResult convertToPdf(Message input, String filename, ConversionOption conversionOption) throws IOException;
}
