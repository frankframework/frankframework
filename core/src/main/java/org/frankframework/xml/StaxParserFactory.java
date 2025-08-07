/*
   Copyright 2020 Integration Partners

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
package org.frankframework.xml;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.stax.WstxInputFactory;

/**
 * Descendant of Woodstox XMLInputFactory, that accepts XML 1.1 compliant content in XML 1.0 documents too.
 * This is introduced to solve a problem in ExchangeMailListener, where illegal characters are seen inside Exchange-documents.
 *
 * @author Gerrit van Brakel
 *
 */
public class StaxParserFactory extends WstxInputFactory {

	/**
	 * Enable XML 1.1, to avoid errors like:
	 * Illegal character entity: expansion character (code 0x3 at [row,col {unknown-source}]: [1,53]
	 */
	@Override
	public ReaderConfig createPrivateConfig() {
		ReaderConfig result = super.createPrivateConfig();
		result.enableXml11(true);
		return result;
	}

}
