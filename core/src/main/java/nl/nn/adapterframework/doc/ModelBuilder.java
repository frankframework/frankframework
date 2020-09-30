package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilder {
	private @Getter FrankDocModel model;

	static class AttributeSeed {
		private @Getter String name;

		AttributeSeed(Method reflectMethod) {
			name = reflectMethod.getName();
		}

		AttributeSeed(String name) {
			this.name = name;
		}
	}

	static class ElementSeed {
		private @Getter @Setter Map<String, AttributeSeed> methods;
		private @Getter @Setter Map<String, AttributeSeed> methodsWithInherited;
		private @Getter String fullName;
		private @Getter @Setter String simpleName;

		ElementSeed(Class<?> clazz) {
			methods = new HashMap<>();
			for(Method reflect: clazz.getDeclaredMethods()) {
				// Jacoco is a tool for code coverage. To have predictible results,
				// we omit methods introduced by Jacoco.
				if(reflect.getName().contains("jacoco")) {
					continue;
				}
				methods.put(reflect.getName(), new AttributeSeed(reflect));
			}
			methodsWithInherited = new HashMap<>();
			methodsWithInherited.putAll(methods);
			for(Method reflect: clazz.getMethods()) {
				methodsWithInherited.putIfAbsent(reflect.getName(), new AttributeSeed(reflect));
			}
			fullName = clazz.getName();
			simpleName = clazz.getSimpleName();
		}

		ElementSeed(final String fullName) {
			this.fullName = fullName;
		}
	}

	public ModelBuilder() {
		model = new FrankDocModel();
		model.setGroups(new ArrayList<>());
		model.setAllElements(new HashMap<>());
	}

	/**
	 * @param interfaceName The interface for which we want SpringBean objects.
	 * @return All classes implementing interfaceName, ordered by their full class name.
	 */
	static List<SpringBean> getSpringBeans(final String interfaceName) {
		Class<?> interfaze = InfoBuilderSource.getClass(interfaceName);
		if(interfaze == null) {
			throw new NullPointerException();
		}
		if(!interfaze.isInterface()) {
			throw new IllegalArgumentException("Only retrieve Spring beans from an interface");
		}
		Set<SpringBean> unfiltered = InfoBuilderSource.getSpringBeans(interfaze);
		List<SpringBean> result = new ArrayList<SpringBean>();
		for(SpringBean b: unfiltered) {
			if(interfaze.isAssignableFrom(b.getClazz())) {
				result.add(b);
			}
		}
		return result;
	}

	static List<ElementSeed> getSelfAndAncestorSeeds(Class<?> clazz) {
		List<ElementSeed> result = new ArrayList<>();
		result.add(new ElementSeed(clazz));
		Class<?> superClass = clazz.getSuperclass();
		while(superClass != null) {
			result.add(new ElementSeed(superClass));
			superClass = superClass.getSuperclass();
		}
		return result;
	}

	FrankDocGroup addGroup(String name) {
		FrankDocGroup group = new FrankDocGroup(name);
		group.setElements(new HashMap<>());
		model.getGroups().add(group);
		return group;
	}

	void addElementsToGroup(List<ElementSeed> elementHierarchy, FrankDocGroup group) {
		List<ElementSeed> reversedSeeds = new ArrayList<>(elementHierarchy);
		Collections.reverse(reversedSeeds);
		FrankElement parent = null;
		for(ElementSeed seed: reversedSeeds) {
			if(model.getAllElements().containsKey(seed.getFullName())) {
				parent = model.getAllElements().get(seed.getFullName());
			}
			else {
				parent = createFrankElement(seed, parent);
				model.getAllElements().put(parent.getFullName(), parent);
			}
		}
		group.getElements().putIfAbsent(parent.getFullName(), parent);
	}

	FrankElement createFrankElement(ElementSeed seed, FrankElement parent) {
		FrankElement result = new FrankElement(seed.getFullName(), seed.getSimpleName());
		result.setParent(parent);
		result.setAttributes(createAttributes(seed));
		return result;
	}

	List<FrankAttribute> createAttributes(ElementSeed elementSeed) {
		Set<String> setters = selectNamesByPrefix(elementSeed.getMethods().keySet(), "set");
		Map<String, String> setterAttributes = getAttributeToMethodNameMap(setters, "set");
		Set<String> getters = selectNamesByPrefix(elementSeed.getMethodsWithInherited().keySet(), "get");
		Map<String, String> getterAttributes = getAttributeToMethodNameMap(getters, "get");
		Map<String, String> attributes = new HashMap<>();
		for(String attributeName: setterAttributes.keySet()) {
			if(getterAttributes.containsKey(attributeName)) {
				attributes.put(attributeName, setterAttributes.get(attributeName));
			}
		}
		List<FrankAttribute> result = new ArrayList<>();
		for(String attributeName: attributes.keySet()) {
			FrankAttribute attribute = new FrankAttribute(attributeName);
			result.add(attribute);
		}
		return result;
	}

	Set<String> selectNamesByPrefix(final Set<String> source, final String prefix) {
		return source.stream()
				.filter(name -> name.startsWith(prefix) && (name.length() >= prefix.length()))
				.collect(Collectors.toSet());		
	}

	Map<String, String> getAttributeToMethodNameMap(Set<String> methodNames, String prefix) {
		Map<String, String> result = new HashMap<>();
		for(String methodName: methodNames) {
			if(!methodName.startsWith(prefix)) {
				throw new IllegalArgumentException("Only apply this method with matching prefix: " + prefix);
			}
			String strippedName = methodName.substring(prefix.length());
			String attributeName = strippedName.substring(0, 1).toUpperCase() + strippedName.substring(1);
			if(result.containsKey(attributeName)) {
				throw new IllegalStateException("Duplicate method for attribute name: " + attributeName);
			}
			result.put(attributeName, methodName);
		}
		return result;
	}
}
