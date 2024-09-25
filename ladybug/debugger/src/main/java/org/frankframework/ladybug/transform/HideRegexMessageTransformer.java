/*
   Copyright 2018 Nationale-Nederlanden, 2023-2024 WeAreFrank!

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
package org.frankframework.ladybug.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.transform.MessageTransformer;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.logging.IbisMaskingLayout;
import org.frankframework.util.StringUtil;

/**
 * Hide the same data as is hidden in the Ibis logfiles based on the
 * log.hideRegex property in log4j4ibis.properties and {@link org.frankframework.core.IPipe#setHideRegex(String)}.
 *
 * @author Jaco de Groot
 */
public class HideRegexMessageTransformer implements MessageTransformer {
	Map<String, Pattern> hideRegex = new HashMap<>();

	@Override
	public String transform(Checkpoint checkpoint, String message) {
		if (StringUtils.isBlank(message)) {
			return message;
		}
		String result = IbisMaskingLayout.maskSensitiveInfo(message);
		// Apply additional regexes that may have been set specifically on the MessageTransformer
		return StringUtil.hideAll(result, hideRegex.values());
	}

	public Set<String> getHideRegex() {
		return hideRegex.keySet();
	}

	public void setHideRegex(Set<String> string) {
		hideRegex = string.stream()
				.map(re -> Map.entry(re, Pattern.compile(re)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
