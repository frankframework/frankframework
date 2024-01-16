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
package org.frankframework.util;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.frankframework.stream.Message;

public class MessageToStringResolver implements AdditionalStringResolver {
	@Override
	public Optional<String> resolve(String key, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		return getMessageFromMap(key, props1)
				.or(() -> getMessageFromMap(key, props2));
	}

	private Optional<String> getMessageFromMap(String key, Map<?, ?> map) {
		if (map == null || !map.containsKey(key)) {
			return Optional.empty();
		}
		Object val = map.get(key);
		if (!(val instanceof Message)) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(((Message) val).asString());
		} catch (IOException e) {
			// Do not get Logger early as this code might run during logger configuration.
			LogUtil.getLogger(MessageToStringResolver.class).error("Cannot get String representation for key [{}] of Message:", key, e);

			// Return an empty string instead of empty Optional, to stop further parameter
			// substitution beyond this point (we found the match, the match was broken.
			// Should not look for more matches as we might just find it again but call
			// .toString() which gives wrong result).
			return Optional.of("");
		}
	}
}
