package nl.nn.adapterframework.doc.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.util.LogUtil;

public class FrankDocModel {
	private static final String JAVA_STRING = "java.lang.String";

	private static Logger log = LogUtil.getLogger(FrankDocModel.class);

	private @Getter List<FrankDocGroup> groups;
	private @Getter Map<String, FrankElement> allElements = new HashMap<>();

	public FrankDocModel() {
		groups = new ArrayList<>();
	}

	public FrankElement findOrCreateFrankElement(Class<?> clazz) {
		if(allElements.containsKey(clazz.getName())) {
			return allElements.get(clazz.getName());
		}
		Class<?> superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : findOrCreateFrankElement(superClass);
		FrankElement current = new FrankElement(clazz, parent);
		current.setAttributes(createAttributes(clazz.getDeclaredMethods(), current));
		allElements.put(current.getFullName(), current);
		return current;
	}

	List<FrankAttribute> createAttributes(Method[] methods, FrankElement attributeOwner) {
		Map<String, Method> setterAttributes = getAttributeToMethodMap(methods, "set");
		Map<String, Method> getterAttributes = getGetterAndIsserAttributes(methods, attributeOwner);
		Map<String, String> setterToAttributeName = new HashMap<>();
		for(String attributeName: setterAttributes.keySet()) {
			setterToAttributeName.put(setterAttributes.get(attributeName).getName(), attributeName);
		}
		List<FrankAttribute> result = new ArrayList<>();
		// We iterate over methods instead of setterAttributes. This way, we preserve method order within
		// the list of attributes. This makes sense when the order field of an attribute is not unique.
		for(Method method: methods) {
			if(setterToAttributeName.containsKey(method.getName())) {
				String attributeName = setterToAttributeName.get(method.getName());
				if(getterAttributes.containsKey(attributeName)) {
					checkForTypeConflict(method, getterAttributes.get(attributeName), attributeOwner);
				}
				FrankAttribute attribute = new FrankAttribute(attributeName, attributeOwner);
				documentAttribute(attribute, method, attributeOwner);
				result.add(attribute);
			}
		}
		return result;
	}

	private Map<String, Method> getGetterAndIsserAttributes(Method[] methods, FrankElement attributeOwner) {
		Map<String, Method> getterAttributes = getAttributeToMethodMap(methods, "get");
		Map<String, Method> isserAttributes = getAttributeToMethodMap(methods, "is");
		for(String isserAttributeName : isserAttributes.keySet()) {
			if(getterAttributes.containsKey(isserAttributeName)) {
				log.warn(String.format("For FrankElement [%s], attribute [%s] has both a getX and an isX method",
						attributeOwner.getSimpleName(), isserAttributeName));
			} else {
				getterAttributes.put(isserAttributeName, isserAttributes.get(isserAttributeName));
			}
		}
		return getterAttributes;
	}

	static Map<String, Method> getAttributeToMethodMap(Method[] methods, String prefix) {
		List<Method> methodList = Arrays.asList(methods);
		methodList = methodList.stream()
				.filter(FrankDocModel::isGetterOrSetter)
				.filter(m -> m.getName().startsWith(prefix) && (m.getName().length() > prefix.length()))
				.collect(Collectors.toList());		
		Map<String, Method> result = new HashMap<>();
		for(Method method: methodList) {
			String strippedName = method.getName().substring(prefix.length());
			String attributeName = strippedName.substring(0, 1).toLowerCase() + strippedName.substring(1);
			result.put(attributeName, method);
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
					(method.getReturnType().isPrimitive()
							&& !method.getReturnType().getName().equals("void"))
					|| method.getReturnType().getName().equals(JAVA_STRING)
				) && (method.getParameterTypes().length == 0);
		return isSetter || isGetter;
	}

	private void checkForTypeConflict(Method setter, Method getter, FrankElement attributeOwner) {
		String setterType = setter.getParameterTypes()[0].getName();
		String getterType = getter.getReturnType().getName();
		if(! getterType.equals(setterType)) {
			log.warn(String.format("In Frank element [%s]: setter [%s] has type [%s] while the getter has type [%s]",
					attributeOwner.getSimpleName(), setter.getName(), setterType, getterType));
		}
	}

	private void documentAttribute(FrankAttribute attribute, Method method, FrankElement attributeOwner) {
		attribute.setDeprecated(AnnotationUtils.findAnnotation(method, Deprecated.class) != null);
		IbisDocRef ibisDocRef = AnnotationUtils.findAnnotation(method, IbisDocRef.class);
		if(ibisDocRef != null) {
			ParsedIbisDocRef parsed = parseIbisDocRef(ibisDocRef, method);
			IbisDoc ibisDoc = AnnotationUtils.findAnnotation(parsed.getReferredMethod(), IbisDoc.class);
			if(ibisDoc != null) {
				attribute.setDescribingElement(findOrCreateFrankElement(parsed.getReferredMethod().getDeclaringClass()));
				attribute.parseIbisDocAnnotation(ibisDoc);
				if(parsed.hasOrder) {
					attribute.setOrder(parsed.getOrder());
				}
				return;
			}
		}
		IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
		if(ibisDoc != null) {
			attribute.parseIbisDocAnnotation(ibisDoc);
		}
		else {
			log.warn(String.format("No documentation available for FrankElement [%s], attribute [%s]",
					attributeOwner.getSimpleName(), attribute.getName()));
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
				log.warn(String.format("Could not parse order in @IbisDocRef annotation: [%s]", ibisDocRef.value()[0]));
			}
		}
		else {
			log.warn(String.format(String.format("Too many or zero parameters in @IbisDocRef annotation on method: [%s].[%s]",
					originalMethod.getDeclaringClass().getName(), originalMethod.getName())));
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
			log.warn("Super class [" + className + "] was not found!");
			return null;
		}
	}

	public FrankDocGroup addGroup(String name) {
		FrankDocGroup group = new FrankDocGroup(name);
		groups.add(group);
		return group;
	}
}
