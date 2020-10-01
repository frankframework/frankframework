package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilder {
	private @Getter FrankDocModel model;

	@EqualsAndHashCode
	static class Type {
		private @Getter @Setter boolean isPrimitive;
		private @Getter @Setter String name;

		Type(Class<?> clazz) {
			isPrimitive = clazz.isPrimitive();
			name = clazz.getName();
		}

		Type() {		
		}

		Type(Parameter param) {
			this(param.getType());
		}

		static Type typeVoid() {
			Type result = new Type();
			result.setPrimitive(true);
			result.setName("void");
			return result;
		}

		static Type typeString() {
			Type result = new Type();
			result.setPrimitive(false);
			result.setName("java.lang.String");
			return result;
		}

		static Type typeBoolean() {
			Type result = new Type();
			result.setPrimitive(true);
			result.setName("boolean");
			return result;
		}
	}

	static class AttributeSeed {
		private @Getter String name;
		private @Getter @Setter List<Type> argumentTypes;
		private @Getter @Setter Type returnType;

		AttributeSeed(Method reflectMethod) {
			name = reflectMethod.getName();
			returnType = new Type(reflectMethod.getReturnType());
			argumentTypes = new ArrayList<>();
			for(Parameter p: reflectMethod.getParameters()) {
				argumentTypes.add(new Type(p));
			}
		}

		AttributeSeed(String name) {
			this.name = name;
		}
	}

	static class ElementSeed {
		private @Getter @Setter Map<String, AttributeSeed> methods;
		private @Getter String fullName;
		private @Getter @Setter String simpleName;
		private @Getter @Setter FrankElement existingParent;

		ElementSeed(Class<?> clazz) {
			methods = new HashMap<>();
			for(Method reflect: clazz.getDeclaredMethods()) {
				// Jacoco is a tool for code coverage. To have predictable results,
				// we omit methods introduced by Jacoco.
				if(reflect.getName().contains("jacoco")) {
					continue;
				}
				methods.put(reflect.getName(), new AttributeSeed(reflect));
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

	static List<ElementSeed> getSelfAndAncestorSeeds(Class<?> clazz, Map<String, FrankElement> repository) {
		List<ElementSeed> result = new ArrayList<>();
		Class<?> superClass = clazz;
		ElementSeed newSeed = null;
		while((superClass != null) && (!repository.containsKey(superClass.getName()))) {
			newSeed = new ElementSeed(superClass);
			result.add(newSeed);
			superClass = superClass.getSuperclass();
		}
		if((newSeed != null) && (superClass != null)) {
			newSeed.setExistingParent(repository.get(superClass.getName()));
		}
		return result;
	}

	FrankDocGroup addGroup(String name) {
		FrankDocGroup group = new FrankDocGroup(name);
		group.setElements(new HashMap<>());
		model.getGroups().add(group);
		return group;
	}

	void addElementsToGroup(String elementName, List<ElementSeed> elementHierarchy, FrankDocGroup group) {	
		List<ElementSeed> reversedSeeds = new ArrayList<>(elementHierarchy);
		Collections.reverse(reversedSeeds);
		if(reversedSeeds.isEmpty()) {
			group.getElements().putIfAbsent(elementName, model.getAllElements().get(elementName));
		} else {
			FrankElement parent = reversedSeeds.get(0).getExistingParent();
			for(ElementSeed seed: reversedSeeds) {
				parent = createFrankElement(seed, parent);
				model.getAllElements().put(parent.getFullName(), parent);
			}
			group.getElements().putIfAbsent(parent.getFullName(), parent);
		}
	}

	FrankElement createFrankElement(ElementSeed seed, FrankElement parent) {
		FrankElement result = new FrankElement(seed.getFullName(), seed.getSimpleName());
		result.setParent(parent);
		result.setAttributes(createAttributes(seed));
		return result;
	}

	static List<FrankAttribute> createAttributes(ElementSeed elementSeed) {
		Map<String, String> setterAttributes = getAttributeToMethodNameMap(elementSeed.getMethods(), "set");
		Map<String, String> getterAttributes = getAttributeToMethodNameMap(elementSeed.getMethods(), "get");
		getterAttributes.putAll(getAttributeToMethodNameMap(elementSeed.getMethods(), "is"));
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

	static Map<String, String> getAttributeToMethodNameMap(Map<String, AttributeSeed> methods, String prefix) {
		Set<String> methodNames = methods.values().stream()
				.filter(ModelBuilder::isGetterOrSetter)
				.map(method -> method.getName())
				.filter(name -> name.startsWith(prefix) && (name.length() > prefix.length()))
				.collect(Collectors.toSet());		
		Map<String, String> result = new HashMap<>();
		for(String methodName: methodNames) {
			String strippedName = methodName.substring(prefix.length());
			String attributeName = strippedName.substring(0, 1).toLowerCase() + strippedName.substring(1);
			result.put(attributeName, methodName);
		}
		return result;
	}

	static boolean isGetterOrSetter(AttributeSeed attributeSeed) {
		boolean isSetter = attributeSeed.getReturnType().equals(Type.typeVoid())
				&& (attributeSeed.getArgumentTypes().size() == 1)
				&& isPrimitiveOrString(attributeSeed.getArgumentTypes().get(0));
		boolean isGetter = isPrimitiveOrString(attributeSeed.getReturnType())
				&& (attributeSeed.getArgumentTypes().size() == 0);
		return isSetter || isGetter;
	}

	private static boolean isPrimitiveOrString(Type t) {
		boolean isString = t.getName().equals("java.lang.String");
		boolean isVoid = t.getName().equals("void");
		return (!isVoid) && (isString || t.isPrimitive);
	}
}
