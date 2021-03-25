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

class FrankClassRepositoryReflect implements FrankClassRepository {
	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		try {
			return new FrankClassReflect(Class.forName(fullName));
		} catch(ClassNotFoundException e) {
			throw new FrankDocException(String.format("Could not find class [%s]", fullName), e);
		}
	}
}
