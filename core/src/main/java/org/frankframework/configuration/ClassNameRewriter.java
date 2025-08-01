/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.configuration;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.xml.FullXmlFilter;
import org.frankframework.xml.WritableAttributes;

@Log4j2
public class ClassNameRewriter extends FullXmlFilter {

	public static final String LEGACY_PACKAGE_NAME = "nl.nn.adapterframework.";
	public static final String ORG_FRANKFRAMEWORK_PACKAGE_NAME = "org.frankframework.";
	public static final String CLASS_NAME_ATTRIBUTE = "className";

	private static final List<String> OLD_IMPLICIT_CLASSNAMES = List.of("org.frankframework.pipes.PutInSession", "org.frankframework.pipes.RemoveFromSession",
			"org.frankframework.pipes.GetFromSession", "org.frankframework.pipes.XmlWellFormedChecker", "org.frankframework.pipes.JsonWellFormedChecker",
			"org.frankframework.pipes.LargeBlockTester", "org.frankframework.extensions.sap.jco3.SapLUWManager", "org.frankframework.extensions.rekenbox.RekenBoxCaller");

	private final ConfigurationWarnings configWarning;

	public ClassNameRewriter(ContentHandler handler, ApplicationContext applicationContext) {
		super(handler);

		if (applicationContext != null && applicationContext.containsBean("configurationWarnings")) {
			configWarning = applicationContext.getBean("configurationWarnings", ConfigurationWarnings.class);
		} else {
			configWarning = null;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		WritableAttributes writableAttributes = new WritableAttributes(attributes);
		String className = writableAttributes.getValue(CLASS_NAME_ATTRIBUTE);

		if (className != null && (className.startsWith(LEGACY_PACKAGE_NAME) || OLD_IMPLICIT_CLASSNAMES.contains(className))) {
			writableAttributes.setValue(CLASS_NAME_ATTRIBUTE, rewriteClassName(className));
		}

		super.startElement(uri, localName, qName, writableAttributes);
	}

	private String rewriteClassName(final String originalClassName) {
		final String newClassName = addElementSuffix(rewritePackageName(originalClassName));

		if (canLoadClass(newClassName)) {
			return newClassName;
		}
		if (!canLoadClass(originalClassName)) {
			addDeprecationWarning("Cannot load class [%s] from configuration. Please check if this was a deprecated class removed in this release, or if it's a custom class that needs to be reworked.".formatted(originalClassName));
		} else {
			log.debug("Cannot load a class named [{}], will build configuration with original classname [{}]", newClassName, originalClassName);
		}
		return originalClassName;
	}

	private void addDeprecationWarning(String message) {
		if (configWarning == null) {
			log.warn(message);
		} else {
			configWarning.add((Object) null, log, message, SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
		}
	}

	private static String rewritePackageName(String originalClassName) {
		if (originalClassName.startsWith(LEGACY_PACKAGE_NAME)) {
			String newClassName = originalClassName.replace(LEGACY_PACKAGE_NAME, ORG_FRANKFRAMEWORK_PACKAGE_NAME);
			log.debug("Replaced classname [{}] in configuration with classname [{}]", originalClassName, newClassName);

			return newClassName;
		}

		return originalClassName;
	}

	private String addElementSuffix(String originalClassName) {
		if (OLD_IMPLICIT_CLASSNAMES.contains(originalClassName)) {
			String newClassName = "%sPipe".formatted(originalClassName);
			addDeprecationWarning("[%s] has been renamed to [%s]. Please use the new syntax or change the className attribute.".formatted(originalClassName, newClassName));

			return newClassName;
		}

		return originalClassName;
	}

	private static boolean canLoadClass(String className) {
		try {
			ClassNameRewriter.class.getClassLoader().loadClass(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
