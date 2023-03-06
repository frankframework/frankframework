/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;

public interface StringDataSource {

	/**
	 * Return contents as String.
	 *
	 * This method exists separate from {@link Object#toString()} so that the contents can be
	 * returned while {@link Object#toString()} will describe the object.
	 *
	 * @return Contents of the object as String
	 * @throws IOException If an exception occurs during getting the String data.
	 */
	String asString() throws IOException;

	/**
	 * Is the data representation of the implementing object natively String data,
	 * or will the data be translated to String by calling {@link #asString()}.
	 *
	 * @return {@code true} if data is natively a String data, {@code false} if the data needs
	 * to be translated into String by calling {@link #asString()}.
	 */
	boolean isStringNative();
}
