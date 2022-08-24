/*
   Copyright 2022 WeAreFrank!

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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;

public class ResponseMessage {
	public static final String STATUS_KEY = "meta:status";
	public static final String MIMETYPE_KEY = "meta:type";
	public static final String NO_CONTENT_PAYLOAD = "no-content";

	public static Message<String> ok(String payload) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 200);
		return new GenericMessage<>(payload, headers);
	}

	public static Message<InputStream> ok(InputStream configFlow, MimeType mediaType) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 200);
		headers.put(MIMETYPE_KEY, mediaType.toString());
		return new GenericMessage<>(configFlow, headers);
	}

	public static Message<String> noContent() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(STATUS_KEY, 204);
		return new GenericMessage<>(NO_CONTENT_PAYLOAD, headers);
	}
}
