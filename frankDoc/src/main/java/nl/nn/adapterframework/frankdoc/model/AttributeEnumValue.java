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

package nl.nn.adapterframework.frankdoc.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.frankdoc.doclet.FrankAnnotation;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.FrankEnumConstant;
import nl.nn.adapterframework.util.LogUtil;

public class AttributeEnumValue {
	private static Logger log = LogUtil.getLogger(AttributeEnumValue.class);

	private static final String ENUM_LABEL = EnumLabel.class.getName();

	private @Getter boolean explicitLabel = false;
	private @Getter String javaTag;
	private @Getter String label;
	private @Getter String description;

	AttributeEnumValue(FrankEnumConstant c) {
		this.javaTag = c.getName();
		this.label = this.javaTag;
		FrankAnnotation annotation = c.getAnnotation(ENUM_LABEL);
		String annotationValue = null;
		if(annotation != null) {
			try {
				annotationValue = (String) annotation.getValue();
			} catch(FrankDocException e) {
				log.warn("Could not parse annotation value of {}", ENUM_LABEL, e);
			}			
		}
		if(! StringUtils.isBlank(annotationValue)) {
			this.explicitLabel = true;
			this.label = annotationValue;
		}
		String javaDoc = c.getJavaDoc();
		if(! StringUtils.isBlank(javaDoc)) {
			this.description = javaDoc;
		}
	}
}
