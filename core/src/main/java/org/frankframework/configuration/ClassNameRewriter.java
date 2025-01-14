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
package org.frankframework.configuration;

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

	public ClassNameRewriter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		WritableAttributes writableAttributes = new WritableAttributes(attributes);
		String className = writableAttributes.getValue(CLASS_NAME_ATTRIBUTE);
		if (className != null && (className.startsWith(LEGACY_PACKAGE_NAME))) {
			writableAttributes.setValue(CLASS_NAME_ATTRIBUTE, rewriteClassName(className));
		}
		super.startElement(uri, localName, qName, writableAttributes);
	}

	private static String rewriteClassName(final String originalClassName) {
		final String newClassName = originalClassName.replace(LEGACY_PACKAGE_NAME, ORG_FRANKFRAMEWORK_PACKAGE_NAME);
		if (canLoadClass(newClassName)) {
			log.debug("Replaced classname [{}] in configuration with classname [{}]", originalClassName, newClassName);
			return newClassName;
		}
		if (!canLoadClass(originalClassName)) {
			log.warn("Cannot load class [{}] from configuration. Please check if this was a deprecated class removed in this release, or if it's a custom class that needs to be reworked.", originalClassName);
		} else {
			log.debug("Cannot load a class named [{}], will build configuration with original classname [{}]", newClassName, originalClassName);
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
