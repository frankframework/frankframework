/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.testtool;

import org.frankframework.util.XmlEncodingUtils;

public class Util {

	public static String throwableToXml(Throwable throwable) {
		String xml = "<throwable>";
		xml = xml + "<class>" + throwable.getClass().getName() + "</class>";
		xml = xml + "<message>" + XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters((throwable.getMessage()))) + "</message>";
		Throwable cause = throwable.getCause();
		if (cause != null) {
			xml = xml + "<cause>" + throwableToXml(cause) + "</cause>";
		}
		xml = xml + "</throwable>";
		return xml;
	}

}
