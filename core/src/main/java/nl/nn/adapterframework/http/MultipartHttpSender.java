/*
   Copyright 2019 Nationale-Nederlanden

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
import java.io.InputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.mime.FormBodyPart;
import org.w3c.dom.Element;

public class MultipartHttpSender extends HttpSender {

	@Override
	public void configure() throws ConfigurationException {
		setMethodType("POST");
		setInputMessageParam("message");
		setParamsInUrl(false);
		setMultipart(true);

		super.configure();
	}

	@Override
	protected FormBodyPart elementToFormBodyPart(Element element, IPipeLineSession session) {
		String partType = element.getAttribute("type"); //File or otherwise
		String partName = element.getAttribute("name"); //Name of the part
		String fileName = (StringUtils.isNotEmpty(element.getAttribute("filename"))) ? element.getAttribute("filename") : element.getAttribute("fileName"); //Name of the file
		String sessionKey = element.getAttribute("sessionKey"); //SessionKey to retrieve data from
		String mimeType = element.getAttribute("mimeType"); //MimeType of the part
		String partValue = element.getAttribute("value"); //Value when SessionKey is empty or not set
		Object partObject = session.get(sessionKey);

		if (partObject != null && partObject instanceof InputStream) {
			InputStream fis = (InputStream) partObject;

			if(StringUtils.isNotEmpty(fileName)) {
				return createMultipartBodypart(partName, fis, fileName, mimeType);
			}
			else if("file".equalsIgnoreCase(partType)) {
				return createMultipartBodypart(sessionKey, fis, partName, mimeType);
			}
			else {
				return createMultipartBodypart(partName, fis, null, mimeType);
			}
		} else {
			String value = (String) session.get(sessionKey);
			if(StringUtils.isEmpty(value))
				value = partValue;

			return createMultipartBodypart(partName, value, mimeType);
		}
	}

	/**
	 * Automatically detect if the response is a multipart response or not. (duh!)
	 */
	@Override
	protected Message extractResult(HttpResponseHandler responseHandler, IPipeLineSession session) throws SenderException, IOException {
		String contentType = responseHandler.getHeader("content-type");
		if(contentType != null)
			setMultipartResponse(contentType.contains("multipart"));

		return super.extractResult(responseHandler, session);
	}
}
