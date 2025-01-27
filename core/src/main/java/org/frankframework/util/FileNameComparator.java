/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package org.frankframework.util;

import java.io.File;

/**
 * Compares filenames, so directory listings appear in a kind of natural order.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class FileNameComparator extends AbstractNameComparator<File> {

	public static int compareFilenames(File f0, File f1) {
		if (f0.isDirectory()!=f1.isDirectory()) {
			if (f0.isDirectory()) {
				return -1;
			}
			return 1;
		}

		return compareNames(f0.getName(), f1.getName());
	}

	@Override
	public int compare(File arg0, File arg1) {
		return compareFilenames(arg0, arg1);
	}
}
