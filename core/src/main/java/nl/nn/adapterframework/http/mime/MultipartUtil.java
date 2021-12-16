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
package nl.nn.adapterframework.http.mime;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.http.InputStreamDataSource;

public abstract class MultipartUtil {

	public static boolean isMultipart(HttpServletRequest request) {
		String httpMethod = request.getMethod().toUpperCase();
		if("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) {
			return request.getContentType().startsWith("multipart/");
		}
		return false;
	}

	public static MimeMultipart parse(HttpServletRequest request) throws IOException, MessagingException {
		InputStreamDataSource dataSource = new InputStreamDataSource(request.getContentType(), request.getInputStream()); //the entire InputStream will be read here!
		MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
//		mimeMultipart.
		return mimeMultipart;
	}
}
