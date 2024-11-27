/*
   Copyright 2018, 2021 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;

import org.apache.http.Header;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;

@Deprecated(forRemoval = true, since = "7.6.0")
public class RestSender extends HttpSender {

	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
		String responseString = super.extractResult(responseHandler, session).asString();
		int statusCode = responseHandler.getStatusLine().getStatusCode();
		XmlBuilder result = new XmlBuilder("result");

		XmlBuilder statuscode = new XmlBuilder("statuscode");
		statuscode.setValue(statusCode + "");
		result.addSubElement(statuscode);

		Header[] headers = responseHandler.getAllHeaders();
		if(headers != null) {
			XmlBuilder headersXml = new XmlBuilder("headers");
			for (Header header : headers) {
				String name = header.getName().toLowerCase();
				XmlBuilder headerXml = new XmlBuilder("header");
				headerXml.addAttribute("name", name);
				headerXml.addAttribute("value", header.getValue());
				headersXml.addSubElement(headerXml);
			}
			result.addSubElement(headersXml);
		}

		if (validateResponseCode(statusCode)) {
			XmlBuilder message = new XmlBuilder("message");
			message.setValue(responseString, false);
			result.addSubElement(message);
		}
		else {
			XmlBuilder message = new XmlBuilder("error");
			message.setValue(responseString);
			result.addSubElement(message);
		}

		return result.asMessage();
	}
}
