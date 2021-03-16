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

package nl.nn.adapterframework.doc.doclet;

import java.util.List;

public interface FrankClass extends FrankType {
	@Override
	default boolean isPrimitive() {
		return false;
	}

	@Override
	default boolean isAnnotation() {
		return false;
	}

	String getSimpleName();
	FrankClass getSuperclass();
	FrankClass[] getInterfaces();
	boolean isAbstract();
	boolean isInterface();
	boolean isPublic();

	/**
	 * Assumes that this object models a Java interface and get the non-abstract interface implementations.
	 */
	List<FrankClass> getInterfaceImplementations() throws DocletReflectiveOperationException;

	FrankMethod[] getDeclaredMethods();
	FrankMethod[] getDeclaredAndInheritedMethods();

	String[] getEnumConstants();
}
