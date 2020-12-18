/* 
Copyright 2020 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;
import org.xml.sax.SAXException;

import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.doc.objects.SpringBean;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class FrankDocModel {
	private static Logger log = LogUtil.getLogger(FrankDocModel.class);
	private static final String DIGESTER_RULES = "digester-rules.xml";
	static final String OTHER = "Other";

	private @Getter Map<String, ConfigChildSetterDescriptor> configChildDescriptors = new HashMap<>();
	private @Getter LinkedHashMap<String, FrankDocGroup> groups = new LinkedHashMap<>();
	// We want to iterate FrankElement in the order they are created, to be able
	// to create the ElementRole objects in the right order. 
	private @Getter Map<String, FrankElement> allElements = new LinkedHashMap<>();
	private @Getter Map<String, ElementType> allTypes = new HashMap<>();
	private @Getter Map<ElementRole.Key, ElementRole> allElementRoles = new HashMap<>();
	private final ElementRole.Factory elementRoleFactory = new ElementRole.Factory();

	/**
	 * Get the FrankDocModel needed in production. This is just a first draft. The
	 * present version does not have groups yet. It will be improved in future
	 * pull requests. 
	 */
	public static FrankDocModel populate() {
		return FrankDocModel.populate(DIGESTER_RULES, "nl.nn.adapterframework.configuration.Configuration");
	}

	public static FrankDocModel populate(final String digesterRulesFileName, final String rootClassName) {
		FrankDocModel result = new FrankDocModel();
		try {
			result.createConfigChildDescriptorsFrom(digesterRulesFileName);
			result.findOrCreateFrankElement(Utils.getClass(rootClassName));
			result.calculateFoundersElementRoles();
			result.setOverriddenFrom();
			result.buildGroups();
		} catch(Exception e) {
			log.fatal("Could not populate FrankDocModel", e);
			return null;
		}
		return result;
	}

	void createConfigChildDescriptorsFrom(String path) throws IOException, SAXException {
		Resource resource = Resource.getResource(path);
		if(resource == null) {
			throw new IOException(String.format("Cannot find resource on the classpath: [%s]", path));
		}
		try {
			XmlUtils.parseXml(resource.asInputSource(), new Handler(path));
		}
		catch(IOException e) {
			throw new IOException(String.format("An IOException occurred while parsing XML from [%s]", path), e);
		}
		catch(SAXException e) {
			throw new SAXException(String.format("A SAXException occurred while parsing XML from [%s]", path), e);
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

	public boolean hasType(String typeName) {
		return allTypes.containsKey(typeName);
	}

	FrankElement findOrCreateFrankElement(Class<?> clazz) throws ReflectiveOperationException {
		if(allElements.containsKey(clazz.getName())) {
			return allElements.get(clazz.getName());
		}
		FrankElement current = new FrankElement(clazz);
		allElements.put(clazz.getName(), current);
		Class<?> superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : findOrCreateFrankElement(superClass);
		current.setParent(parent);
		current.setAttributes(createAttributes(clazz.getDeclaredMethods(), current));
		current.setConfigChildren(createConfigChildren(clazz.getDeclaredMethods(), current));
		return current;
	}

	public FrankElement findFrankElement(String fullName) {
		return allElements.get(fullName);
	}

	List<FrankAttribute> createAttributes(Method[] methods, FrankElement attributeOwner) throws ReflectiveOperationException {
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
		Collections.sort(result);
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
				.filter(m -> Modifier.isPublic(m.getModifiers()))
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
		if(getter.getName().startsWith("get")) {
			// For issers we require an exact match of the type name. For getters,
			// the setter and the getter may mix boxed and unboxed types.
			// This allows the framework code to distinguish null values
			// (=not configured) from default values.
			setterType = Utils.promoteIfPrimitive(setterType);
			getterType = Utils.promoteIfPrimitive(getterType);
		}
		if(! getterType.equals(setterType)) {
			log.warn(String.format("In Frank element [%s]: setter [%s] has type [%s] while the getter has type [%s]",
					attributeOwner.getSimpleName(), setter.getName(), setterType, getterType));
		}
	}

	private void documentAttribute(FrankAttribute attribute, Method method, FrankElement attributeOwner) throws ReflectiveOperationException {
		attribute.setDeprecated(method.getAnnotation(Deprecated.class) != null);
		attribute.setDocumented(
				(method.getAnnotation(IbisDoc.class) != null)
				|| (method.getAnnotation(IbisDocRef.class) != null));
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

	private List<ConfigChild> createConfigChildren(Method[] methods, FrankElement parent) throws ReflectiveOperationException {
		List<ConfigChild> result = new ArrayList<>();
		for(ConfigChild.SortNode sortNode: createSortNodes(methods, parent)) {
			ConfigChild configChild = new ConfigChild(parent, sortNode);
			ConfigChildSetterDescriptor configChildDescriptor = configChildDescriptors.get(sortNode.getName());
			configChild.setAllowMultiple(configChildDescriptor.isAllowMultiple());
			configChild.setMandatory(configChildDescriptor.isMandatory());
			Class<?> elementTypeClass = sortNode.getMethod().getParameterTypes()[0];
			configChild.setElementRole(findOrCreateElementRole(
					elementTypeClass, configChildDescriptor.getSyntax1Name()));
			result.add(configChild);
		}
		return result;
	}

	private List<ConfigChild.SortNode> createSortNodes(Method[] methods, FrankElement parent) {
		List<Method> configChildSetters = Arrays.asList(methods).stream()
				.filter(m -> Modifier.isPublic(m.getModifiers()))
				.filter(Utils::isConfigChildSetter)
				.filter(m -> configChildDescriptors.get(m.getName()) != null)
				.collect(Collectors.toList());
		List<ConfigChild.SortNode> sortNodes = new ArrayList<>();
		for(Method setter: configChildSetters) {
			ConfigChild.SortNode sortNode = new ConfigChild.SortNode(setter);
			try {
				sortNode.parseIbisDocAnnotation();
			} catch(ConfigChild.IbisDocAnnotationException e) {
				log.warn(String.format("For FrankElement [%s]: %s", parent.getSimpleName(), e.getMessage()));
			}
			sortNodes.add(sortNode);
		}
		Collections.sort(sortNodes);
		return sortNodes;
	}

	ElementRole findOrCreateElementRole(Class<?> elementTypeClass, String syntax1Name) throws ReflectiveOperationException {
		ElementType elementType = findOrCreateElementType(elementTypeClass);
		ElementRole.Key key = new ElementRole.Key(elementType.getFullName(), syntax1Name);
		if(allElementRoles.containsKey(key)) {
			return allElementRoles.get(key);
		} else {
			ElementRole result = elementRoleFactory.create(elementType, syntax1Name);
			elementType.registerElementRole(result);
			allElementRoles.put(key, result);
			return result;
		}
	}

	public ElementRole findElementRole(ElementRole.Key key) {
		return allElementRoles.get(key);
	}

	public ElementRole findElementRole(ConfigChild configChild) {
		return findElementRole(new ElementRole.Key(configChild));
	}

	ElementRole findElementRole(String fullElementTypeName, String syntax1Name) {
		return allElementRoles.get(new ElementRole.Key(fullElementTypeName, syntax1Name));
	}

	ElementType findOrCreateElementType(Class<?> clazz) throws ReflectiveOperationException {
		if(allTypes.containsKey(clazz.getName())) {
			return allTypes.get(clazz.getName());
		}
		final ElementType result = new ElementType(clazz);
		// If a containing FrankElement contains the type being created, we do not
		// want recursion.
		allTypes.put(result.getFullName(), result);
		if(result.isFromJavaInterface()) {
			List<SpringBean> springBeans = Utils.getSpringBeans(clazz.getName());
			for(SpringBean b: springBeans) {
				FrankElement frankElement = findOrCreateFrankElement(b.getClazz());
				result.addMember(frankElement);
			}
		} else {
			result.addMember(findOrCreateFrankElement(clazz));
		}
		return result;
	}

	public ElementType findElementType(String fullName) {
		return allTypes.get(fullName);
	}

	void calculateFoundersElementRoles() {
		allTypes.values().forEach(et -> et.calculateHighestCommonInterface(this));
	}

	/**
	 * For an {@link ElementType} assemble all config children of members and calculate
	 * a list of {@link ElementRole} from that. Assume that the {@link ElementRole}
	 * objects exist and that the members of the element type are set.
	 */
	public List<ElementRole> getElementTypeMemberChildRoles(
			ElementType elementType,
			Predicate<ElementChild> selector,
			Predicate<ElementChild> rejector,
			Predicate<FrankElement> frankElementFilter) {
		List<ElementRole> allMemberChildRoles = elementType.getMembers().values().stream()
				// TODO: Filtering FrankElements, typically no filter or deprecated omitted, is
				// not covered by unit tests.
				.filter(frankElementFilter)
				.map(frankElement -> frankElement.getCumulativeConfigChildren(selector, rejector))
				.flatMap(l -> l.stream())
				.map(ElementRole.Key::new)
				// TODO: Not covered with unit tests that keys should be de-doubled
				.distinct()
				.map(this::findElementRole)
				.collect(Collectors.toList());
		Collections.sort(allMemberChildRoles, Comparator.comparing(er -> ((ElementRole) er).createXsdElementName("")));
		return allMemberChildRoles;
	}

	void buildGroups() {
		Map<String, List<FrankDocGroup>> groupsBase = new HashMap<>();
		List<FrankElement> membersOfOther = new ArrayList<>();
		for(ElementType elementType: getAllTypes().values()) {
			if(elementType.isFromJavaInterface()) {
				if(groupsBase.containsKey(elementType.getSimpleName())) {
					groupsBase.get(elementType.getSimpleName()).add(FrankDocGroup.getInstanceFromElementType(elementType));
				} else {
					groupsBase.put(elementType.getSimpleName(), Arrays.asList(FrankDocGroup.getInstanceFromElementType(elementType)));
				}
			}
			else {
				try {
					membersOfOther.add(elementType.getSingletonElement());
				} catch(ReflectiveOperationException e) {
					log.warn(String.format("Error adding ElementType [%s] to group other because it has multiple FrankElement objects: [%s]",
								elementType.getFullName(),
								elementType.getMembers().values().stream().map(FrankElement::getSimpleName).collect(Collectors.joining(", "))),
							e);
				}
			}
		}
		if(groupsBase.containsKey(OTHER)) {
			log.warn(String.format("Name \"[%s]\" cannot been used for others group because it is the name of an ElementType", OTHER));
		}
		else {
			groupsBase.put(OTHER, Arrays.asList(FrankDocGroup.getInstanceFromFrankElements(OTHER, membersOfOther)));
		}
		for(String groupName: groupsBase.keySet()) {
			if(groupsBase.get(groupName).size() != 1) {
				log.warn(String.format("Group name [%s] used for multiple groups", groupName));
			}
		}
		// Sort the groups alphabetically, including group "Other". We have to update
		// this code if "Other" needs to be put to the end.
		groups = new LinkedHashMap<>();
		List<String> sortedGroups = new ArrayList<>(groupsBase.keySet());
		Collections.sort(sortedGroups);
		for(String groupName: sortedGroups) {
			groups.put(groupName, groupsBase.get(groupName).get(0));
		}
	}

	void setOverriddenFrom() {
		Set<String> remainingElements = allElements.values().stream().map(FrankElement::getFullName).collect(Collectors.toSet());
		while(! remainingElements.isEmpty()) {
			FrankElement current = allElements.get(remainingElements.iterator().next());
			while((current.getParent() != null) && (remainingElements.contains(current.getParent().getFullName()))) {
				current = current.getParent();
			}
			current.getConfigChildren(ALL).forEach(c -> c.calculateOverriddenFrom());
			current.getAttributes(ALL).forEach(c -> c.calculateOverriddenFrom());
			current.getStatistics().finish();
			remainingElements.remove(current.getFullName());
		}
	}
}
