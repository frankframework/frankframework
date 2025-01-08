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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import org.frankframework.statistics.HasApplicationContext;
import org.frankframework.util.LogUtil;

public class RootValidations implements Iterable<RootValidation> {
	protected Logger log = LogUtil.getLogger(this);

	private final Set<RootValidation> rootValidations;

	public RootValidations() {
		this.rootValidations = new LinkedHashSet<>();
	}

	public RootValidations(RootValidation rootValidation) {
		this();
		this.rootValidations.add(rootValidation);
	}

	public RootValidations(String... rootElement) {
		this(new RootValidation(rootElement));
	}

	public void check(HasApplicationContext source, Set<IXSD> xsds) {
		for (RootValidation path: rootValidations) {
			path.check(source, xsds);
		}
	}

	public void add(RootValidation rootValidation) {
		this.rootValidations.add(rootValidation);
	}
	public void add(String rootElement) {
		add(new RootValidation(rootElement));
	}

	@Override
	public Iterator<RootValidation> iterator() {
		return rootValidations.iterator();
	}

	public boolean contains(RootValidation rootValidation) {
		return rootValidations.contains(rootValidation);
	}
}
