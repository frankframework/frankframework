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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

class TransitiveImplementedInterfaceBrowser<T> {
	final Deque<FrankClass> interfazes = new ArrayDeque<>();
	final Set<String> interfaceNamesDone = new HashSet<>();

	TransitiveImplementedInterfaceBrowser(FrankClass clazz) throws FrankDocException {
		uniquelyEnqueueSuperInterfaces(clazz);
	}

	private void uniquelyEnqueueSuperInterfaces(FrankClass clazz) {
		Arrays.asList(clazz.getInterfaces()).forEach(this::enqueueUniquely);
	}

	private void enqueueUniquely(FrankClass clazz) {
		if(! interfaceNamesDone.contains(clazz.getName())) {
			interfaceNamesDone.add(clazz.getName());
			interfazes.addLast(clazz);
		}
	}

	T search(Function<FrankClass, T> testFunction) throws FrankDocException {
		while(! interfazes.isEmpty()) {
			FrankClass current = interfazes.removeFirst();
			T result = testFunction.apply(current);
			if(result != null) {
				return result;
			}
			uniquelyEnqueueSuperInterfaces(current);
		}
		return null;
	}
}
