/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.ibistesttool.transform;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.testtool.transform.MessageTransformer;

import org.apache.commons.lang.StringUtils;

/**
 * Hide the same data as is hidden in the Ibis logfiles based on the
 * log.hideRegex property in log4j4ibis.properties.
 * 
 * @author Jaco de Groot
 */
public class HideRegexMessageTransformer implements MessageTransformer {
	String hideRegex;

	HideRegexMessageTransformer() {
		hideRegex = LogUtil.getLog4jHideRegex();
	}

	public String transform(String message) {
		if (message != null) {
			if (StringUtils.isNotEmpty(hideRegex)) {
				message = Misc.hideAll(message, hideRegex);
			}

			String threadHideRegex = LogUtil.getThreadHideRegex();
			if (StringUtils.isNotEmpty(threadHideRegex)) {
				message = Misc.hideAll(message, threadHideRegex);
			}
		}
		return message;
	}

	public String getHideRegex() {
		return hideRegex;
	}

	public void setHideRegex(String string) {
		hideRegex = string;
	}

}
