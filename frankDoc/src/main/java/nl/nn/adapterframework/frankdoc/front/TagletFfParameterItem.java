/* 
Copyright 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc.front;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

public class TagletFfParameterItem {
	private @Getter String parameterName;
	private @Getter String description;

	TagletFfParameterItem(String text) {
		if(StringUtils.isBlank(text)) {
			parameterName = "-";
			description = "";
		} else {
			parameterName = text.split("[ \\t]")[0];
			description = text.substring(parameterName.length()).trim();
		}
	}
}
