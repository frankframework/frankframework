package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.objects.SpringBean;
import nl.nn.adapterframework.util.LogUtil;

public class ModelBuilder {
	private static final String JAVA_STRING = "java.lang.String";

	private static Logger log = LogUtil.getLogger(ModelBuilder.class);

	private @Getter FrankDocModel model;

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
			throw new NullPointerException("Class or interface is not available on the classpath: " + interfaceName);
		}
		if(!interfaze.isInterface()) {
			throw new IllegalArgumentException("This exists on the classpath but is not an interface: " + interfaceName);
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

	FrankDocGroup addGroup(String name) {
		FrankDocGroup group = new FrankDocGroup(name);
		group.setElements(new HashMap<>());
		model.getGroups().add(group);
		return group;
	}

	void addElementsToGroup(Class<?> clazz, FrankDocGroup group) {
		group.getElements().put(clazz.getName(), frankElement(clazz));
	}

	FrankElement frankElement(Class<?> clazz) {
		if(model.getAllElements().containsKey(clazz.getName())) {
			return model.getAllElements().get(clazz.getName());
		}
		Class<?> superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : frankElement(superClass);
		FrankElement current = createFrankElement(clazz, parent);
		model.getAllElements().put(current.getFullName(), current);
		return current;
	}

	FrankElement createFrankElement(Class<?> clazz, FrankElement parent) {
		FrankElement result = new FrankElement(clazz.getName(), clazz.getSimpleName());
		result.setParent(parent);
		result.setAttributes(createAttributes(result, clazz.getDeclaredMethods()));
		return result;
	}

	List<FrankAttribute> createAttributes(FrankElement frankElement, Method[] methods) {
		Map<String, String> setterAttributes = getAttributeToMethodNameMap(methods, "set");
		Map<String, String> getterAttributes = getAttributeToMethodNameMap(methods, "get");
		getterAttributes.putAll(getAttributeToMethodNameMap(methods, "is"));
		Map<String, String> setterToAttributeName = new HashMap<>();
		for(String candidateAttributeName: setterAttributes.keySet()) {
			setterToAttributeName.put(setterAttributes.get(candidateAttributeName), candidateAttributeName);
		}
		List<FrankAttribute> result = new ArrayList<>();
		for(Method method: methods) {
			if(setterToAttributeName.containsKey(method.getName())) {
				String candidateAttributeName = setterToAttributeName.get(method.getName());
				FrankAttribute candidate = new FrankAttribute(candidateAttributeName);
				candidate.setDescribingElement(frankElement);
				boolean isDocumented = documentAttribute(candidate, method);
				if(getterAttributes.containsKey(candidateAttributeName) || isDocumented) {
					result.add(candidate);
				}
			}
		}
		return result;
	}

	static Map<String, String> getAttributeToMethodNameMap(Method[] methods, String prefix) {
		List<Method> methodList = Arrays.asList(methods);
		Set<String> methodNames = methodList.stream()
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

	static boolean isGetterOrSetter(Method method) {
		boolean isSetter = method.getReturnType().isPrimitive()
				&& method.getReturnType().getName().equals("void")
				&& (method.getParameterTypes().length == 1)
				&& (method.getParameterTypes()[0].isPrimitive()
						|| method.getParameterTypes()[0].getName().equals(JAVA_STRING));
		boolean isGetter = (
					method.getReturnType().isPrimitive()
					|| method.getReturnType().getName().equals(JAVA_STRING)
				) && (method.getParameterTypes().length == 0);
		return isSetter || isGetter;
	}

	private boolean documentAttribute(FrankAttribute attribute, Method method) {
		attribute.setDeprecated(AnnotationUtils.findAnnotation(method, Deprecated.class) != null);
		IbisDocRef ibisDocRef = AnnotationUtils.findAnnotation(method, IbisDocRef.class);
		if(ibisDocRef != null) {
			ParsedIbisDocRef parsed = parseIbisDocRef(ibisDocRef, method);
			IbisDoc ibisDoc = AnnotationUtils.findAnnotation(parsed.getReferredMethod(), IbisDoc.class);
			if(ibisDoc != null) {
				attribute.setDescribingElement(frankElement(parsed.getReferredMethod().getDeclaringClass()));
				ibisDocIntoFrankElement(ibisDoc, attribute);
				if(parsed.hasOrder) {
					attribute.setOrder(parsed.getOrder());
				}
				return true;
			}
		}
		IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
		if(ibisDoc != null) {
			ibisDocIntoFrankElement(ibisDoc, attribute);
			return true;
		}
		else {
			return false;
		}
	}

	private class ParsedIbisDocRef {
		private @Getter @Setter boolean hasOrder;
		private @Getter @Setter int order;
		private @Getter @Setter Method referredMethod;
	}

	private ParsedIbisDocRef parseIbisDocRef(IbisDocRef ibisDocRef, Method originalMethod) {
		ParsedIbisDocRef result = new ParsedIbisDocRef();
		result.setHasOrder(false);
		String[] values = ibisDocRef.value();
		String methodString = null;
		if (values.length == 1) {
			methodString = ibisDocRef.value()[0];
		} else if (values.length == 2) {
			methodString = ibisDocRef.value()[1];
			try {
				result.setOrder(Integer.parseInt(ibisDocRef.value()[0]));
				result.setHasOrder(true);
			} catch (Throwable t) {
				log.warn("Could not parse order in @IbisDocRef annotation: "
						+ Integer.parseInt(ibisDocRef.value()[0]));
			}
		}
		else {
			log.warn(String.format("Invalid @IbisDocRef annotation on method: %s.%s",
					originalMethod.getDeclaringClass().getName(), originalMethod.getName()));
			return null;
		}
		result.setReferredMethod(getReferredMethod(methodString, originalMethod));
		return result;
	}

	private static Method getReferredMethod(String methodString, Method originalMethod) {
		String lastNameComponent = methodString.substring(methodString.lastIndexOf(".") + 1).trim();
		char firstLetter = lastNameComponent.toCharArray()[0];
		String fullClassName = methodString;
		String methodName = lastNameComponent;
		if (Character.isLowerCase(firstLetter)) {
			int index = methodString.lastIndexOf(".");
			fullClassName = methodString.substring(0, index);
		} else {
			methodName = originalMethod.getName();
		}
		return getParentMethod(fullClassName, methodName);
	}

	private static Method getParentMethod(String className, String methodName) {
		try {
			Class<?> parentClass = Class.forName(className);
			for (Method parentMethod : parentClass.getMethods()) {
				if (parentMethod.getName().equals(methodName)) {
					return parentMethod;
				}
			}
			return null;
		} catch (ClassNotFoundException e) {
			log.warn("Super class [" + e + "] was not found!");
			return null;
		}
	}

	private void ibisDocIntoFrankElement(IbisDoc ibisDoc, FrankAttribute attribute) {
		String[] ibisDocValues = ibisDoc.value();
		boolean isIbisDocHasOrder = false;
		attribute.setOrder(Integer.MAX_VALUE);
		try {
			int order = Integer.parseInt(ibisDocValues[0]);
			attribute.setOrder(order);
			isIbisDocHasOrder = true;
		} catch (NumberFormatException e) {
			log.warn("Could not parse order in @IbisDoc annotation: " + ibisDocValues[0]);
		}
		if (isIbisDocHasOrder) {
			attribute.setDescription(ibisDocValues[1]);
			if (ibisDocValues.length > 2) {
				attribute.setDefaultValue(ibisDocValues[2]); 
			}
		} else {
			attribute.setDescription(ibisDocValues[0]);
			if (ibisDocValues.length > 1) {
				attribute.setDefaultValue(ibisDocValues[1]);
			}
		}
	}
}
