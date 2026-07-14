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
package org.frankframework.management.bus.message;

import org.springframework.http.HttpStatus;

public class EmptyMessage extends StringMessage {

	private static final String EMPTY_RESPONSE = "no-content";

	public EmptyMessage(int statuscode) {
		this(statuscode, getErrorMessage(statuscode));
	}
	public EmptyMessage(int statuscode, String errorMessage) {
		super(errorMessage);
		setStatus(statuscode);
	}

	private static String getErrorMessage(int statuscode) {
		HttpStatus status = HttpStatus.resolve(statuscode);
		return status != null ? status.getReasonPhrase() : EMPTY_RESPONSE;
	}

	private static EmptyMessage createNoContentResponse(int statuscode) {
		return new EmptyMessage(statuscode, EMPTY_RESPONSE);
	}

	public static EmptyMessage created() {
		return createNoContentResponse(201);
	}

	public static EmptyMessage accepted() {
		return createNoContentResponse(202);
	}

	public static EmptyMessage noContent() {
		return createNoContentResponse(204);
	}
}
