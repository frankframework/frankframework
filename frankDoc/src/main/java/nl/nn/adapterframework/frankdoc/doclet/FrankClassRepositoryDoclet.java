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
			FrankClassDoclet frankClass = findOrCreateClass(classDoc);
			if(classDoc.isInterface()) {
				interfacesByName.put(frankClass.getName(), frankClass);
			}
		}
		final Set<String> correctedIncludeFilters = includeFilters.stream().map(FrankClassRepository::removeTrailingDot).collect(Collectors.toSet());
		List<FrankClassDoclet> filteredClassesForInterfaceImplementations = classesByName.values().stream()
				.filter(c -> correctedIncludeFilters.stream().anyMatch(i -> c.getPackageName().startsWith(i)))
				.filter(c -> ! excludeFilters.contains(c.getName()))
				.collect(Collectors.toList());
		for(FrankClassDoclet c: filteredClassesForInterfaceImplementations) {
			setInterfaceImplementations(c, interfacesByName);
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
		try {
			FrankClassDoclet implementation = (FrankClassDoclet) findClass(clazz.getName());
			for(String implementedInterfaceName: implementedInterfaceNames) {
				FrankClassDoclet interfaze = availableInterfacesByName.get(implementedInterfaceName);
				interfaze.recursivelyAddInterfaceImplementation(implementation);
				for(FrankClass parentInterfaze: interfaze.getInterfaces()) {
					((FrankClassDoclet) parentInterfaze).recursivelyAddInterfaceImplementation(implementation);
				}
			}
		} catch(FrankDocException e) {
			log.warn("Error setting implemented interfaces of class {}", clazz.getName(), e);
		}
	}

	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		return classesByName.get(fullName);
	}
}
