/*
   Copyright 2016-2018, 2020 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import jakarta.servlet.http.HttpServletRequest;

import org.frankframework.core.PipeLineSession;

/**
 * One utility for working with WSDL Generator Pipes.
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
}
