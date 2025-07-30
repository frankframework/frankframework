/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.parameters;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.frankframework.core.ParameterException;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlException;

/**
 * A parameter which represents its value as a {@link Message} with mimetype {@literal application/json}. If the
 * derived value was of type {@literal application/xml} then it will be converted into JSON using the same rules as the
 * {@link org.frankframework.pipes.JsonPipe}.
 * If the derived value of the parameter was neither XML nor JSON format then a JSON will be constructed that looks like
 * {@code {"paramName":value}}. (The value will be quoted if it was not a number or boolean value).
 */
public class JsonParameter extends AbstractParameter {

	public JsonParameter() {
		setType(ParameterType.JSON);
	}

	@Override
	protected Object getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		try {
			Message result = MessageUtils.convertToJsonMessage(request, getName());
			// Caller closes the request-message. So if we get back the same instance as passed in we need to copy it.
			if (result == request) return result.copyMessage();
			return result;
		} catch (XmlException e) {
			throw new ParameterException("Cannot convert value to JSON", e);
		}
	}
}
