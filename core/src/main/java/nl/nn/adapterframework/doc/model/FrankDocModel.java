/* 
Copyright 2020, 2021 WeAreFrank! 

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
	private Map<Set<ElementRole.Key>, ElementRoleSet> allElementRoleSets = new HashMap<>();

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
			if(log.isTraceEnabled()) {
				log.trace("Populating FrankDocModel");
			}
			result.createConfigChildDescriptorsFrom(digesterRulesFileName);
			result.findOrCreateFrankElement(Utils.getClass(rootClassName));
			result.calculateHighestCommonInterfaces();
			result.setOverriddenFrom();
			result.setHighestCommonInterface();
			result.createConfigChildSets();
			result.buildGroups();
		} catch(Exception e) {
			log.fatal("Could not populate FrankDocModel", e);
			return null;
		}
		if(log.isTraceEnabled()) {
			log.trace("Done populating FrankDocModel");
		}
		return result;
	}

	void createConfigChildDescriptorsFrom(String path) throws IOException, SAXException {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Creating config child descriptors from file [%s]", path));
		}
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
		if(log.isTraceEnabled()) {
			log.trace("Successfully created config child descriptors");
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
		if(log.isTraceEnabled()) {
			log.trace(String.format("FrankElement requested for class name [%s]", clazz.getName()));
		}
		if(allElements.containsKey(clazz.getName())) {
			if(log.isTraceEnabled()) {
				log.trace("Already present");
			}
			return allElements.get(clazz.getName());
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Creating FrankElement for class name [%s]", clazz.getName()));
		}
		FrankElement current = new FrankElement(clazz);
		allElements.put(clazz.getName(), current);
		Class<?> superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : findOrCreateFrankElement(superClass);
		current.setParent(parent);
		current.setAttributes(createAttributes(clazz.getDeclaredMethods(), current));
		current.setConfigChildren(createConfigChildren(clazz.getDeclaredMethods(), current));
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done creating FrankElement for class name [%s]", clazz.getName()));
		}
		return current;
	}

	public FrankElement findFrankElement(String fullName) {
		return allElements.get(fullName);
	}

	List<FrankAttribute> createAttributes(Method[] methods, FrankElement attributeOwner) throws ReflectiveOperationException {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Creating attributes for FrankElement [%s]", attributeOwner.getFullName()));
		}
		Map<String, Method> setterAttributes = getAttributeToMethodMap(methods, "set");
		Map<String, Method> getterAttributes = getGetterAndIsserAttributes(methods, attributeOwner);
		List<FrankAttribute> result = new ArrayList<>();
		for(Entry<String, Method> entry: setterAttributes.entrySet()) {
			String attributeName = entry.getKey();
			if(log.isTraceEnabled()) {
				log.trace(String.format("Attribute [%s]", attributeName));
			}
			Method method = entry.getValue();
			if(getterAttributes.containsKey(attributeName)) {
				checkForTypeConflict(method, getterAttributes.get(attributeName), attributeOwner);
			}
			FrankAttribute attribute = new FrankAttribute(attributeName, attributeOwner);
			documentAttribute(attribute, method, attributeOwner);
			result.add(attribute);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Attribute [%s] done", attributeName));
			}
		}
		Collections.sort(result);
		if(log.isTraceEnabled()) {
			log.trace("Sorted the attributes");
			log.trace("Done creating attributes");
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
		if(log.isTraceEnabled()) {
			log.trace(String.format("Checking for type conflict with getter or isser [%s]", getter.getName()));
		}
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
		if(log.isTraceEnabled()) {
			log.trace(String.format("Attribute: deprecated = [%b], documented = [%b]", attribute.isDeprecated(), attribute.isDocumented()));
		}
		IbisDocRef ibisDocRef = AnnotationUtils.findAnnotation(method, IbisDocRef.class);
		if(ibisDocRef != null) {
			if(log.isTraceEnabled()) {
				log.trace("Found @IbisDocRef annotation");
			}
			ParsedIbisDocRef parsed = parseIbisDocRef(ibisDocRef, method);
			IbisDoc ibisDoc = null;
			if(parsed.getReferredMethod() != null) {
				ibisDoc = AnnotationUtils.findAnnotation(parsed.getReferredMethod(), IbisDoc.class);
				if(ibisDoc != null) {
					attribute.setDescribingElement(findOrCreateFrankElement(parsed.getReferredMethod().getDeclaringClass()));
					if(log.isTraceEnabled()) {
						log.trace(String.format("Describing element of attribute [%s].[%s] is [%s]",
								attributeOwner.getFullName(), attribute.getName(), attribute.getDescribingElement().getFullName()));
					}
					if(! attribute.parseIbisDocAnnotation(ibisDoc)) {
						log.warn(String.format("FrankAttribute [%s] of FrankElement [%s] does not have a configured order", attribute.getName(), attributeOwner.getFullName()));
					}
					if(parsed.hasOrder) {
						attribute.setOrder(parsed.getOrder());
						if(log.isTraceEnabled()) {
							log.trace(String.format("Attribute [%s] has order from @IbisDocRef: [%d]", attribute.getName(), attribute.getOrder()));
						}
					}
					if(log.isTraceEnabled()) {
						log.trace(String.format("Done documenting attribute [%s]", attribute.getName()));
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
			if(log.isTraceEnabled()) {
				log.trace(String.format("For attribute [%s], have @IbisDoc without @IbisDocRef", attribute));
			}
			attribute.parseIbisDocAnnotation(ibisDoc);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Order [%d], default [%s]", attribute.getOrder(), attribute.getDefaultValue()));
			}
		}
		else {
			log.warn(String.format("No documentation available for FrankElement [%s], attribute [%s]",
					attributeOwner.getSimpleName(), attribute.getName()));
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done documenting attribute [%s]", attribute.getName()));
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
		if(log.isTraceEnabled()) {
			log.trace(String.format("Creating config children of FrankElement [%s]", parent.getFullName()));
		}
		List<ConfigChild> result = new ArrayList<>();
		for(ConfigChild.SortNode sortNode: createSortNodes(methods, parent)) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Have config child SortNode [%s]", sortNode.getName()));
			}
			ConfigChild configChild = new ConfigChild(parent, sortNode);
			ConfigChildSetterDescriptor configChildDescriptor = configChildDescriptors.get(sortNode.getName());
			if(log.isTraceEnabled()) {
				log.trace(String.format("Have ConfigChildSetterDescriptor, methodName = [%s], syntax1Name = [%s], mandatory = [%b], allowMultiple = [%b]",
						configChildDescriptor.getMethodName(), configChildDescriptor.getSyntax1Name(), configChildDescriptor.isMandatory(), configChildDescriptor.isAllowMultiple()));
			}
			configChild.setAllowMultiple(configChildDescriptor.isAllowMultiple());
			configChild.setMandatory(configChildDescriptor.isMandatory());
			if(log.isTraceEnabled()) {
				log.trace(String.format("For FrankElement [%s] method [%s], going to search element role", parent.getFullName(), sortNode.getName()));
			}
			configChild.setElementRole(findOrCreateElementRole(
					sortNode.getElementTypeClass(), configChildDescriptor.getSyntax1Name()));
			if(log.isTraceEnabled()) {
				log.trace(String.format("For FrankElement [%s] method [%s], have the element role", parent.getFullName(), sortNode.getName()));
			}
			if(sortNode.getIbisDoc() == null) {
				log.warn(String.format("No @IbisDoc annotation for config child [%s] of FrankElement [%s]", configChild.getKey().toString(), parent.getFullName()));
			} else if(! configChild.parseIbisDocAnnotation(sortNode.getIbisDoc())) {
				log.warn(String.format("@IbisDoc annotation for config child [%s] of FrankElement [%s] does not specify a sort order", configChild.getKey().toString(), parent.getFullName()));
			}
			if(! StringUtils.isEmpty(configChild.getDefaultValue())) {
				log.warn(String.format("Default value [%s] of config child [%s] of FrankElement [%s] is not used", configChild.getDefaultValue(), configChild.getKey().toString(), parent.getFullName()));
			}
			result.add(configChild);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done creating ConfigChild for SortNode [%s], order = [%d]", sortNode.getName(), configChild.getOrder()));
			}
		}
		Collections.sort(result);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done creating config children of FrankElement [%s]", parent.getFullName()));
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
			sortNodes.add(sortNode);
		}
		Collections.sort(sortNodes);
		return sortNodes;
	}

	ElementRole findOrCreateElementRole(Class<?> elementTypeClass, String syntax1Name) throws ReflectiveOperationException {
		if(log.isTraceEnabled()) {
			log.trace(String.format("ElementRole requested for elementTypeClass [%s] and syntax1Name [%s]", elementTypeClass.getName(), syntax1Name));
			log.trace("Going to get the ElementType");
		}
		ElementType elementType = findOrCreateElementType(elementTypeClass);
		ElementRole.Key key = new ElementRole.Key(elementTypeClass.getName(), syntax1Name);
		if(allElementRoles.containsKey(key)) {
			if(log.isTraceEnabled()) {
				log.trace("ElementRole already present");
			}
			ElementRole result = allElementRoles.get(key);
			return result;
		} else {
			ElementRole result = elementRoleFactory.create(elementType, syntax1Name);
			allElementRoles.put(key, result);
			if(log.isTraceEnabled()) {
				log.trace(String.format("For ElementType [%s] and syntax1Name [%s], created ElementRole [%s]", elementType.getFullName(), syntax1Name, result.createXsdElementName("")));
			}
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
		if(log.isTraceEnabled()) {
			log.trace(String.format("Requested ElementType for class [%s]", clazz.getName()));
		}
		if(allTypes.containsKey(clazz.getName())) {
			if(log.isTraceEnabled()) {
				log.trace("Already present");
			}
			return allTypes.get(clazz.getName());
		}
		final ElementType result = new ElementType(clazz);
		// If a containing FrankElement contains the type being created, we do not
		// want recursion.
		allTypes.put(result.getFullName(), result);
		if(result.isFromJavaInterface()) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Class [%s] is a Java interface, going to create all member FrankElement", clazz.getName()));
			}
			List<SpringBean> springBeans = Utils.getSpringBeans(clazz.getName());
			for(SpringBean b: springBeans) {
				FrankElement frankElement = findOrCreateFrankElement(b.getClazz());
				result.addMember(frankElement);
			}
		} else {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Class [%s] is not a Java interface, creating its FrankElement", clazz.getName()));
			}
			result.addMember(findOrCreateFrankElement(clazz));
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done creating ElementType for class [%s]", clazz.getName()));
		}
		return result;
	}

	public ElementType findElementType(String fullName) {
		return allTypes.get(fullName);
	}

	void calculateHighestCommonInterfaces() {
		if(log.isTraceEnabled()) {
			log.trace("Going to calculate highest common interface for every ElementType");
		}
		allTypes.values().forEach(et -> et.calculateHighestCommonInterface(this));
		if(log.isTraceEnabled()) {
			log.trace("Done calculating highest common interface for every ElementType");
		}		
	}

	void buildGroups() {
		if(log.isTraceEnabled()) {
			log.trace("Building groups");
		}
		Map<String, List<FrankDocGroup>> groupsBase = new HashMap<>();
		List<FrankElement> membersOfOther = new ArrayList<>();
		for(ElementType elementType: getAllTypes().values()) {
			if(elementType.isFromJavaInterface()) {
				if(groupsBase.containsKey(elementType.getSimpleName())) {
					groupsBase.get(elementType.getSimpleName()).add(FrankDocGroup.getInstanceFromElementType(elementType));
				} else {
					groupsBase.put(elementType.getSimpleName(), Arrays.asList(FrankDocGroup.getInstanceFromElementType(elementType)));
				}
				if(log.isTraceEnabled()) {
					log.trace(String.format("Appended group [%s] with candidate element type [%s], which is based on a Java interface",
							elementType.getSimpleName(), elementType.getFullName()));
				}
			}
			else {
				try {
					membersOfOther.add(elementType.getSingletonElement());
					if(log.isTraceEnabled()) {
						log.trace(String.format("Appended the others group with FrankElement [%s]", elementType.getSingletonElement().getFullName()));
					}
				} catch(ReflectiveOperationException e) {
					String frankElementsString = elementType.getMembers().stream()
							.map(FrankElement::getSimpleName).collect(Collectors.joining(", "));
					log.warn(String.format("Error adding ElementType [%s] to group other because it has multiple FrankElement objects: [%s]",
								elementType.getFullName(), frankElementsString), e);
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
			if(log.isTraceEnabled()) {
				log.trace(String.format("Creating group [%s]", groupName));
			}
			groups.put(groupName, groupsBase.get(groupName).get(0));
		}
		if(log.isTraceEnabled()) {
			log.trace("Done building groups");
		}
	}

	void setOverriddenFrom() {
		if(log.isTraceEnabled()) {
			log.trace("Going to set property overriddenFrom for all config children and all attributes of all FrankElement");
		}
		Set<String> remainingElements = allElements.values().stream().map(FrankElement::getFullName).collect(Collectors.toSet());
		while(! remainingElements.isEmpty()) {
			FrankElement current = allElements.get(remainingElements.iterator().next());
			while((current.getParent() != null) && (remainingElements.contains(current.getParent().getFullName()))) {
				current = current.getParent();
			}
			if(log.isTraceEnabled()) {
				log.trace(String.format("Seting property overriddenFrom for all config children and all attributes of FrankElement [%s]", current.getFullName()));
			}
			current.getConfigChildren(ALL).forEach(c -> c.calculateOverriddenFrom());
			current.getAttributes(ALL).forEach(c -> c.calculateOverriddenFrom());
			current.getStatistics().finish();
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done seting property overriddenFrom for FrankElement [%s]", current.getFullName()));
			}
			remainingElements.remove(current.getFullName());
		}
		if(log.isTraceEnabled()) {
			log.trace("Done setting property overriddenFrom");
		}
	}

	void setHighestCommonInterface() {
		if(log.isTraceEnabled()) {
			log.trace("Doing FrankDocModel.setHighestCommonInterface");
		}
		for(ElementRole role: allElementRoles.values()) {
			String syntax1Name = role.getSyntax1Name();
			ElementType et = role.getElementType().getHighestCommonInterface();
			ElementRole result = findElementRole(new ElementRole.Key(et.getFullName(), syntax1Name));
			if(result == null) {
				log.warn(String.format("Promoting ElementRole [%s] results in ElementType [%s] and syntax 1 name [%s], but there is no corresponding ElementRole",
						toString(), et.getFullName(), syntax1Name));
				role.setHighestCommonInterface(role);
			} else {
				role.setHighestCommonInterface(result);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Role [%s] has highest common interface [%s]", role.toString(), result.toString()));
				}
			}
		}
		if(log.isTraceEnabled()) {
			log.trace("Done FrankDocModel.setHighestCommonInterface");
		}
	}

	void createConfigChildSets() {
		if(log.isTraceEnabled()) {
			log.trace("Doing FrankDocModel.createConfigChildSets");
		}
		allElementRoles.values().forEach(ElementRole::initConflicts);
		List<FrankElement> sortedFrankElements = new ArrayList<>(allElements.values());
		Collections.sort(sortedFrankElements);
		sortedFrankElements.forEach(this::createConfigChildSets);
		List<ElementRole> sortedElementRoles = new ArrayList<>(allElementRoles.values());
		Collections.sort(sortedElementRoles);
		sortedElementRoles.stream()
			.filter(role -> role.getElementType().isFromJavaInterface())
			.forEach(role -> recursivelyCreateElementRoleSets(Arrays.asList(role), 1));
		allElementRoleSets.values().forEach(ElementRoleSet::initConflicts);
		if(log.isTraceEnabled()) {
			log.trace("Done FrankDocModel.createConfigChildSets");
		}
	}

	private void createConfigChildSets(FrankElement frankElement) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Handling FrankElement [%s]", frankElement.getFullName()));
		}
		Map<String, List<ConfigChild>> cumChildrenBySyntax1Name = frankElement.getCumulativeConfigChildren(ElementChild.ALL, ElementChild.NONE).stream()
				.collect(Collectors.groupingBy(c -> c.getElementRole().getSyntax1Name()));
		for(String syntax1Name: cumChildrenBySyntax1Name.keySet()) {
			List<ConfigChild> configChildren = cumChildrenBySyntax1Name.get(syntax1Name);
			if(configChildren.stream().map(ConfigChild::getOwningElement).anyMatch(childOwner -> (childOwner == frankElement))) {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Found ConfigChildSet for syntax 1 name [%s]", syntax1Name));
				}
				ConfigChildSet configChildSet = new ConfigChildSet(configChildren);
				frankElement.addConfigChildSet(configChildSet);
				ElementRoleSet elementRoleSet = findOrCreateElementRoleSet(configChildSet);
				if(log.isTraceEnabled()) {
					log.trace(String.format("The config child with syntax 1 name [%s] has ElementRoleSet [%s]", syntax1Name, elementRoleSet.toString()));
				}
				configChildSet.setElementRoleSet(elementRoleSet);
			}
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done handling FrankElement [%s]", frankElement.getFullName()));
		}
	}

	private ElementRoleSet findOrCreateElementRoleSet(ConfigChildSet configChildSet) {
		Set<ElementRole> roles = configChildSet.getConfigChildren().stream()
				.map(ConfigChild::getElementRole)
				.collect(Collectors.toSet());
		Set<ElementRole.Key> key = roles.stream()
				.map(ElementRole::getKey)
				.collect(Collectors.toSet());
		if(! allElementRoleSets.containsKey(key)) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("New ElementRoleSet for roles [%s]", ElementRole.describeCollection(roles)));
			}
			allElementRoleSets.put(key, new ElementRoleSet(roles));
		}
		return allElementRoleSets.get(key);
	}

	/**
	 * Create {@link nl.nn.adapterframework.doc.model.ElementRoleSet}, taking
	 * care of generic element option recursion as explained in
	 * {@link nl.nn.adapterframework.doc.model}.
	 */
	private void recursivelyCreateElementRoleSets(List<ElementRole> roleGroup, int recursionDepth) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Enter with roles [%s] and recursion depth [%d]", ElementRole.describeCollection(roleGroup), recursionDepth));
		}
		List<FrankElement> rawMembers = roleGroup.stream()
				.flatMap(role -> role.getRawMembers().stream())
				.distinct()
				.collect(Collectors.toList());
		Map<String, List<ConfigChild>> configChildrenBySyntax1Name = rawMembers.stream()
				.flatMap(element -> element.getConfigChildren(ElementChild.ALL).stream())
				.collect(Collectors.groupingBy(ConfigChild::getSyntax1Name));
		List<String> names = new ArrayList<>(configChildrenBySyntax1Name.keySet());
		Collections.sort(names);
		for(String name: names) {
			List<ConfigChild> configChildren = configChildrenBySyntax1Name.get(name);
			Set<ElementRole> roles = configChildren.stream().map(ConfigChild::getElementRole).collect(Collectors.toSet());
			Set<ElementRole.Key> key = roles.stream().map(ElementRole::getKey).collect(Collectors.toSet());
			if(! allElementRoleSets.containsKey(key)) {
				allElementRoleSets.put(key, new ElementRoleSet(roles));
				if(log.isTraceEnabled()) {
					log.trace(String.format("Added new ElementRoleSet [%s]", allElementRoleSets.get(key).toString()));
				}
				List<ElementRole> recursionParents = new ArrayList<>(roles);
				recursionParents = recursionParents.stream().collect(Collectors.toList());
				Collections.sort(recursionParents);
				recursivelyCreateElementRoleSets(recursionParents, recursionDepth + 1);
			}
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Leave for roles [%s] and recursion depth [%d]", ElementRole.describeCollection(roleGroup), recursionDepth));
		}
	}
}
