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

	FrankClassRepositoryDoclet(ClassDoc[] classDocs, Set<String> includeFilters, Set<String> excludeFilters, Set<String> excludeFiltersForSuperclass) {
		this.excludeFiltersForSuperclass = new HashSet<>(excludeFiltersForSuperclass);
		Map<String, FrankClassDoclet> interfacesByName = new HashMap<>();
		for(ClassDoc classDoc: classDocs) {
			findOrCreateFrankClassAndUpdateInterfaces(classDoc, interfacesByName);
			for(ClassDoc innerClassDoc: classDoc.innerClasses()) {
				findOrCreateFrankClassAndUpdateInterfaces(innerClassDoc, interfacesByName);
			}
		}
		final Set<String> correctedIncludeFilters = includeFilters.stream().map(FrankClassRepository::removeTrailingDot).collect(Collectors.toSet());
		List<FrankClassDoclet> filteredClassesForInterfaceImplementations = classesByName.values().stream()
				.filter(c -> correctedIncludeFilters.stream().anyMatch(i -> c.getPackageName().startsWith(i)))
				.filter(c -> ! excludeFilters.contains(c.getName()))
				.collect(Collectors.toList());
		for(FrankClassDoclet c: filteredClassesForInterfaceImplementations) {
			log.trace("Examining what interfaces are implemented by class [{}]", () -> c.getName());
			setInterfaceImplementations(c, interfacesByName);
			log.trace("Done examining what interfaces are implemented by class [{}]", () -> c.getName());
		}
	}

	private void findOrCreateFrankClassAndUpdateInterfaces(ClassDoc classDoc, Map<String, FrankClassDoclet> interfacesByName) {
		FrankClassDoclet frankClass = findOrCreateClass(classDoc);
		if(classDoc.isInterface()) {
			interfacesByName.put(frankClass.getName(), frankClass);
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

	private void setInterfaceImplementations(FrankClassDoclet clazz, Map<String, FrankClassDoclet> availableInterfacesByName) {
		Set<String> implementedInterfaceNames = clazz.getInterfacesRaw().stream()
				.map(FrankClass::getName)
				.collect(Collectors.toSet());
		implementedInterfaceNames.retainAll(availableInterfacesByName.keySet());
		log.trace("Directly implemented interfaces: [{}]", () -> implementedInterfaceNames.stream().collect(Collectors.joining(", ")));
		try {
			for(String implementedInterfaceName: implementedInterfaceNames) {
				FrankClassDoclet interfaze = availableInterfacesByName.get(implementedInterfaceName);
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

	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		return classesByName.get(fullName);
	}
}
