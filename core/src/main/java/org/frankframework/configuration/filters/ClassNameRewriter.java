/*
   Copyright 2023-2026 WeAreFrank!

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
package org.frankframework.configuration.filters;

import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
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
	private final boolean rewriteLegacyClassNames;
	private final ClassNameAliases aliases;

	public ClassNameRewriter(ContentHandler handler, ApplicationContext applicationContext) {
		this(handler, applicationContext, true, ClassNameAliases.getInstance());
	}

	/**
	 * @param rewriteLegacyClassNames whether to rewrite {@value #LEGACY_PACKAGE_NAME} class names and the old implicit class names
	 * @param aliases the class name aliases to apply, see {@link ClassNameAliases}
	 */
	public ClassNameRewriter(ContentHandler handler, ApplicationContext applicationContext, boolean rewriteLegacyClassNames, ClassNameAliases aliases) {
		super(handler);
		this.rewriteLegacyClassNames = rewriteLegacyClassNames;
		this.aliases = aliases;

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

		if (className != null) {
			String rewritten = className;
			if (rewriteLegacyClassNames && (className.startsWith(LEGACY_PACKAGE_NAME) || OLD_IMPLICIT_CLASSNAMES.contains(className))) {
				rewritten = rewriteClassName(className);
			}
			rewritten = applyAlias(rewritten);
			if (!rewritten.equals(className)) {
				writableAttributes.setValue(CLASS_NAME_ATTRIBUTE, rewritten);
			}
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

	private String applyAlias(final String className) {
		Optional<String> target = aliases.target(className);
		if (target.isEmpty()) {
			return className;
		}
		String newClassName = target.get();
		if (!canLoadClass(newClassName)) {
			addDeprecationWarning("Class name alias [%s] points at [%s], which cannot be loaded. Building the configuration with [%s].".formatted(className, newClassName, className));
			return className;
		}
		addDeprecationWarning("[%s] has been renamed to [%s]. Please change the className attribute.".formatted(className, newClassName));
		return newClassName;
	}

	private void addDeprecationWarning(String message) {
		if (configWarning == null) {
			log.warn(message);
		} else {
			configWarning.add(null, log, message, SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
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
