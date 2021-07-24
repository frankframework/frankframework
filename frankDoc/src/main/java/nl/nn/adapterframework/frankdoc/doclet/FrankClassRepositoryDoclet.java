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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.ClassDoc;

import lombok.AccessLevel;
import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;

class FrankClassRepositoryDoclet implements FrankClassRepository {
	private static Logger log = LogUtil.getLogger(FrankClassRepositoryDoclet.class);

	private @Getter(AccessLevel.PACKAGE) Set<String> excludeFiltersForSuperclass;

	private final Map<String, FrankClassDoclet> classesByName = new HashMap<>();
	private final Set<FrankClassDoclet> filteredClassesForInterfaceImplementations;

	FrankClassRepositoryDoclet(ClassDoc[] classDocs, Set<String> includeFilters, Set<String> excludeFilters, Set<String> excludeFiltersForSuperclass) {
		this.excludeFiltersForSuperclass = new HashSet<>(excludeFiltersForSuperclass);
		for(ClassDoc classDoc: classDocs) {
			findOrCreateClass(classDoc);
		}
		final Set<String> correctedIncludeFilters = includeFilters.stream().map(FrankClassRepository::removeTrailingDot).collect(Collectors.toSet());
		filteredClassesForInterfaceImplementations = classesByName.values().stream()
				.filter(c -> correctedIncludeFilters.stream().anyMatch(i -> c.getPackageName().startsWith(i)))
				.filter(c -> ! excludeFilters.contains(c.getName()))
				// Filter is there to get the same as with reflection.
				.filter(c -> c.isTopLevel())
				.collect(Collectors.toSet());
		for(FrankClassDoclet c: filteredClassesForInterfaceImplementations) {
			log.trace("Examining what interfaces are implemented by class [{}]", () -> c.getName());
			setInterfaceImplementations(c);
			log.trace("Done examining what interfaces are implemented by class [{}]", () -> c.getName());
		}
	}

	private FrankClassDoclet findOrCreateClass(ClassDoc classDoc) {
		if(classesByName.containsKey(classDoc.qualifiedName())) {
			return classesByName.get(classDoc.qualifiedName());
		}
		FrankClassDoclet result = new FrankClassDoclet(classDoc, this);
		classesByName.put(result.getName(), result);
		ClassDoc superClassDoc = classDoc.superclass();
		if(superClassDoc != null) {
			FrankClassDoclet superClazz = findOrCreateClass(superClassDoc);
			superClazz.addChild(result.getName());
		}
		return result;
	}

	private void setInterfaceImplementations(FrankClassDoclet clazz) {
		List<FrankClassDoclet> implementedInterfaces = clazz.getInterfacesAsList().stream()
				.distinct()
				.map(c -> (FrankClassDoclet) c)
				.collect(Collectors.toList());
		log.trace("Directly implemented interfaces: [{}]", () -> implementedInterfaces.stream().map(FrankClass::getSimpleName).collect(Collectors.joining(", ")));
		try {
			for(FrankClassDoclet interfaze: implementedInterfaces) {
				interfaze.recursivelyAddInterfaceImplementation(clazz);
				new TransitiveImplementedInterfaceBrowser<FrankClassDoclet>(interfaze).search(i -> loggedAddInterfaceImplementation(i, clazz));
			}
		} catch(FrankDocException e) {
			log.warn("Error setting implemented interfaces of class {}", clazz.getName(), e);
		}
	}

	private FrankClassDoclet loggedAddInterfaceImplementation(FrankClass interfaze, FrankClassDoclet clazz) {
		log.trace("Considering ancestor interface {}", () -> interfaze.getName());
		try {
			((FrankClassDoclet) interfaze).recursivelyAddInterfaceImplementation(clazz);
		} catch(FrankDocException e) {
			log.warn("Could not recurse over chidren of {} to set them as implementations of {}", clazz.getName(), interfaze.getName(), e);
		}
		return null;
	}

	boolean classIsAllowedAsInterfaceImplementation(FrankClassDoclet clazz) {
		return filteredClassesForInterfaceImplementations.contains(clazz);
	}

	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		return classesByName.get(fullName);
	}
}
