package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.Getter;
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
		if(model.getAllElements().containsKey(clazz.getName())) {
			group.getElements().put(clazz.getName(), model.getAllElements().get(clazz.getName()));
			return;			
		}
		List<Class<?>> classesForNewElements = new ArrayList<>();
		FrankElement parent = null;
		Class<?> superClass = clazz;
		while(true) {
			classesForNewElements.add(superClass);
			superClass = superClass.getSuperclass();
			if(superClass == null) {
				break;
			}
			if(model.getAllElements().containsKey(superClass.getName())) {
				parent = model.getAllElements().get(superClass.getName());
				break;
			}
		}
		Collections.reverse(classesForNewElements);
		for(Class<?> seed: classesForNewElements) {
			parent = createFrankElement(seed, parent);
			model.getAllElements().put(parent.getFullName(), parent);
		}
		group.getElements().put(parent.getFullName(), parent);
	}

	FrankElement createFrankElement(Class<?> clazz, FrankElement parent) {
		FrankElement result = new FrankElement(clazz.getName(), clazz.getSimpleName());
		result.setParent(parent);
		result.setAttributes(createAttributes(result, clazz.getDeclaredMethods()));
		return result;
	}

	static List<FrankAttribute> createAttributes(FrankElement frankElement, Method[] methods) {
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

	private static boolean documentAttribute(FrankAttribute attribute, Method method) {
		IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
		attribute.setDeprecated(AnnotationUtils.findAnnotation(method, Deprecated.class) != null);
		if(ibisDoc != null) {
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
			return true;
		}
		else {
			return false;
		}
	}
}
