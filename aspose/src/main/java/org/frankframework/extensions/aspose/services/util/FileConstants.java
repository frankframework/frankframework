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
package org.frankframework.extensions.aspose.services.util;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public class FileConstants {

	/**
	 * The specific allowed characters in a filename (with the exception of digits
	 * and letters).
	 */
	public static final String ALLOWED_SPECIFIC_CHARACTERS_IN_NAME = "!#$%&()=@^_";

	/**
	 * The specific allowed characters in filename of digits and letters.
	 */
	public static final String ALLOWED_CHARACTERS_IN_NAME = "0-9a-zA-Z ";

	/**
	 * The specific allowed characters in a filename of digits and letters and
	 * special characters.
	 */
	public static final String ALLOWED_CHARACTERS_IN_NAME_REGEX = "[" + ALLOWED_CHARACTERS_IN_NAME
			+ ALLOWED_SPECIFIC_CHARACTERS_IN_NAME + "]+";

	/**
	 * The inverse of ALLOWED_CHARACTERS_IN_NAME_REGEX.
	 */
	public static final String REPLACE_CHARACTERS_IN_NAME_REGEX = "[^" + ALLOWED_CHARACTERS_IN_NAME
			+ ALLOWED_SPECIFIC_CHARACTERS_IN_NAME + "]+";

	/**
	 * The replace character.
	 */
	public static final String REPLACE_CHARACTER = "_";

	/**
	 * Private constructor to prevent constructing this class.
	 */
	private FileConstants() {
	}

}
