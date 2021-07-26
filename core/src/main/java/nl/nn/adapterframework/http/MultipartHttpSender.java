/*
   Copyright 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.mime.FormBodyPart;
import org.w3c.dom.Element;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class MultipartHttpSender extends HttpSender {

	public MultipartHttpSender() {
		setPostType(PostType.FORMDATA.name());
		setMethodType("POST");
		setFirstBodyPartName("message");
	}

	@Override
	protected FormBodyPart elementToFormBodyPart(Element element, PipeLineSession session) throws IOException {
		String partType = element.getAttribute("type"); //File or otherwise
		String partName = element.getAttribute("name"); //Name of the part
		String fileName = (StringUtils.isNotEmpty(element.getAttribute("filename"))) ? element.getAttribute("filename") : element.getAttribute("fileName"); //Name of the file
		String sessionKey = element.getAttribute("sessionKey"); //SessionKey to retrieve data from
		String mimeType = element.getAttribute("mimeType"); //MimeType of the part
		String partValue = element.getAttribute("value"); //Value when SessionKey is empty or not set
		Message partObject = session.getMessage(sessionKey);

		if (partObject != null && partObject.isBinary()) {
			InputStream fis = partObject.asInputStream();

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
			String value = partObject.asString();
			if(StringUtils.isEmpty(value))
				value = partValue;

			return createMultipartBodypart(partName, value, mimeType);
		}
	}
}
