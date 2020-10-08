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

public class FrankElement {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private static final String JAVA_STRING = "java.lang.String";

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private @Getter FrankElement parent;
	private @Getter List<FrankAttribute> attributes;

	public interface FrankElementStore {
		boolean hasFrankElement(String name);
		FrankElement getFrankElement(String name);
		void addFrankElement(FrankElement frankElement);
		int numFrankElements();
	}

	public static FrankElement frankElement(Class<?> clazz, FrankElementStore store) {
		if(store.hasFrankElement(clazz.getName())) {
			return store.getFrankElement(clazz.getName());
		}
		Class<?> superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : frankElement(superClass, store);
		FrankElement current = new FrankElement(clazz, parent, store);
		store.addFrankElement(current);
		return current;
	}

	FrankElement(Class<?> clazz, FrankElement parent, FrankElementStore store) {
		this(clazz.getName(), clazz.getSimpleName());
		this.parent = parent;
		this.attributes = createAttributes(clazz.getDeclaredMethods(), store);
	}

	FrankElement(final String fullName, final String simpleName) {
		this.fullName = fullName;
		this.simpleName = simpleName;
	}

	List<FrankAttribute> createAttributes(Method[] methods, FrankElementStore store) {
		Map<String, Method> setterAttributes = getAttributeToMethodMap(methods, "set");
		Map<String, Method> getterAttributes = getGetterAndIsserAttributes(methods);
		Map<String, String> setterToAttributeName = new HashMap<>();
		for(String attributeName: setterAttributes.keySet()) {
			setterToAttributeName.put(setterAttributes.get(attributeName).getName(), attributeName);
		}
		List<FrankAttribute> result = new ArrayList<>();
		for(Method method: methods) {
			if(setterToAttributeName.containsKey(method.getName())) {
				String attributeName = setterToAttributeName.get(method.getName());
				if(getterAttributes.containsKey(attributeName)) {
					compareGetterWithSetter(method, getterAttributes.get(attributeName));
				} else {
					log.warn(String.format("FrankElement %s and attribute %s: no getter method",
							simpleName, attributeName));
				}
				FrankAttribute attribute = new FrankAttribute(attributeName);
				attribute.setDescribingElement(this);
				documentAttribute(attribute, method, store);
				result.add(attribute);
			}
		}
		return result;
	}

	private Map<String, Method> getGetterAndIsserAttributes(Method[] methods) {
		Map<String, Method> getterAttributes = getAttributeToMethodMap(methods, "get");
		Map<String, Method> isserAttributes = getAttributeToMethodMap(methods, "is");
		for(String isserAttributeName : isserAttributes.keySet()) {
			if(getterAttributes.containsKey(isserAttributeName)) {
				log.warn(String.format("For FrankElement %s, attribute %s has both a getX and an isX method",
						simpleName, isserAttributeName));
			} else {
				getterAttributes.put(isserAttributeName, isserAttributes.get(isserAttributeName));
			}
		}
		return getterAttributes;
	}

	static Map<String, Method> getAttributeToMethodMap(Method[] methods, String prefix) {
		List<Method> methodList = Arrays.asList(methods);
		methodList = methodList.stream()
				.filter(FrankElement::isGetterOrSetter)
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
					method.getReturnType().isPrimitive()
					|| method.getReturnType().getName().equals(JAVA_STRING)
				) && (method.getParameterTypes().length == 0);
		return isSetter || isGetter;
	}

	private void compareGetterWithSetter(Method setter, Method getter) {
		String setterType = setter.getParameterTypes()[0].getName();
		String getterType = getter.getReturnType().getName();
		if(! getterType.equals(setterType)) {
			log.warn(String.format("In Frank element %s: setter %s has type %s while the getter has type %s",
					simpleName, setter.getName(), setterType, getterType));
		}
	}

	private void documentAttribute(
			FrankAttribute attribute,
			Method method,
			FrankElementStore store) {
		attribute.setDeprecated(AnnotationUtils.findAnnotation(method, Deprecated.class) != null);
		IbisDocRef ibisDocRef = AnnotationUtils.findAnnotation(method, IbisDocRef.class);
		if(ibisDocRef != null) {
			ParsedIbisDocRef parsed = parseIbisDocRef(ibisDocRef, method);
			IbisDoc ibisDoc = AnnotationUtils.findAnnotation(parsed.getReferredMethod(), IbisDoc.class);
			if(ibisDoc != null) {
				attribute.setDescribingElement(frankElement(
						parsed.getReferredMethod().getDeclaringClass(), store));
				ibisDocIntoFrankElement(ibisDoc, attribute);
				if(parsed.hasOrder) {
					attribute.setOrder(parsed.getOrder());
				}
				return;
			}
		}
		IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
		if(ibisDoc != null) {
			ibisDocIntoFrankElement(ibisDoc, attribute);
		}
		else {
			log.warn(String.format("No documentation available for FrankElement %s, attribute %s",
					simpleName, attribute.getName()));
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
				log.warn(String.format("Could not parse order in @IbisDocRef annotation: "
						+ Integer.parseInt(ibisDocRef.value()[0])));
			}
		}
		else {
			log.warn(String.format(String.format("Invalid @IbisDocRef annotation on method: %s.%s",
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
			log.warn(String.format("Super class [" + e + "] was not found!"));
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
			log.warn(String.format("Could not parse order in @IbisDoc annotation: " + ibisDocValues[0]));
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
