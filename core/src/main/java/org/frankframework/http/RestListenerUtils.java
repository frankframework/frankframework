/*
   Copyright 2016-2018, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.frankframework.core.PipeLineSession;
import org.frankframework.util.StreamUtil;

/**
 * Some utilities for working with {@link RestListener}.
 *
 * @author Peter Leeuwenburgh
 */
public class RestListenerUtils {

	public static String retrieveRequestURL(PipeLineSession session) {
		HttpServletRequest request = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (request != null) {
			return request.getRequestURL().toString();
		}
		return null;
	}

	@Deprecated
	public static void writeToResponseOutputStream(PipeLineSession session, InputStream input) throws IOException {
		OutputStream output = retrieveServletOutputStream(session);
		StreamUtil.copyStream(input, output, 30000);
	}

	private static ServletOutputStream retrieveServletOutputStream(PipeLineSession session) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if (response != null) {
			return response.getOutputStream();
		}
		return null;
	}

	@Deprecated
	public static void setResponseContentType(PipeLineSession session, String contentType) {
		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if (response != null) {
			response.setContentType(contentType);
		}
	}
}
