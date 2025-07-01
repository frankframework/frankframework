/*
   Copyright 2021, 2022 WeAreFrank!

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
package org.frankframework.configuration.digester;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Java representation of a digester rule specified in the digester-rules.xml file.
 *
 * @author Niels Meijer
 */
public class DigesterRule {

	/**
	 * The digester rule's pattern.
	 */
	private @Getter @Setter String pattern;

	/**
	 * The class to use when creating the bean through Spring.
	 */
	private @Getter @Setter String beanClass;

	/**
	 * The 'factory-create-rule' attribute.
	 * When non specified it uses the GenericFactory. When specified as
	 * NULL-String it does not use a factory.
	 */
	private @Getter @Setter String factory;

	/**
	 * The 'set-next-rule' attribute. Register the just-created-object on it's parent.
	 */
	private @Getter @Setter String registerMethod;

	/**
	 * The 'registerTextMethod()' attribute. Add the element body text to the parent.
	 */
	private @Getter @Setter String registerTextMethod;


	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
