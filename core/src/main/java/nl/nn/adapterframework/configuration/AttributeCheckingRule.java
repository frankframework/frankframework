/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.digester3.Rule;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Helper class to check that each attribute set from the configuration is available on the 
 * object being configured. 
 * 
 * @author  Gerrit van Brakel
 */
public class AttributeCheckingRule extends Rule {
	protected Logger log = LogUtil.getLogger(this);
	private boolean suppressDeprecationWarnings = AppConstants.getInstance().getBoolean(SuppressKeys.DEPRECATION_SUPPRESS_KEY.getValue(), false);

	/**
	 * Returns the name of the object. In case a Spring proxy is being used, 
	 * the name will be something like XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d
	 * ClassUtils.getUserClass() makes sure the original class will be returned.
	 */
	private String getObjectName(Object o) {
		String result = ClassUtils.getUserClass(o).getSimpleName();
		if (o instanceof INamedObject) {
			result+=" ["+((INamedObject)o).getName()+"]";
		}
		return result;
	}

	@Override
	public void begin(String uri, String elementName, Attributes attributes) throws Exception {
		Object top = getDigester().peek();

		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			if ("".equals(name)) {
				name = attributes.getQName(i);
			}
			if (name!=null && !name.equals("className")) {
				if (log.isTraceEnabled()) {
					log.trace(getObjectName(top)+" checking for setter for attribute ["+name+"]");
				}
				PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(top, name);
				Method m=null;
				if (pd!=null) {
					m = PropertyUtils.getWriteMethod(pd);
				}
				if (m==null) {
					Locator loc = getDigester().getDocumentLocator();
					String msg = "line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+": "+getObjectName(top)+" does not have an attribute ["+name+"] to set to value ["+attributes.getValue(name)+"]";
					ConfigurationWarnings.add(null, log, msg); //We need to use this as it's a configuration specific warning
				} else {
					ConfigurationWarning warning = AnnotationUtils.findAnnotation(m, ConfigurationWarning.class);
					if(warning != null) {
						Locator loc = getDigester().getDocumentLocator();
						String msg = "line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+": "+getObjectName(top)+" attribute ["+name+"]";
						boolean isDeprecated = AnnotationUtils.findAnnotation(m, Deprecated.class) != null;

						if(isDeprecated) {
							msg += " is deprecated";
						}

						if(StringUtils.isNotEmpty(warning.value())) {
							msg += ": " + warning.value();
						}

						if(!(suppressDeprecationWarnings && isDeprecated)) { //Don't log if deprecation warnings are suppressed and it is deprecated
							ConfigurationWarnings.add(null, log, msg); //We need to use this as it's a configuration specific warning
						}
					}
				}
			}
		}
	}
}
