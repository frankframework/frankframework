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

package nl.nn.adapterframework.frankdoc.model;

import static nl.nn.adapterframework.frankdoc.model.ElementChild.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.frankdoc.Utils;
import nl.nn.adapterframework.frankdoc.doclet.FrankAnnotation;
import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocletConstants;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class FrankDocModel {
	private static Logger log = LogUtil.getLogger(FrankDocModel.class);
	private static String ENUM = "Enum";
	private static final String DIGESTER_RULES = "digester-rules.xml";
	static final String OTHER = "Other";

	private FrankClassRepository classRepository;

	private @Getter Map<String, ConfigChildSetterDescriptor> configChildDescriptors = new HashMap<>();
	
	/**
	 * Values of the groups map are sorted alphabetically.
	 */
	private @Getter LinkedHashMap<String, FrankDocGroup> groups = new LinkedHashMap<>();

	// We want to iterate FrankElement in the order they are created, to be able
	// to create the ElementRole objects in the right order. 
	private @Getter Map<String, FrankElement> allElements = new LinkedHashMap<>();

	// We have a LinkedHashMap because the sequence of the types is relevant. This
	// sequence determines the sort order of the elements of FrankDocGroup Other.
	private @Getter Map<String, ElementType> allTypes = new LinkedHashMap<>();

	private @Getter Map<ElementRole.Key, ElementRole> allElementRoles = new HashMap<>();
	private final ElementRole.Factory elementRoleFactory = new ElementRole.Factory();
	private Map<Set<ElementRole.Key>, ElementRoleSet> allElementRoleSets = new HashMap<>();
	private AttributeValuesFactory attributeValuesFactory = new AttributeValuesFactory();

	FrankDocModel(FrankClassRepository classRepository) {
		this.classRepository = classRepository;
	}

	/**
	 * Get the FrankDocModel needed in production. This is just a first draft. The
	 * present version does not have groups yet. It will be improved in future
	 * pull requests. 
	 */
	public static FrankDocModel populate(FrankClassRepository classRepository) {
		return FrankDocModel.populate(DIGESTER_RULES, "nl.nn.adapterframework.configuration.Configuration", classRepository);
	}

	public static FrankDocModel populate(final String digesterRulesFileName, final String rootClassName, FrankClassRepository classRepository) {
		FrankDocModel result = new FrankDocModel(classRepository);
		try {
			log.trace("Populating FrankDocModel");
			result.createConfigChildDescriptorsFrom(digesterRulesFileName);
			result.findOrCreateFrankElement(rootClassName);
			result.calculateHighestCommonInterfaces();
			result.setOverriddenFrom();
			result.setHighestCommonInterface();
			result.createConfigChildSets();
			result.setElementNamesOfFrankElements(rootClassName);
			result.buildGroups();
		} catch(Exception e) {
			log.fatal("Could not populate FrankDocModel", e);
			return null;
		}
		log.trace("Done populating FrankDocModel");
		return result;
	}

	void createConfigChildDescriptorsFrom(String path) throws IOException, SAXException {
		log.trace("Creating config child descriptors from file [{}]", path);
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
		log.trace("Successfully created config child descriptors");
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
			String roleName = null;
			while(tokenizer.hasMoreElements()) {
				String token = tokenizer.nextToken();
				if(!"*".equals(token)) {
					roleName = token;
				}
			}
			if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				add(rule.getRegisterMethod(), roleName);
			}			
		}

		private void add(String registerMethod, String roleName) throws SAXException {
			ConfigChildSetterDescriptor item = new ConfigChildSetterDescriptor(registerMethod, roleName);
			if(configChildDescriptors.containsKey(item.getMethodName())) {
				log.warn("In digester rules [{}], duplicate method name [{}]", path, registerMethod);
			} else {
				configChildDescriptors.put(item.getMethodName(), item);
			}
		}
	}

	public boolean hasType(String typeName) {
		return allTypes.containsKey(typeName);
	}

	FrankElement findOrCreateFrankElement(String fullClassName) throws FrankDocException {
		FrankClass clazz = classRepository.findClass(fullClassName);
		log.trace("FrankElement requested for class name [{}]", () -> clazz.getName());
		if(allElements.containsKey(clazz.getName())) {
			log.trace("Already present");
			return allElements.get(clazz.getName());
		}
		log.trace("Creating FrankElement for class name [{}]", () -> clazz.getName());
		FrankElement current = new FrankElement(clazz);
		allElements.put(clazz.getName(), current);
		FrankClass superClass = clazz.getSuperclass();
		FrankElement parent = superClass == null ? null : findOrCreateFrankElement(superClass.getName());
		current.setParent(parent);
		current.setAttributes(createAttributes(clazz, current));
		current.setConfigChildren(createConfigChildren(clazz.getDeclaredMethods(), current));
		log.trace("Done creating FrankElement for class name [{}]", () -> clazz.getName());
		return current;
	}

	public FrankElement findFrankElement(String fullName) {
		return allElements.get(fullName);
	}

	List<FrankAttribute> createAttributes(FrankClass clazz, FrankElement attributeOwner) throws FrankDocException {
		log.trace("Creating attributes for FrankElement [{}]", () -> attributeOwner.getFullName());
		FrankMethod[] methods = clazz.getDeclaredMethods();
		Map<String, FrankMethod> enumGettersByAttributeName = getEnumGettersByAttributeName(clazz);
		Map<String, FrankMethod> setterAttributes = getAttributeToMethodMap(methods, "set");
		Map<String, FrankMethod> getterAttributes = getGetterAndIsserAttributes(methods, attributeOwner);
		List<FrankAttribute> result = new ArrayList<>();
		for(Entry<String, FrankMethod> entry: setterAttributes.entrySet()) {
			String attributeName = entry.getKey();
			log.trace("Attribute [{}]", attributeName);
			FrankMethod method = entry.getValue();
			if(getterAttributes.containsKey(attributeName)) {
				checkForTypeConflict(method, getterAttributes.get(attributeName), attributeOwner);
			}
			FrankAttribute attribute = new FrankAttribute(attributeName, attributeOwner);
			attribute.setAttributeType(AttributeType.fromJavaType(method.getParameterTypes()[0].getName()));
			documentAttribute(attribute, method, attributeOwner);
			if(enumGettersByAttributeName.containsKey(attributeName)) {
				attribute.setAttributeValues(findOrCreateAttributeValues((FrankClass) enumGettersByAttributeName.get(attributeName).getReturnType()));
			}
			result.add(attribute);
			log.trace("Attribute [{}] done", attributeName);
		}
		Collections.sort(result);
		log.trace("Sorted the attributes and done creating attributes");
		return result;
	}

	private Map<String, FrankMethod> getGetterAndIsserAttributes(FrankMethod[] methods, FrankElement attributeOwner) {
		Map<String, FrankMethod> getterAttributes = getAttributeToMethodMap(methods, "get");
		Map<String, FrankMethod> isserAttributes = getAttributeToMethodMap(methods, "is");
		for(String isserAttributeName : isserAttributes.keySet()) {
			if(getterAttributes.containsKey(isserAttributeName)) {
				log.warn("For FrankElement [{}], attribute [{}] has both a getX and an isX method", () -> attributeOwner.getSimpleName(), () -> isserAttributeName);
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
	static Map<String, FrankMethod> getAttributeToMethodMap(FrankMethod[] methods, String prefix) {
		List<FrankMethod> methodList = Arrays.asList(methods);
		methodList = methodList.stream()
				.filter(FrankMethod::isPublic)
				.filter(Utils::isAttributeGetterOrSetter)
				.filter(m -> m.getName().startsWith(prefix) && (m.getName().length() > prefix.length()))
				.collect(Collectors.toList());
		// The sort order determines the creation order of AttributeValues instances.
		Collections.sort(methodList, Comparator.comparing(FrankMethod::getName));
		Map<String, FrankMethod> result = new LinkedHashMap<>();
		for(FrankMethod method: methodList) {
			String attributeName = attributeOf(method.getName(), prefix);
			result.put(attributeName, method);
		}
		return result;
	}

	private static String attributeOf(String methodName, String prefix) {
		String strippedName = methodName.substring(prefix.length());
		String attributeName = strippedName.substring(0, 1).toLowerCase() + strippedName.substring(1);
		return attributeName;
	}

	static Map<String, FrankMethod> getEnumGettersByAttributeName(FrankClass clazz) {
		FrankMethod[] rawMethods = clazz.getDeclaredAndInheritedMethods();
		List<FrankMethod> methods = Arrays.asList(rawMethods).stream()
				.filter(m -> m.getName().endsWith(ENUM))
				.filter(m -> m.getReturnType().isEnum())
				.filter(m -> m.getParameterCount() == 0)
				// This filter cannot be covered with tests, because getMethods
				// does not include a non-public method in the test classes.
				.filter(FrankMethod::isPublic)
				.collect(Collectors.toList());
		Map<String, FrankMethod> result = new HashMap<>();
		for(FrankMethod m: methods) {
			result.put(enumAttributeOf(m), m);
		}
		return result;
	}

	private static String enumAttributeOf(FrankMethod method) {
		String nameWithoutEnum = method.getName().substring(0, method.getName().length() - ENUM.length());
		return attributeOf(nameWithoutEnum, "get");
	}

	private void checkForTypeConflict(FrankMethod setter, FrankMethod getter, FrankElement attributeOwner) {
		log.trace("Checking for type conflict with getter or isser [{}]", () -> getter.getName());
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
			// Cannot work with lambdas because setterType is not final.
			if(log.isWarnEnabled()) {
				log.warn("In Frank element [{}]: setter [{}] has type [{}] while the getter has type [{}]",
						attributeOwner.getSimpleName(), setter.getName(), setterType, getterType);
			}
		}
	}

	private void documentAttribute(FrankAttribute attribute, FrankMethod method, FrankElement attributeOwner) throws FrankDocException {
		attribute.setDeprecated(method.getAnnotation(FrankDocletConstants.DEPRECATED) != null);
		attribute.setDocumented(
				(method.getAnnotation(FrankDocletConstants.IBISDOC) != null)
				|| (method.getAnnotation(FrankDocletConstants.IBISDOCREF) != null));
		log.trace("Attribute: deprecated = [{}], documented = [{}]", () -> attribute.isDeprecated(), () -> attribute.isDocumented());
		FrankAnnotation ibisDocRef = method.getAnnotationInludingInherited(FrankDocletConstants.IBISDOCREF);
		if(ibisDocRef != null) {
			log.trace("Found @IbisDocRef annotation");
			ParsedIbisDocRef parsed = parseIbisDocRef(ibisDocRef, method);
			FrankAnnotation ibisDoc = null;
			if((parsed != null) && (parsed.getReferredMethod() != null)) {
				ibisDoc = parsed.getReferredMethod().getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
				if(ibisDoc != null) {
					attribute.setDescribingElement(findOrCreateFrankElement(parsed.getReferredMethod().getDeclaringClass().getName()));
					log.trace("Describing element of attribute [{}].[{}] is [{}]",
							() -> attributeOwner.getFullName(), () -> attribute.getName(), () -> attribute.getDescribingElement().getFullName());
					if(! attribute.parseIbisDocAnnotation(ibisDoc)) {
						log.warn("FrankAttribute [{}] of FrankElement [{}] does not have a configured order", () -> attribute.getName(), () -> attributeOwner.getFullName());
					}
					if(parsed.hasOrder) {
						attribute.setOrder(parsed.getOrder());
						log.trace("Attribute [{}] has order from @IbisDocRef: [{}]", () -> attribute.getName(), () -> attribute.getOrder());
					}
					log.trace("Done documenting attribute [{}]", () -> attribute.getName());
					return;
				}				
			} else {
				log.warn("@IbisDocRef of Frank elelement [{}] attribute [{}] points to non-existent method", () -> attributeOwner.getSimpleName(), () -> attribute.getName());
			}
		}
		FrankAnnotation ibisDoc = method.getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
		if(ibisDoc != null) {
			log.trace("For attribute [{}], have @IbisDoc without @IbisDocRef", attribute);
			attribute.parseIbisDocAnnotation(ibisDoc);
			log.trace("Order [{}], default [{}]", () -> attribute.getOrder(), () -> attribute.getDefaultValue());
		}
		else {
			log.warn("No documentation available for FrankElement [{}], attribute [{}]", () -> attributeOwner.getSimpleName(), () -> attribute.getName());
		}
		log.trace("Done documenting attribute [{}]", () -> attribute.getName());
	}

	private class ParsedIbisDocRef {
		private @Getter @Setter boolean hasOrder;
		private @Getter @Setter int order;
		private @Getter @Setter FrankMethod referredMethod;
	}

	private ParsedIbisDocRef parseIbisDocRef(FrankAnnotation ibisDocRef, FrankMethod originalMethod) {
		ParsedIbisDocRef result = new ParsedIbisDocRef();
		result.setHasOrder(false);
		String[] values = null;
		try {
			values = (String[]) ibisDocRef.getValue();
		} catch(FrankDocException e) {
			log.warn("IbisDocRef annotation did not have a value", e);
			return result;
		}
		String methodString = null;
		if (values.length == 1) {
			methodString = values[0];
		} else if (values.length == 2) {
			methodString = values[1];
			try {
				result.setOrder(Integer.parseInt(values[0]));
				result.setHasOrder(true);
			} catch (Throwable t) {
				final String[] finalValues = values;
				log.warn("Could not parse order in @IbisDocRef annotation: [{}]", () -> finalValues[0]);
			}
		}
		else {
			log.warn("Too many or zero parameters in @IbisDocRef annotation on method: [{}].[{}]", () -> originalMethod.getDeclaringClass().getName(), () -> originalMethod.getName());
			return null;
		}
		try {
			result.setReferredMethod(getReferredMethod(methodString, originalMethod));
		} catch(Exception e) {
			log.warn("@IbisDocRef on [{}].[{}] annotation references invalid method [{}], ignoring @IbisDocRef annotation",
					originalMethod.getDeclaringClass().getName(), originalMethod.getName(), methodString);
			return null;
		}
		return result;
	}

	private FrankMethod getReferredMethod(String methodString, FrankMethod originalMethod) {
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

	private FrankMethod getParentMethod(String className, String methodName) {
		try {
			FrankClass parentClass = classRepository.findClass(className);
			if(parentClass == null) {
				log.warn("Class {} is unknown", className);
				return null;
			}
			for (FrankMethod parentMethod : parentClass.getDeclaredAndInheritedMethods()) {
				if (parentMethod.getName().equals(methodName)) {
					return parentMethod;
				}
			}
			return null;
		} catch (FrankDocException e) {
			log.warn("Super class [{}] was not found!", className, e);
			return null;
		}
	}

	private List<ConfigChild> createConfigChildren(FrankMethod[] methods, FrankElement parent) throws FrankDocException {
		log.trace("Creating config children of FrankElement [{}]", () -> parent.getFullName());
		List<ConfigChild> result = new ArrayList<>();
		for(ConfigChild.SortNode sortNode: createSortNodes(methods, parent)) {
			log.trace("Have config child SortNode [{}]", () -> sortNode.getName());
			ConfigChild configChild = new ConfigChild(parent, sortNode);
			ConfigChildSetterDescriptor configChildDescriptor = configChildDescriptors.get(sortNode.getName());
			log.trace("Have ConfigChildSetterDescriptor, methodName = [{}], roleName = [{}], mandatory = [{}], allowMultiple = [{}]",
					() -> configChildDescriptor.getMethodName(), () -> configChildDescriptor.getRoleName(), () -> configChildDescriptor.isMandatory(), () -> configChildDescriptor.isAllowMultiple());
			configChild.setAllowMultiple(configChildDescriptor.isAllowMultiple());
			configChild.setMandatory(configChildDescriptor.isMandatory());
			log.trace("For FrankElement [{}] method [{}], going to search element role", () -> parent.getFullName(), () -> sortNode.getName());
			configChild.setElementRole(findOrCreateElementRole(
					(FrankClass) sortNode.getElementType(), configChildDescriptor.getRoleName()));
			log.trace("For FrankElement [{}] method [{}], have the element role", () -> parent.getFullName(), () -> sortNode.getName());
			if(sortNode.getIbisDoc() == null) {
				log.warn("No @IbisDoc annotation for config child [{}] of FrankElement [{}]", () -> configChild.getKey().toString(), () -> parent.getFullName());
			} else if(! configChild.parseIbisDocAnnotation(sortNode.getIbisDoc())) {
				log.warn("@IbisDoc annotation for config child [{}] of FrankElement [{}] does not specify a sort order", () -> configChild.getKey().toString(), () -> parent.getFullName());
			}
			if(! StringUtils.isEmpty(configChild.getDefaultValue())) {
				log.warn("Default value [{}] of config child [{}] of FrankElement [{}] is not used", () -> configChild.getDefaultValue(), () -> configChild.getKey().toString(), () -> parent.getFullName());
			}
			result.add(configChild);
			log.trace("Done creating ConfigChild for SortNode [{}], order = [{}]", () -> sortNode.getName(), () -> configChild.getOrder());
		}
		log.trace("Removing duplicate config children of FrankElement [{}]", () -> parent.getFullName());
		result = ConfigChild.removeDuplicates(result);
		Collections.sort(result);
		log.trace("Sorted config children are:");
		if(log.isTraceEnabled()) {
			result.forEach(c -> log.trace("{}", c.toString()));
		}
		log.trace("Done creating config children of FrankElement [{}]", () -> parent.getFullName());
		return result;
	}

	private List<ConfigChild.SortNode> createSortNodes(FrankMethod[] methods, FrankElement parent) {
		List<FrankMethod> configChildSetters = Arrays.asList(methods).stream()
				.filter(FrankMethod::isPublic)
				.filter(Utils::isConfigChildSetter)
				.filter(m -> configChildDescriptors.get(m.getName()) != null)
				.collect(Collectors.toList());
		List<ConfigChild.SortNode> sortNodes = new ArrayList<>();
		for(FrankMethod setter: configChildSetters) {
			ConfigChild.SortNode sortNode = new ConfigChild.SortNode(setter);
			sortNodes.add(sortNode);
		}
		Collections.sort(sortNodes);
		return sortNodes;
	}

	ElementRole findOrCreateElementRole(FrankClass elementTypeClass, String roleName) throws FrankDocException {
		log.trace("ElementRole requested for elementTypeClass [{}] and roleName [{}]. Going to get the ElementType", () -> elementTypeClass.getName(), () -> roleName);
		ElementType elementType = findOrCreateElementType(elementTypeClass);
		ElementRole.Key key = new ElementRole.Key(elementTypeClass.getName(), roleName);
		if(allElementRoles.containsKey(key)) {
			log.trace("ElementRole already present");
			ElementRole result = allElementRoles.get(key);
			return result;
		} else {
			ElementRole result = elementRoleFactory.create(elementType, roleName);
			allElementRoles.put(key, result);
			log.trace("For ElementType [{}] and roleName [{}], created ElementRole [{}]", () -> elementType.getFullName(), () -> roleName, () -> result.createXsdElementName(""));
			return result;
		}
	}

	public ElementRole findElementRole(ElementRole.Key key) {
		return allElementRoles.get(key);
	}

	public ElementRole findElementRole(ConfigChild configChild) {
		return findElementRole(new ElementRole.Key(configChild));
	}

	ElementRole findElementRole(String fullElementTypeName, String roleName) {
		return allElementRoles.get(new ElementRole.Key(fullElementTypeName, roleName));
	}

	ElementType findOrCreateElementType(FrankClass clazz) throws FrankDocException {
		log.trace("Requested ElementType for class [{}]", () -> clazz.getName());
		if(allTypes.containsKey(clazz.getName())) {
			log.trace("Already present");
			return allTypes.get(clazz.getName());
		}
		final ElementType result = new ElementType(clazz);
		// If a containing FrankElement contains the type being created, we do not
		// want recursion.
		allTypes.put(result.getFullName(), result);
		if(result.isFromJavaInterface()) {
			log.trace("Class [{}] is a Java interface, going to create all member FrankElement", () -> clazz.getName());
			List<FrankClass> memberClasses = clazz.getInterfaceImplementations();
			// We sort here to make the order deterministic.
			Collections.sort(memberClasses, Comparator.comparing(FrankClass::getName));
			for(FrankClass memberClass: memberClasses) {
				FrankElement frankElement = findOrCreateFrankElement(memberClass.getName());
				result.addMember(frankElement);
			}
		} else {
			log.trace("Class [{}] is not a Java interface, creating its FrankElement", () -> clazz.getName());
			result.addMember(findOrCreateFrankElement(clazz.getName()));
		}
		log.trace("Done creating ElementType for class [{}]", () -> clazz.getName());
		return result;
	}

	public ElementType findElementType(String fullName) {
		return allTypes.get(fullName);
	}

	void calculateHighestCommonInterfaces() {
		log.trace("Going to calculate highest common interface for every ElementType");
		allTypes.values().forEach(et -> et.calculateHighestCommonInterface(this));
		log.trace("Done calculating highest common interface for every ElementType");
	}

	void buildGroups() {
		log.trace("Building groups");
		Map<String, List<FrankDocGroup>> groupsBase = new HashMap<>();
		List<FrankElement> membersOfOther = new ArrayList<>();
		for(ElementType elementType: getAllTypes().values()) {
			if(elementType.isFromJavaInterface()) {
				FrankDocGroup interfaceBasedGroup = FrankDocGroup.getInstanceFromElementType(elementType);
				elementType.setFrankDocGroup(interfaceBasedGroup);
				String groupName = elementType.getGroupName();
				if(groupsBase.containsKey(groupName)) {
					groupsBase.get(groupName).add(interfaceBasedGroup);
				} else {
					groupsBase.put(groupName, Arrays.asList(interfaceBasedGroup));
				}
				log.trace("Appended group [{}] with candidate element type [{}], which is based on a Java interface", () -> elementType.getSimpleName(), () -> elementType.getFullName());
			}
			else {
				try {
					membersOfOther.add(elementType.getSingletonElement());
					// Cannot eliminate the isTraceEnabled, because Lambdas dont work here.
					// getSingletonElement throws an exception.
					if(log.isTraceEnabled()) {
						log.trace("Appended the others group with FrankElement [{}]", elementType.getSingletonElement().getFullName());
					}
				} catch(ReflectiveOperationException e) {
					String frankElementsString = elementType.getMembers().stream()
							.map(FrankElement::getSimpleName).collect(Collectors.joining(", "));
					log.warn("Error adding ElementType [{}] to group other because it has multiple FrankElement objects: [{}]",
								() -> elementType.getFullName(), () -> frankElementsString, () -> e);
				}
			}
		}
		if(groupsBase.containsKey(OTHER)) {
			log.warn("Name \"[{}]\" cannot been used for others group because it is the name of an ElementType", OTHER);
		}
		else {
			final FrankDocGroup groupOther = FrankDocGroup.getInstanceFromFrankElements(OTHER, membersOfOther);
			allTypes.values().stream()
				.filter(et -> ! et.isFromJavaInterface())
				.forEach(et -> et.setFrankDocGroup(groupOther));
			groupsBase.put(OTHER, Arrays.asList(groupOther));
		}
		for(String groupName: groupsBase.keySet()) {
			if(groupsBase.get(groupName).size() != 1) {
				log.warn("Group name [{}] used for multiple groups", groupName);
			}
		}
		// Sort the groups alphabetically, including group "Other". We have to update
		// this code if "Other" needs to be put to the end.
		groups = new LinkedHashMap<>();
		List<String> sortedGroups = new ArrayList<>(groupsBase.keySet());
		Collections.sort(sortedGroups);
		for(String groupName: sortedGroups) {
			log.trace("Creating group [{}]", groupName);
			groups.put(groupName, groupsBase.get(groupName).get(0));
		}
		log.trace("Done building groups");
	}

	void setOverriddenFrom() {
		log.trace("Going to set property overriddenFrom for all config children and all attributes of all FrankElement");
		Set<String> remainingElements = allElements.values().stream().map(FrankElement::getFullName).collect(Collectors.toSet());
		while(! remainingElements.isEmpty()) {
			FrankElement current = allElements.get(remainingElements.iterator().next());
			while((current.getParent() != null) && (remainingElements.contains(current.getParent().getFullName()))) {
				current = current.getParent();
			}
			// Cannot eliminate isTraceEnabled here. Then the operation on variable current would need a lambda,
			// but that is not possible because variable current is not final.
			if(log.isTraceEnabled()) {
				log.trace("Seting property overriddenFrom for all config children and all attributes of FrankElement [{}]", current.getFullName());
			}
			current.getConfigChildren(ALL).forEach(c -> c.calculateOverriddenFrom());
			current.getAttributes(ALL).forEach(c -> c.calculateOverriddenFrom());
			current.getStatistics().finish();
			if(log.isTraceEnabled()) {
				log.trace("Done seting property overriddenFrom for FrankElement [{}]", current.getFullName());
			}
			remainingElements.remove(current.getFullName());
		}
		log.trace("Done setting property overriddenFrom");
	}

	void setElementNamesOfFrankElements(String rootClassName) {
		FrankElement root = allElements.get(rootClassName);
		root.addXmlElementName(root.getSimpleName());
		for(ElementRole role: allElementRoles.values()) {
			role.getMembers().forEach(frankElement -> frankElement.addXmlElementName(frankElement.getXsdElementName(role)));
		}
	}

	void setHighestCommonInterface() {
		log.trace("Doing FrankDocModel.setHighestCommonInterface");
		for(ElementRole role: allElementRoles.values()) {
			String roleName = role.getRoleName();
			ElementType et = role.getElementType().getHighestCommonInterface();
			ElementRole result = findElementRole(new ElementRole.Key(et.getFullName(), roleName));
			if(result == null) {
				log.warn("Promoting ElementRole [{}] results in ElementType [{}] and role name {}], but there is no corresponding ElementRole",
						() -> toString(), () -> et.getFullName(), () -> roleName);
				role.setHighestCommonInterface(role);
			} else {
				role.setHighestCommonInterface(result);
				log.trace("Role [{}] has highest common interface [{}]", () -> role.toString(), () -> result.toString());
			}
		}
		log.trace("Done FrankDocModel.setHighestCommonInterface");
	}

	void createConfigChildSets() {
		log.trace("Doing FrankDocModel.createConfigChildSets");
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
		log.trace("Done FrankDocModel.createConfigChildSets");
	}

	private void createConfigChildSets(FrankElement frankElement) {
		log.trace("Handling FrankElement [{}]", () -> frankElement.getFullName());
		Map<String, List<ConfigChild>> cumChildrenByRoleName = frankElement.getCumulativeConfigChildren(ElementChild.ALL, ElementChild.NONE).stream()
				.collect(Collectors.groupingBy(c -> c.getElementRole().getRoleName()));
		for(String roleName: cumChildrenByRoleName.keySet()) {
			List<ConfigChild> configChildren = cumChildrenByRoleName.get(roleName);
			if(configChildren.stream().map(ConfigChild::getOwningElement).anyMatch(childOwner -> (childOwner == frankElement))) {
				log.trace("Found ConfigChildSet for syntax 1 name [{}]", roleName);
				ConfigChildSet configChildSet = new ConfigChildSet(configChildren);
				frankElement.addConfigChildSet(configChildSet);
				ElementRoleSet elementRoleSet = findOrCreateElementRoleSet(configChildSet);
				log.trace("The config child with syntax 1 name [{}] has ElementRoleSet [{}]", () -> roleName, () -> elementRoleSet.toString());
				configChildSet.setElementRoleSet(elementRoleSet);
			}
		}
		log.trace("Done handling FrankElement [{}]", () -> frankElement.getFullName());
	}

	private ElementRoleSet findOrCreateElementRoleSet(ConfigChildSet configChildSet) {
		Set<ElementRole> roles = configChildSet.getConfigChildren().stream()
				.map(ConfigChild::getElementRole)
				.collect(Collectors.toSet());
		Set<ElementRole.Key> key = roles.stream()
				.map(ElementRole::getKey)
				.collect(Collectors.toSet());
		if(! allElementRoleSets.containsKey(key)) {
			log.trace("New ElementRoleSet for roles [{}]", () -> ElementRole.describeCollection(roles));
			allElementRoleSets.put(key, new ElementRoleSet(roles));
		}
		return allElementRoleSets.get(key);
	}

	/**
	 * Create {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}, taking
	 * care of generic element option recursion as explained in
	 * {@link nl.nn.adapterframework.frankdoc.model}.
	 */
	private void recursivelyCreateElementRoleSets(List<ElementRole> roleGroup, int recursionDepth) {
		log.trace("Enter with roles [{}] and recursion depth [{}]", () -> ElementRole.describeCollection(roleGroup), () -> recursionDepth);
		List<FrankElement> rawMembers = roleGroup.stream()
				.flatMap(role -> role.getRawMembers().stream())
				.distinct()
				.collect(Collectors.toList());
		Map<String, List<ConfigChild>> configChildrenByRoleName = rawMembers.stream()
				.flatMap(element -> element.getConfigChildren(ElementChild.ALL).stream())
				.collect(Collectors.groupingBy(ConfigChild::getRoleName));
		List<String> names = new ArrayList<>(configChildrenByRoleName.keySet());
		Collections.sort(names);
		for(String name: names) {
			List<ConfigChild> configChildren = configChildrenByRoleName.get(name);
			Set<ElementRole> roles = configChildren.stream().map(ConfigChild::getElementRole).collect(Collectors.toSet());
			Set<ElementRole.Key> key = roles.stream().map(ElementRole::getKey).collect(Collectors.toSet());
			if(! allElementRoleSets.containsKey(key)) {
				allElementRoleSets.put(key, new ElementRoleSet(roles));
				log.trace("Added new ElementRoleSet [{}]", () -> allElementRoleSets.get(key).toString());
				List<ElementRole> recursionParents = new ArrayList<>(roles);
				recursionParents = recursionParents.stream().collect(Collectors.toList());
				Collections.sort(recursionParents);
				recursivelyCreateElementRoleSets(recursionParents, recursionDepth + 1);
			}
		}
		log.trace("Leave for roles [{}] and recursion depth [{}]", () -> ElementRole.describeCollection(roleGroup), () -> recursionDepth);
	}

	AttributeValues findOrCreateAttributeValues(FrankClass clazz) {
		return attributeValuesFactory.findOrCreateAttributeValues(clazz);
	}

	public AttributeValues findAttributeValues(String enumTypeFullName) {
		return attributeValuesFactory.findAttributeValues(enumTypeFullName);
	}

	public List<AttributeValues> getAllAttributeValuesInstances() {
		return attributeValuesFactory.getAll();
	}
}