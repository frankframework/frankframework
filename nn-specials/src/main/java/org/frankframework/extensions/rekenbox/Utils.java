/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.extensions.rekenbox;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.frankframework.util.StreamUtil;

public class Utils {

	private Utils() {
		// do not construct utility class
	}

	/**
	 * Please consider using {@link StreamUtil#resourceToString(URL, String, boolean)} instead of relying on files.
	 */
	@Deprecated
	public static String fileToString(String fileName, String endOfLineString, boolean xmlEncode) throws IOException {
		try (FileReader reader = new FileReader(fileName)) {
			return StreamUtil.readerToString(reader, endOfLineString, xmlEncode);
		}
	}
}
