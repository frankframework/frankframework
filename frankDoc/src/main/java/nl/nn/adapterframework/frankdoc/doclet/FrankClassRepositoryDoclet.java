package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.sun.javadoc.ClassDoc;

import nl.nn.adapterframework.util.LogUtil;

class FrankClassRepositoryDoclet implements FrankClassRepository {
	private static Logger log = LogUtil.getLogger(FrankClassRepositoryDoclet.class);

	private Set<String> excludeFilters;
	private String[] includeFilter;

	private final Map<String, FrankClassDoclet> classesByName = new HashMap<>();

	FrankClassRepositoryDoclet(ClassDoc[] classDocs) {
		Map<String, FrankClassDoclet> interfacesByName = new HashMap<>();
		for(ClassDoc classDoc: classDocs) {
			FrankClassDoclet frankClass = findOrCreateClass(classDoc);
			if(classDoc.isInterface()) {
				interfacesByName.put(frankClass.getName(), frankClass);
			}
		}
		for(ClassDoc classDoc: classDocs) {
			setInterfaceImplementations(classDoc, interfacesByName);
		}
	}

	@Override
	public void setExcludeFilters(Set<String> excludeFilters) {
		this.excludeFilters = excludeFilters;
	}

	@Override
	public Set<String> getExcludeFilters() {
		return excludeFilters;
	}

	@Override
	public void setIncludeFilters(String ...items) {
		includeFilter = items;
	}

	@Override
	public String[] getIncludeFilter() {
		return includeFilter;
	}

	private FrankClassDoclet findOrCreateClass(ClassDoc classDoc) {
		if(classesByName.containsKey(classDoc.name())) {
			return classesByName.get(classDoc.name());
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

	private void setInterfaceImplementations(ClassDoc classDoc, Map<String, FrankClassDoclet> availableInterfacesByName) {
		Set<String> implementedInterfaceNames = Arrays.asList(classDoc.interfaces()).stream()
				.map(ClassDoc::qualifiedName)
				.collect(Collectors.toSet());
		implementedInterfaceNames.retainAll(availableInterfacesByName.keySet());
		try {
			FrankClassDoclet implementation = (FrankClassDoclet) findClass(classDoc.qualifiedName());
			for(String implementedInterfaceName: implementedInterfaceNames) {
				FrankClassDoclet interfaze = availableInterfacesByName.get(implementedInterfaceName);
				interfaze.recursivelyAddInterfaceImplementation(implementation);
				for(FrankClass parentInterfaze: interfaze.getInterfaces()) {
					((FrankClassDoclet) parentInterfaze).recursivelyAddInterfaceImplementation(implementation);
				}
			}
		} catch(FrankDocException e) {
			log.warn("Error setting implemented interfaces of class {}", classDoc.name(), e);
		}
	}

	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		return classesByName.get(fullName);
	}
}
