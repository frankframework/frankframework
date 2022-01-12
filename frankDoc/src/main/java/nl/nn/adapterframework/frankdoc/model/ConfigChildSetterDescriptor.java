/* 
Copyright 2020, 2021 WeAreFrank! 

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

import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

/**
 * Class {@link nl.nn.adapterframework.frankdoc.model.ConfigChild} specifies what Frank elements
 * can be contained in another Frank element. The present class {@code ConfigChildSetterDescriptor}
 * contains some of the information that has to be put in {@link nl.nn.adapterframework.frankdoc.model.ConfigChild}.
 * The present helper class holds the information that comes from the file {@code digester-rules.xml}.
 * 
 * Why don't we create {@link nl.nn.adapterframework.frankdoc.model.ConfigChild} directly without
 * creating this helper class? As an example, consider a digester rule that links setter
 * {@code setAbc()} to a syntax 1 name {@code abc}.
 * This rule is represented by an instance of {@code ConfigChildSetterDescriptor}. If there are
 * two classes {@code X} and {@code Y} with method {@code setAbc()}, then two
 * different instances of {@link nl.nn.adapterframework.frankdoc.model.ConfigChild} are needed. The reason is that
 * {@code X.setAbc()} and {@code Y.setAbc()} can have a different {@code sequenceInConfig}.
 * That field is obtained from an {@code IbisDoc} annotation.
 */
abstract class ConfigChildSetterDescriptor {
	private @Getter String methodName;
	private @Getter boolean mandatory;
	private @Getter boolean allowMultiple;
	private @Getter String roleName;

	ConfigChildSetterDescriptor(String methodName, String roleName) throws SAXException {
		this.methodName = methodName;
		this.roleName = roleName;
		mandatory = false;
		if(methodName.startsWith("set")) {
			allowMultiple = false;
		} else if((methodName.startsWith("add")) || methodName.startsWith("register")) {
			allowMultiple = true;
		} else {
			throw new SAXException(
					String.format("Do not know how many elements go in method [%s]", methodName));
		}
	}

	@Override
	public String toString() {
		return String.format("%s(method = %s, roleName = %s, mandatory = %b, allowMultiple = %b)", getClass().getSimpleName(), methodName, roleName, mandatory, allowMultiple);
	}

	abstract ConfigChild createConfigChild(FrankElement parent, FrankMethod method);
	abstract boolean isForObject();

	static class ForObject extends ConfigChildSetterDescriptor {
		ForObject(String methodName, String roleName) throws SAXException {
			super(methodName, roleName);
		}

		@Override
		ConfigChild createConfigChild(FrankElement parent, FrankMethod method) {
			return new ObjectConfigChild(parent, method);
		}

		@Override
		boolean isForObject() {
			return true;
		}
	}

	static class ForText extends ConfigChildSetterDescriptor {
		ForText(String methodName, String roleName) throws SAXException {
			super(methodName, roleName);
		}

		@Override
		ConfigChild createConfigChild(FrankElement parent, FrankMethod method) {
			return new TextConfigChild(parent, method, getRoleName());
		}

		@Override
		boolean isForObject() {
			return false;
		}
	}
}
