/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.HasApplicationContext;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

public class RootValidation {
	protected Logger log = LogUtil.getLogger(this);

	private final List<String> rootValidation;

	public RootValidation(String... rootElement) {
		rootValidation = Arrays.asList(rootElement);
	}

	public void check(HasApplicationContext source, Set<IXSD> xsds) {
		boolean found = false;
		String validElements = rootValidation.get(rootValidation.size() - 1);
		List<String> validElementsAsList = StringUtil.split(validElements);
		for (String validElement : validElementsAsList) {
			if (StringUtils.isNotEmpty(validElement)) {
				List<String> allRootTags = new ArrayList<>();
				for (IXSD xsd : xsds) {
					for (String rootTag : xsd.getRootTags()) {
						allRootTags.add(rootTag);
						if (validElement.equals(rootTag)) {
							found = true;
						}
					}
				}
				if (!found) {
					ConfigurationWarnings.add(source, log, "Element ["+validElement+"] not in list of available root elements "+allRootTags);
				}
			}
		}
	}

	public List<String> getPath() {
		return rootValidation;
	}

	public int getPathLength() {
		return rootValidation.size();
	}

	/**
	 * Comma separated list of elements valid at level in path.
	 */
	public String getValidElementsAtLevel(int level) {
		return rootValidation.get(level);
	}

	public String getValidLastElements() {
		return rootValidation.get(getPathLength()-1);
	}

	public boolean isNamespaceAllowedOnElement(Map<List<String>, List<String>> invalidRootNamespaces, String namespaceURI, String localName) {
		List<String> invalidNamespaces = invalidRootNamespaces.get(getPath());
		return invalidNamespaces == null || !invalidNamespaces.contains(namespaceURI);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + rootValidation;
	}
}
