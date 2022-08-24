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

import javax.ws.rs.core.Response;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class BusMessageUtils {


	public static boolean containsHeader(Message<?> message, String headerName) {
		return message.getHeaders().get(headerName) != null;
	}

	public static boolean getHeader(Message<?> message, String headerName, boolean defaultValue) {
		Object header = message.getHeaders().get(headerName);
		if(header == null) {
			return defaultValue;
		}

		return Boolean.parseBoolean(""+header);
	}

	public static Response convertToJaxRsResponse(Message<?> response) {
		MessageHeaders headers = response.getHeaders();
		int status = (int) headers.get(ResponseMessage.STATUS_KEY);
		String mimeType = (String) headers.get(ResponseMessage.MIMETYPE_KEY);

		return Response.status(status).entity(response.getPayload()).type(mimeType).build();
	}
}
