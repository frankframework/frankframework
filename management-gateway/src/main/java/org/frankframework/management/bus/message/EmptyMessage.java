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

public class EmptyMessage extends StringMessage {

	private static final String EMPTY_RESPONSE = "no-content";

	public EmptyMessage(int statuscode) {
		super(EMPTY_RESPONSE);
		setStatus(statuscode);
	}

	private static EmptyMessage createNoContentResponse(int statuscode) {
		return new EmptyMessage(statuscode);
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
