/*
   Copyright 2013, 2014 Nationale-Nederlanden, 2020 - 2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import nl.nn.adapterframework.stream.Message;

public class MessageToStringResolver implements AdditionalStringResolver {
	@Override
	public Optional<String> resolve(String key, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		// TODO: With Java9 or higher this code can be cleaner, using Optional<>.or()
		return getMessageFromMap(key, props1)
				.map(Optional::of)
				.orElseGet(()-> getMessageFromMap(key, props2));
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
			return Optional.of(((Message) val).asString());
		} catch (IOException e) {
			// Do not get Logger early as this code might run during logger configuration.
			LogUtil.getLogger(MessageToStringResolver.class).error("Cannot get String representation for key [{}] of Message:", key, e);
			return Optional.of("");
		}
	}
}
