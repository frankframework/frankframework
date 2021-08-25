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

	private FrankClassRepository classRepository;

	private @Getter Map<String, ConfigChildSetterDescriptor> configChildDescriptors = new HashMap<>();
	
	private FrankDocGroupFactory groupFactory = new FrankDocGroupFactory();
	private @Getter List<FrankDocGroup> groups;
	
	// We want to iterate FrankElement in the order they are created, to be able
	// to create the ElementRole objects in the right order. 
	private @Getter Map<String, FrankElement> allElements = new LinkedHashMap<>();

	// We have a LinkedHashMap because the sequence of the types is relevant. This
	// sequence determines the sort order of the elements of FrankDocGroup Other.
	private @Getter Map<String, ElementType> allTypes = new LinkedHashMap<>();

	private @Getter List<FrankElement> elementsOutsideConfigChildren; 

	private @Getter Map<ElementRole.Key, ElementRole> allElementRoles = new HashMap<>();
	private final ElementRole.Factory elementRoleFactory = new ElementRole.Factory();
	private Map<Set<ElementRole.Key>, ElementRoleSet> allElementRoleSets = new HashMap<>();
	private AttributeEnumFactory attributeEnumFactory = new AttributeEnumFactory();

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
			result.calculateInterfaceBased();
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
		log.trace("Creating config child descriptors from file [{}]", () -> path);
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
			String registerTextMethod = rule.getRegisterTextMethod();
			if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				if(StringUtils.isNotEmpty(registerTextMethod)) {
					log.warn("digester-rules.xml, role name {}: Have both registerMethod and registerTextMethod, ignoring the latter", roleName);
				}
				addTypeObject(rule.getRegisterMethod(), roleName);
			} else {
				if(StringUtils.isNotEmpty(registerTextMethod)) {
					if(registerTextMethod.startsWith("set")) {
						log.warn("digester-rules.xml: Ignoring registerTextMethod {} because it starts with \"set\" to avoid confusion with attributes", registerTextMethod);
					} else {
						addTypeText(registerTextMethod, roleName);
					}
				} else {
					// roleName is not final, so a lambda wont work in the trace statement.
					// We use isTraceEnabled() instead.
					if(log.isTraceEnabled()) {
						log.trace("digester-rules.xml, ignoring role name {} because there is no registerMethod and no registerTextMethod attribute", roleName);
					}
				}
			}
		}

		private void addTypeObject(String registerMethod, String roleName) throws SAXException {
			log.trace("Have ConfigChildSetterDescriptor for ObjectConfigChild: roleName = {}, registerMethod = {}", () -> roleName, () -> registerMethod);
			ConfigChildSetterDescriptor descriptor = new ConfigChildSetterDescriptor.ForObject(registerMethod, roleName);
			checkDuplicate(descriptor);
		}

		private void addTypeText(String registerMethod, String roleName) throws SAXException {
			log.trace("Have ConfigChildSetterDescriptor for TextConfigChild: roleName = {}, registerMethod = {}", () -> roleName, () -> registerMethod);
			ConfigChildSetterDescriptor descriptor = new ConfigChildSetterDescriptor.ForText(registerMethod, roleName);
			checkDuplicate(descriptor);
		}

		private void checkDuplicate(ConfigChildSetterDescriptor descriptor) {
			if(configChildDescriptors.containsKey(descriptor.getMethodName())) {
				log.warn("In digester rules [{}], duplicate method name [{}], ignoring", path, descriptor.getMethodName());
			} else {
				configChildDescriptors.put(descriptor.getMethodName(), descriptor);
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
		AttributeExcludedSetter attributeExcludedSetter = new AttributeExcludedSetter(clazz);
		FrankMethod[] methods = clazz.getDeclaredMethods();
		Map<String, FrankMethod> enumGettersByAttributeName = getEnumGettersByAttributeName(clazz);
		LinkedHashMap<String, FrankMethod> setterAttributes = getAttributeToMethodMap(methods, "set");
		Map<String, FrankMethod> getterAttributes = getGetterAndIsserAttributes(methods, attributeOwner);
		List<FrankAttribute> result = new ArrayList<>();
		for(Entry<String, FrankMethod> entry: setterAttributes.entrySet()) {
			String attributeName = entry.getKey();
			log.trace("Attribute [{}]", () -> attributeName);
			FrankMethod method = entry.getValue();
			if(getterAttributes.containsKey(attributeName)) {
				checkForTypeConflict(method, getterAttributes.get(attributeName), attributeOwner);
			}
			FrankAttribute attribute = new FrankAttribute(attributeName, attributeOwner);
			attribute.setAttributeType(AttributeType.fromJavaType(method.getParameterTypes()[0].getName()));
			log.trace("Attribute {} has type {}", () -> attributeName, () -> attribute.getAttributeType().toString());
			documentAttribute(attribute, method, attributeOwner);
			log.trace("Default [{}]", () -> attribute.getDefaultValue());
			if(enumGettersByAttributeName.containsKey(attributeName)) {
				log.trace("Attribute {} has enum values", () -> attributeName);
				attribute.setAttributeEnum(findOrCreateAttributeEnum((FrankClass) enumGettersByAttributeName.get(attributeName).getReturnType()));
			}
			try {
				// Method FrankAttribute.typeCheckDefaultValue() does not write the warning
				// but only throws an exception. This allows us to test that method
				// discovers type mismatches.
				attribute.typeCheckDefaultValue();
			} catch(FrankDocException e) {
				log.warn("Attribute [{}] has an invalid default value, [{}, detail {}]", attribute.toString(), attribute.getDefaultValue(), e.getMessage());
			}
			attributeExcludedSetter.updateAttribute(attribute, method);
			result.add(attribute);
			log.trace("Attribute [{}] done", () -> attributeName);
		}
		// We may inherit attribute setters from an interface from which we have to reject the attributes.
		// We must have FrankAttribute instances for these, because otherwise AncestorChildNavigation does not know
		// how to omit them.
		result.addAll(attributeExcludedSetter.getExcludedAttributesForRemainingNames(attributeOwner));
		log.trace("Done creating attributes for {}", attributeOwner.getFullName());
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
	static LinkedHashMap<String, FrankMethod> getAttributeToMethodMap(FrankMethod[] methods, String prefix) {
		List<FrankMethod> methodList = Arrays.asList(methods);
		methodList = methodList.stream()
				.filter(FrankMethod::isPublic)
				.filter(Utils::isAttributeGetterOrSetter)
				.filter(m -> m.getName().startsWith(prefix) && (m.getName().length() > prefix.length()))
				.collect(Collectors.toList());
		LinkedHashMap<String, FrankMethod> result = new LinkedHashMap<>();
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
				|| (method.getAnnotation(FrankDocletConstants.IBISDOCREF) != null)
				|| (method.getJavaDoc() != null)
				|| (method.getJavaDocTag(ElementChild.JAVADOC_DEFAULT_VALUE_TAG) != null));
		log.trace("Attribute: deprecated = [{}], documented = [{}]", () -> attribute.isDeprecated(), () -> attribute.isDocumented());
		attribute.setJavaDocBasedDescriptionAndDefault(method);
		FrankAnnotation ibisDocRef = method.getAnnotationInludingInherited(FrankDocletConstants.IBISDOCREF);
		if(ibisDocRef != null) {
			log.trace("Found @IbisDocRef annotation");
			ParsedIbisDocRef parsed = parseIbisDocRef(ibisDocRef, method);
			FrankAnnotation ibisDoc = null;
			if((parsed != null) && (parsed.getReferredMethod() != null)) {
				attribute.setJavaDocBasedDescriptionAndDefault(parsed.getReferredMethod());
				ibisDoc = parsed.getReferredMethod().getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
				if(ibisDoc != null) {
					attribute.setDescribingElement(findOrCreateFrankElement(parsed.getReferredMethod().getDeclaringClass().getName()));
					log.trace("Describing element of attribute [{}].[{}] is [{}]",
							() -> attributeOwner.getFullName(), () -> attribute.getName(), () -> attribute.getDescribingElement().getFullName());
					attribute.parseIbisDocAnnotation(ibisDoc);
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
		List<FrankMethod> frankMethods = Arrays.asList(methods).stream()
				.filter(FrankMethod::isPublic)
				.filter(Utils::isConfigChildSetter)
				.filter(m -> configChildDescriptors.get(m.getName()) != null)
				.collect(Collectors.toList());
		for(int order = 0; order < frankMethods.size(); ++order) {
			FrankMethod frankMethod = frankMethods.get(order);
			log.trace("Have config child setter [{}]", () -> frankMethod.getName());
			ConfigChildSetterDescriptor configChildDescriptor = configChildDescriptors.get(frankMethod.getName());
			log.trace("Have ConfigChildSetterDescriptor [{}]", () -> configChildDescriptor.toString());
			ConfigChild configChild = configChildDescriptor.createConfigChild(parent, frankMethod);
			configChild.setAllowMultiple(configChildDescriptor.isAllowMultiple());
			configChild.setMandatory(configChildDescriptor.isMandatory());
			if(configChildDescriptor.isForObject()) {
				log.trace("For FrankElement [{}] method [{}], going to search element role", () -> parent.getFullName(), () -> frankMethod.getName());
				FrankClass elementTypeClass = (FrankClass) frankMethod.getParameterTypes()[0];
				((ObjectConfigChild) configChild).setElementRole(findOrCreateElementRole(elementTypeClass, configChildDescriptor.getRoleName()));
				log.trace("For FrankElement [{}] method [{}], have the element role", () -> parent.getFullName(), () -> frankMethod.getName());
			}
			configChild.setOrder(order);
			result.add(configChild);
			log.trace("Done creating config child {}, the order is {}", () -> configChild.toString(), () -> configChild.getOrder());
		}
		log.trace("Removing duplicate config children of FrankElement [{}]", () -> parent.getFullName());
		result = ConfigChild.removeDuplicates(result);
		log.trace("The config children are (sequence follows sequence of Java methods):");
		if(log.isTraceEnabled()) {
			result.forEach(c -> log.trace("{}", c.toString()));
		}
		log.trace("Done creating config children of FrankElement [{}]", () -> parent.getFullName());
		return result;
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

	public ElementRole findElementRole(ObjectConfigChild configChild) {
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
		FrankDocGroup group = groupFactory.getGroup(clazz);
		final ElementType result = new ElementType(clazz, group);
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
		Map<String, List<ConfigChild>> cumChildrenByRoleName = frankElement.getCumulativeConfigChildren(ElementChild.ALL_NOT_EXCLUDED, ElementChild.EXCLUDED).stream()
				.collect(Collectors.groupingBy(c -> c.getRoleName()));
		for(String roleName: cumChildrenByRoleName.keySet()) {
			List<ConfigChild> configChildren = cumChildrenByRoleName.get(roleName);
			if(configChildren.stream().map(ConfigChild::getOwningElement).anyMatch(childOwner -> (childOwner == frankElement))) {
				log.trace("Found ConfigChildSet for role name [{}]", roleName);
				ConfigChildSet configChildSet = new ConfigChildSet(configChildren);
				frankElement.addConfigChildSet(configChildSet);
				createElementRoleSetIfApplicable(configChildSet);
			}
		}
		log.trace("Done handling FrankElement [{}]", () -> frankElement.getFullName());
	}

	private void createElementRoleSetIfApplicable(ConfigChildSet configChildSet) {
		switch(configChildSet.getConfigChildGroupKind()) {
		case TEXT:
			log.trace("[{}] holds only TextConfigChild. No ElementRoleSet needed", () -> configChildSet.toString());
			break;
		case MIXED:
			log.warn("[{}] combines ObjectConfigChild and TextConfigChild, which is not supported", configChildSet.toString());
			break;
		case OBJECT:
			createElementRoleSet(configChildSet);
			break;
		default:
			throw new IllegalArgumentException("Cannot happen, switch should cover all enum values");
		}
	}

	void createElementRoleSet(ConfigChildSet configChildSet) {
		Set<ElementRole> roles = configChildSet.getElementRoleStream()
				.collect(Collectors.toSet());
		Set<ElementRole.Key> key = roles.stream()
				.map(ElementRole::getKey)
				.collect(Collectors.toSet());
		if(! allElementRoleSets.containsKey(key)) {
			log.trace("New ElementRoleSet for roles [{}]", () -> ElementRole.describeCollection(roles));
			allElementRoleSets.put(key, new ElementRoleSet(roles));
		}
		ElementRoleSet elementRoleSet = allElementRoleSets.get(key);
		log.trace("[{}] has ElementRoleSet [{}]", () -> configChildSet.toString(), () -> elementRoleSet.toString());		
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
				.flatMap(element -> element.getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).stream())
				.collect(Collectors.groupingBy(ConfigChild::getRoleName));
		List<String> names = new ArrayList<>(configChildrenByRoleName.keySet());
		Collections.sort(names);
		for(String name: names) {
			List<ConfigChild> configChildren = configChildrenByRoleName.get(name);
			handleMemberChildrenWithCommonRoleName(configChildren, recursionDepth);
		}
		log.trace("Leave for roles [{}] and recursion depth [{}]", () -> ElementRole.describeCollection(roleGroup), () -> recursionDepth);
	}

	void handleMemberChildrenWithCommonRoleName(List<ConfigChild> configChildren, int recursionDepth) {
		log.trace("Considering config children [{}]", () -> ConfigChild.toString(configChildren));
		switch(ConfigChildGroupKind.groupKind(configChildren)) {
		case TEXT:
			log.trace("No ElementRoleSet needed for combination of TextConfigChild [{}]", () -> ConfigChild.toString(configChildren));
			break;
		case MIXED:
			log.warn("Browsing member children produced a combination of ObjectConfigChild and TextConfigChild [{}], which is not supported", ConfigChild.toString(configChildren));
			break;
		case OBJECT:
			findOrCreateElementRoleSetForMemberChildren(configChildren, recursionDepth);
			break;
		default:
			throw new IllegalArgumentException("Should not happen, because switch statement should cover all enum values");
		}
	}

	void findOrCreateElementRoleSetForMemberChildren(List<ConfigChild> configChildren, int recursionDepth) {
		Set<ElementRole> roles = ConfigChild.getElementRoleStream(configChildren).collect(Collectors.toSet());
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

	AttributeEnum findOrCreateAttributeEnum(FrankClass clazz) {
		return attributeEnumFactory.findOrCreateAttributeEnum(clazz);
    }

	public AttributeEnum findAttributeEnum(String enumTypeFullName) {
		return attributeEnumFactory.findAttributeEnum(enumTypeFullName);
	}

	public List<AttributeEnum> getAllAttributeEnumInstances() {
		return attributeEnumFactory.getAll();
	}

	public void calculateInterfaceBased() {
		allTypes.values().stream().filter(ElementType::isFromJavaInterface)
			.flatMap(et -> et.getMembers().stream())
			.forEach(f -> f.setInterfaceBased(true));
	}

	public void buildGroups() {
		Map<String, List<ElementType>> groupsElementTypes = allTypes.values().stream()
				.collect(Collectors.groupingBy(f -> f.getGroup().getName()));
		groups = groupFactory.getAllGroups();
		for(FrankDocGroup group: groups) {
			List<ElementType> elementTypes = new ArrayList<>(groupsElementTypes.get(group.getName()));
			Collections.sort(elementTypes);
			group.setElementTypes(elementTypes);
		}
		final Map<String, FrankElement> leftOvers = new HashMap<>(allElements);
		allTypes.values().stream().flatMap(et -> et.getSyntax2Members().stream()).forEach(f -> leftOvers.remove(f.getFullName()));
		elementsOutsideConfigChildren = leftOvers.values().stream()
				.filter(f -> ! f.isAbstract())
				.filter(f -> ! f.getXmlElementNames().isEmpty())
				.sorted()
				.collect(Collectors.toList());
	}
}