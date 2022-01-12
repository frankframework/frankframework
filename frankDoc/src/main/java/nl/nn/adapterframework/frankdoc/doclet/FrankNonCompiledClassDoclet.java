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

package nl.nn.adapterframework.frankdoc.doclet;

/**
 * Models classes like java.lang.Object, java.lang.String and org.apache.xerces.xs.XSTypeDefinition.
 * Such classes are referenced as arguments or return types in MethodDoc objects, but
 * no detailed information about them is needed. 
 * @author martijn
 *
 */
class FrankNonCompiledClassDoclet extends FrankSimpleType {
	FrankNonCompiledClassDoclet(String name) {
		super(name);
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}
}
