package nl.nn.adapterframework.doc.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class FrankDocModel {
	private static Logger log = LogUtil.getLogger(FrankDocModel.class);
	private static final String DIGESTER_RULES = "digester-rules.xml";
	
	private @Getter Map<String, ConfigChildSetterDescriptor> configChildDescriptors;
	private @Getter List<FrankDocGroup> groups;
	private @Getter Map<String, FrankElement> allElements = new HashMap<>();
	private @Getter Map<String, ElementType> allTypes = new HashMap<>();

	/**
	 * Get the FrankDocModel needed in production. This is just a first draft. The
	 * present version does not have groups yet. It will be improved in future
	 * pull requests. 
	 */
	public static FrankDocModel populate() {
		FrankDocModel result = new FrankDocModel();
		result.createConfigChildDescriptorsFrom(DIGESTER_RULES);
		result.findOrCreateElementType(Utils.getClass("nl.nn.adapterframework.core.IAdapter"));
		return result;
	}

	public FrankDocModel() {
		configChildDescriptors = new HashMap<>();
		groups = new ArrayList<>();
	}

	public void createConfigChildDescriptorsFrom(String path) {
		Resource resource = Resource.getResource(path);
		if(resource == null) {
			throw new RuntimeException(String.format("Cannot find resource on the classpath: [%s]", path));
		}
		try {
			XmlUtils.parseXml(resource.asInputSource(), new Handler(path));
		}
		catch(IOException e) {
			throw new RuntimeException(String.format("An IOException occurred while parsing XML from [%s]", path), e);
		}
		catch(SAXException e) {
			throw new RuntimeException(String.format("A SAXException occurred while parsing XML from [%s]", path), e);
		}
	}

	private class Handler extends DigesterRulesHandler {
		private final String path;

		Handler(String path) {
			this.path = path;
		}

		@Override
		protected void handle(DigesterRule rule) throws SAXException {
			String pattern = rule.getPattern();
			StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
			String syntax1Name = null;
			while(tokenizer.hasMoreElements()) {
				String token = tokenizer.nextToken();
				if(!"*".equals(token)) {
					syntax1Name = token;
				}
			}
			if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				add(rule.getRegisterMethod(), syntax1Name);
			}			
		}

		private void add(String registerMethod, String syntax1Name) throws SAXException {
			ConfigChildSetterDescriptor item = new ConfigChildSetterDescriptor(registerMethod, syntax1Name);
			if(configChildDescriptors.containsKey(item.getMethodName())) {
				log.warn(String.format("In digester rules [%s], duplicate method name [%s]", path, registerMethod));
			} else {
				configChildDescriptors.put(item.getMethodName(), item);
			}
		}
	}

	public ElementType findOrCreateElementType(Class<?> clazz) {
		if(allTypes.containsKey(clazz.getName())) {
			return allTypes.get(clazz.getName());
		}
		final ElementType result = new ElementType(clazz);
		// If a containing FrankElement contains the type being created, we do not
		// want recursion.
		allTypes.put(result.getFullName(), result);
		if(result.isFromJavaInterface()) {
			Utils.getSpringBeans(clazz.getName()).stream()
					.map(b -> b.getClazz())
					.map(cl -> findOrCreateFrankElement(cl))
					.forEach(result::addMember);
		} else {
			result.addMember(findOrCreateFrankElement(clazz));
		}
		return result;
	}

	public boolean hasType(String typeName) {
		return allTypes.containsKey(typeName);
	}

	public FrankElement findOrCreateFrankElement(Class<?> clazz) {
		if(allElements.containsKey(clazz.getName())) {
			return allElements.get(clazz.getName());
		}
		FrankElement current = new FrankElement(clazz);
		allElements.put(clazz.getName(), current);
		Class<?> superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : findOrCreateFrankElement(superClass);
		current.setParent(parent);
		current.setAttributes(createAttributes(clazz.getDeclaredMethods(), current));
		current.setConfigChildren(createConfigChildren(clazz.getMethods(), current));
		return current;
	}

	List<FrankAttribute> createAttributes(Method[] methods, FrankElement attributeOwner) {
		Map<String, Method> setterAttributes = getAttributeToMethodMap(methods, "set");
		Map<String, Method> getterAttributes = getGetterAndIsserAttributes(methods, attributeOwner);
		List<FrankAttribute> result = new ArrayList<>();
		for(Entry<String, Method> entry: setterAttributes.entrySet()) {
			String attributeName = entry.getKey();
			Method method = entry.getValue();
			if(getterAttributes.containsKey(attributeName)) {
				checkForTypeConflict(method, getterAttributes.get(attributeName), attributeOwner);
			}
			FrankAttribute attribute = new FrankAttribute(attributeName, attributeOwner);
			documentAttribute(attribute, method, attributeOwner);
			result.add(attribute);
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

	/**
     * The original order of the methods is preserved, which you get when you iterate
     * over the entrySet() of the returned Map.
	 */
	static Map<String, Method> getAttributeToMethodMap(Method[] methods, String prefix) {
		List<Method> methodList = Arrays.asList(methods);
		methodList = methodList.stream()
				.filter(Utils::isAttributeGetterOrSetter)
				.filter(m -> m.getName().startsWith(prefix) && (m.getName().length() > prefix.length()))
				.collect(Collectors.toList());		
		Map<String, Method> result = new LinkedHashMap<>();
		for(Method method: methodList) {
			String strippedName = method.getName().substring(prefix.length());
			String attributeName = strippedName.substring(0, 1).toLowerCase() + strippedName.substring(1);
			result.put(attributeName, method);
		}
		return result;
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
			IbisDoc ibisDoc = null;
			if(parsed.getReferredMethod() != null) {
				ibisDoc = AnnotationUtils.findAnnotation(parsed.getReferredMethod(), IbisDoc.class);
				if(ibisDoc != null) {
					attribute.setDescribingElement(findOrCreateFrankElement(parsed.getReferredMethod().getDeclaringClass()));
					attribute.parseIbisDocAnnotation(ibisDoc);
					if(parsed.hasOrder) {
						attribute.setOrder(parsed.getOrder());
					}
					return;
				}				
			} else {
				log.warn(String.format(
						"@IbisDocRef of Frank elelement [%s] attribute [%s] points to non-existent method", attributeOwner.getSimpleName(), attribute.getName()));
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

	private List<ConfigChild> createConfigChildren(	Method[] methods, FrankElement parent) {
		List<Method> configChildSetters = Arrays.asList(methods).stream()
				.filter(Utils::isConfigChildSetter)
				.filter(m -> configChildDescriptors.get(m.getName()) != null)
				.collect(Collectors.toList());
		List<ConfigChild> result = new ArrayList<>();
		for(Method m: configChildSetters) {
			ConfigChild configChild = new ConfigChild(parent);
			ConfigChildSetterDescriptor configChildDescriptor = configChildDescriptors.get(m.getName());
			IbisDoc ibisDoc = AnnotationUtils.findAnnotation(m, IbisDoc.class);
			try {
				configChild.setSequenceInConfigFromIbisDocAnnotation(ibisDoc);
			} catch(ParseException e) {
				log.warn(String.format("No config child order for method [%s] of Frank element [%s]",
						m.getName(), parent.getSimpleName()), e);
			}
			Class<?> elementClass = m.getParameterTypes()[0];
			configChild.setElementType(findOrCreateElementType(elementClass));
			configChild.setAllowMultiple(configChildDescriptor.isAllowMultiple());
			configChild.setMandatory(configChildDescriptor.isMandatory());
			configChild.setSyntax1Name(configChildDescriptor.getSyntax1Name());
			result.add(configChild);
		}
		return result;
	}
}
