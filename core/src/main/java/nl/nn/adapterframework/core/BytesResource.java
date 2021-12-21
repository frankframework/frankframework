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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.nn.credentialprovider.util.Misc;

/**
 * Reference to an byte[]. Can be accessed multiple times.
 * 
 * @author Niels Meijer
 *
 */
public class BytesResource extends Resource {
	private String name;
	private byte[] bytes;

	public BytesResource(byte[] bytes, String name) {
		super(new GlobalScopeProvider());

		this.bytes = bytes;
		this.name = name;
	}

	public BytesResource(InputStream inputStream, String name) throws IOException {
		this(Misc.streamToBytes(inputStream), name);
	}

	@Override
	public InputStream openStream() throws IOException {
		return new ByteArrayInputStream(bytes);
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
