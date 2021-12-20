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
package nl.nn.adapterframework.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reference to an InputStream. Can NOT be accessed multiple times.
 * 
 * @author Niels Meijer
 *
 */
public class InputStreamResource extends Resource {
	private String name;
	private InputStream inputStream;

	public InputStreamResource(InputStream inputStream, String name) {
		super(new GlobalScopeProvider());

		this.inputStream = inputStream;
		this.name = name;
	}

	@Override
	public InputStream openStream() throws IOException {
		return inputStream;
	}

	@Override
	public String getSystemId() {
		return name;
	}

	@Override
	public String toString() {
		return "InputStreamResource name ["+name+"]";
	}
}
