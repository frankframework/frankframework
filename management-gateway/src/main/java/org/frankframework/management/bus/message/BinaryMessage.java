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
package org.frankframework.management.bus.message;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

public class BinaryMessage extends MessageBase<InputStream> {

	public BinaryMessage(byte[] payload) {
		this(new ByteArrayInputStream(payload));
	}

	public BinaryMessage(InputStream payload) {
		this(payload, MediaType.APPLICATION_OCTET_STREAM);
	}

	public BinaryMessage(byte[] payload, MimeType defaultMimeType) {
		this(new ByteArrayInputStream(payload), defaultMimeType);
	}

	public BinaryMessage(InputStream payload, MimeType defaultMimeType) {
		super(payload, defaultMimeType);
	}
}
