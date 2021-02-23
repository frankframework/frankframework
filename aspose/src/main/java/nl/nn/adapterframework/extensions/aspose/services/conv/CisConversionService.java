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
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.io.IOException;
import java.io.InputStream;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public interface CisConversionService {

	/**
	 * This will try to convert the given inputStream to a pdf.
	 * <p>
	 * The given document stream is <em>not</em> closed by this method.
	 * 
	 * @param inputStream
	 * @param filename
	 *            (without the path). Is used to detect mediatype and inform the
	 *            user of the name of the file. Is allowed to be null.
	 * @throws IOException 
	 * @throws CisConversionException
	 *             when a failure occurs.
	 */
	CisConversionResult convertToPdf(InputStream inputStream, String filename, ConversionOption conversionOption) throws IOException;

	/**
	 * This will try to convert the given inputStream to a pdf.
	 * <p>
	 * The given document stream is <em>not</em> closed by this method.
	 * 
	 * @param inputStream
	 * @throws IOException 
	 * @throws CisConversionException
	 *             when a failure occurs.
	 */
	CisConversionResult convertToPdf(InputStream inputStream, ConversionOption conversionOption) throws IOException;

	String getFontsDirectory();
}
