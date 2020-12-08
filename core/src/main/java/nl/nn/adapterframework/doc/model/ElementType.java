/* 
Copyright 2020 WeAreFrank! 

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

package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import lombok.Getter;

/**
 * Represents a type of FrankElement instances, which appears in the FF! Java code as
 * a Java interface. FrankElement objects that represent an abstract Java class should
 * be omitted as members. This is done automatically when Spring is used to get the
 * implementing classes of a Java interface.
 *
 * @author martijn
 *
 */
public class ElementType {
	private @Getter String fullName;
	private @Getter String simpleName;
	private @Getter Map<String, FrankElement> members;
	private @Getter boolean fromJavaInterface;
	
	/**
	 * For each syntax 1 name in this set, an &lt;xs:choice&gt; of &lt;xs:element&gt;
	 * is created in the XSD. The elements in a group correspond to the Java
	 * classes that implement the Java interface represented by this {@link ElementChild}.
	 * For each syntax 1 name a different group is needed because the syntax 2
	 * names of the elements are different.
	 * <p>
	 * A {@link LinkedHashSet} is being used to preserve the sequence with which
	 * the syntax 1 names were inserted.
	 */
	private @Getter LinkedHashSet<String> configChildSyntax1Names = new LinkedHashSet<>();

	ElementType(Class<?> clazz) {
		fullName = clazz.getName();
		simpleName = clazz.getSimpleName();
		members = new HashMap<>();
		this.fromJavaInterface = clazz.isInterface();
	}

	void addMember(FrankElement member) {
		members.put(member.getFullName(), member);
	}

	FrankElement getSingletonElement() throws ReflectiveOperationException {
		if(members.size() != 1) {
			throw new ReflectiveOperationException(String.format("Expected that ElementType [%s] contains exactly one element", fullName));
		}
		return members.values().iterator().next();
	}

	void addConfigChildSyntax1Name(String syntax1Name) {
		configChildSyntax1Names.add(syntax1Name);
	}
}
