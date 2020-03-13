/*
   Copyright 2018 Integration Partners

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import org.apache.http.Header;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.XmlBuilder;

public class RestSender extends HttpSender {

	@Override
	protected String extractResult(HttpResponseHandler responseHandler, IPipeLineSession session) throws SenderException, IOException {
		String responseString = super.extractResult(responseHandler, session);
		int statusCode = responseHandler.getStatusLine().getStatusCode();
		XmlBuilder result = new XmlBuilder("result");

		XmlBuilder statuscode = new XmlBuilder("statuscode");
		statuscode.setValue(statusCode + "");
		result.addSubElement(statuscode);

		XmlBuilder headersXml = new XmlBuilder("headers");
		Header[] headers = responseHandler.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			Header header = headers[i];
			String name = header.getName().toLowerCase();
			XmlBuilder headerXml = new XmlBuilder("header");
			headerXml.addAttribute("name", name);
			headerXml.addAttribute("value", header.getValue());
			headersXml.addSubElement(headerXml);
		}
		result.addSubElement(headersXml);

		if (statusCode == HttpURLConnection.HTTP_OK) {
			XmlBuilder message = new XmlBuilder("message");
			message.setValue(responseString, false);
			result.addSubElement(message);
		}
		else {
			XmlBuilder message = new XmlBuilder("error");
			message.setValue(responseString);
			result.addSubElement(message);
		}

		return result.toXML();
	}
}
