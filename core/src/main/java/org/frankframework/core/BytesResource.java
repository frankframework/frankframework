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
package org.frankframework.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.util.StreamUtil;


/**
 * Reference to an byte[]. Can be accessed multiple times.
 *
 * @author Niels Meijer
 *
 */
public class BytesResource extends Resource {
	private final String name;
	private final byte[] bytes;

	public BytesResource(byte[] bytes, String name, IScopeProvider scopeProvider) {
		super(scopeProvider);
		if(StringUtils.isEmpty(name)) {
			throw new IllegalStateException("name may not be empty");
		}

		this.bytes = bytes;
		this.name = name;
	}

	public BytesResource(InputStream inputStream, String name, IScopeProvider scopeProvider) throws IOException {
		this(StreamUtil.streamToBytes(inputStream), name, scopeProvider);
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
		return "BytesResource name ["+name+"]";
	}
}
