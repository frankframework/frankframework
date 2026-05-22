/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.management.gateway;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public final class ImmutableMessage<T> extends GenericMessage<T> implements Message<T> {

	public ImmutableMessage(T payload, Map<String, Object> headers) {
		super(payload, headers);
	}

	@Override
	public String toString() {
		return "ImmutableMessage@" + Integer.toHexString(hashCode());
	}
}