/*
   Copyright 2018 Nationale-Nederlanden, 2023 WeAreFrank!

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

import java.util.Set;

import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.Misc;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.transform.MessageTransformer;

/**
 * Hide the same data as is hidden in the Ibis logfiles based on the
 * log.hideRegex property in log4j4ibis.properties.
 * 
 * @author Jaco de Groot
 */
public class HideRegexMessageTransformer implements MessageTransformer {
	Set<String> hideRegex;

	HideRegexMessageTransformer() {
		hideRegex = IbisMaskingLayout.getGlobalReplace();
	}

	@Override
	public String transform(Checkpoint checkpoint, String message) {
		if (message != null) {
			message = Misc.hideAll(message, hideRegex);

			Set<String> threadHideRegex = IbisMaskingLayout.getThreadLocalReplace();
			message = Misc.hideAll(message, threadHideRegex);
		}
		return message;
	}

	public Set<String> getHideRegex() {
		return hideRegex;
	}

	public void setHideRegex(Set<String> string) {
		hideRegex = string;
	}

}
