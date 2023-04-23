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
package nl.nn.adapterframework.management.bus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

public class BinaryResponseMessage extends ResponseMessageBase<InputStream> {

	public BinaryResponseMessage(byte[] payload) {
		this(new ByteArrayInputStream(payload));
	}

	public BinaryResponseMessage(InputStream payload) {
		this(payload, MediaType.APPLICATION_OCTET_STREAM);
	}

	public BinaryResponseMessage(byte[] payload, MimeType defaultMimeType) {
		this(new ByteArrayInputStream(payload), defaultMimeType);
	}

	public BinaryResponseMessage(InputStream payload, MimeType defaultMimeType) {
		super(payload, defaultMimeType);
	}
}
