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

package nl.nn.adapterframework.doc;

import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAnyAttribute;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttribute;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addChoice;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addComplexContent;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addDocumentation;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElementRef;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElementWithType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addExtension;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addSequence;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.createAttributeGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.createComplexType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.createElementWithType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.createGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.getXmlSchema;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.AttributeUse.OPTIONAL;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.AttributeUse.PROHIBITED;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.AttributeUse.REQUIRED;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.AttributeValueStatus.DEFAULT;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.AttributeValueStatus.FIXED;
import static nl.nn.adapterframework.doc.model.ElementChild.DEPRECATED;
import static nl.nn.adapterframework.doc.model.ElementChild.IN_XSD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * This class writes the XML Schema document (XSD) that checks the validity of a
 * Frank configuration XML file. The XML Schema is written based on the information
 * in a {@link FrankDocModel} object (the model).
 * 
 * <h1>The syntax 2 name</h1>
 *
 * Below, a few implementation details are explained. First, the integration specialist
 * references an element by a name the reveals both the requested Java class
 * (expressed as a {@link FrankElement} in the model)
 * and the role it plays (e.g. sender or error sender). These requirements are
 * implemented by model method {@link FrankElement#getXsdElementName}. This
 * method takes as an argument the relevant {@link ElementType} to which the {@link FrankElement}
 * of the Java class belongs. The link between this {@link ElementType} and the
 * containing &lt;xs:element&gt; is made through a {@link ConfigChild}.
 * The <code>syntax1Name</code> attribute of this {@link ConfigChild}
 * is the other argument required by {@link FrankElement#getXsdElementName}.
 * <p>
 * Each element within the Frank!Framework appears as an &lt;complexType&gt;
 * under the XSD root. It is not duplicated for the different roles it
 * can play. This complex type references a group of config children and a group
 * of attributes. A group of config children consists of &lt;xs:choice&gt; elements that
 * each list the allowed elements available to the integration specialist, element groups.
 * An option within an element group appears as an &lt;xs:element&gt; that has
 * the syntax 2 name as name and the XSD type of the referenced {@link FrankElement} as type.
 * <p>
 * There is a different element group for each combination of an {@link ElementType} and
 * config child syntax 1 name, so there can be multiple element groups per {@link ElementType}. This is
 * the only duplication we need because of syntax 2 names. A {@link FrankElement} is
 * represented in the XSD with a top-level &lt;xs:complexType&gt; item. That item references
 * attribute groups and XSD sequences for config children, but these are not duplicated
 * because of the syntax 2 name issue.
 *
 * <h1>Inheritance of attributes and config children</h1>
 * 
 * Each {@link FrankElement} is represented by a top-level &lt;xs:complexType&gt;.
 * Each Frank!Framework element can have attributes or other elements. 
 * These correspond to {@link FrankAttribute}
 * objects or {@link ConfigChild} objects in the model.
 * <p>
 * In the model, a {@link FrankElement} only holds its declared attributes, but
 * the top-level &lt;xs:complexType&gt; should allow both the declared attributes and the attributes
 * inherited from the ancestors of the {@link FrankElement} (the inherited attributes).
 * The same holds for configuration children. This similarity appears in the model
 * through the common base class {@link ElementChild}, which is a parent class of both
 * {@link FrankAttribute} and {@link ConfigChild}. An attribute defined high in the
 * class hierarchy of the Frank!Framework can be allowed for many FF! elements,
 * but we do not want to repeat the same &lt;xs:attribute&gt; tags in all these cases.
 * We solve this by grouping the attributes, and the config children, in the XSD, for example:
 * 
 * <pre>
 * {@code
<xs:complexType name="ConfigurationType">
  <xs:group ref="ConfigurationDeclaredChildGroup" />
  <xs:attributeGroup ref="ConfigurationDeclaredAttributeGroup" />
</xs:complexType>
}
 * </pre>
 * <p>
 * The example shows a group named <code>ConfigurationDeclaredChildGroup</code>. This group
 * declares all allowed child FF! elements.
 * <p>
 * An XSD group ending with "DeclaredChildGroup" only holds the <em>declared</em> configuration children
 * or attributes. This is sufficient for Frank config element &lt;Configuration&gt; because the corresponding
 * Java class has only <code>Object</code> as parent. We also use cumulative groups
 * that allow the declared items (attributes / config children) of a {@link FrankElement}
 * as well as the inherited items. The following example in the XSD illustrates this:
 * <pre>
 {@code
<xs:attributeGroup name="LockerCumulativeAttributeGroup">
  <xs:attributeGroup ref="LockerDeclaredAttributeGroup" />
  <xs:attributeGroup ref="JdbcFacadeCumulativeAttributeGroup" />
</xs:attributeGroup>
}
 * </pre>
 * The Frank!Framework has a Java class named <code>Locker</code> that has class
 * <code>JdbcFacade</code> as its parent. The group <code>LockerCumulativeAttributeGroup</code>
 * is defined recursively: all declared attributes of <code>Locker</code> are in, and all
 * declared and inherited attributes of the parent class <code>JdbcFacade</code>. The
 * recursion stops with the ancestor that holds the last inherited attributes, because
 * for that ancestor we do not introduce a cumulative group and use the declared group
 * only.
 * <p>
 * Another issue about groups needs explanation. Some Java classes of the Frank!Framework override
 * attributes that become then duplicate in the model. They appear as declared attributes
 * in two {@link FrankElement} objects, one modeling the Java subclass and one modeling the
 * Java ancestor class. In this situation, only the attribute (or config child) corresponding
 * to the Java subclass is needed. The attribute
 * of the ancestor class is omitted. The following example illustrates this:
 * <pre>
 {@code
<xs:attributeGroup name="SoapValidatorCumulativeAttributeGroup">
  <xs:attributeGroup ref="SoapValidatorDeclaredAttributeGroup" />
  <xs:attributeGroup ref="Json2XmlValidatorDeclaredAttributeGroup" />
  <xs:attribute name="ignoreUnknownNamespaces" type="xs:string" />
  ...
  <xs:attributeGroup ref="FixedForwardPipeCumulativeAttributeGroup" />
</xs:attributeGroup>
 }
 * </pre>
 * Java class <code>SoapValidator</code> overrides a method <code>setRoot()</code> from
 * the grand-parent class <code>XmlValidator</code>. If the cumulative group of
 * the parent class <code>Json2XmlValidatorCumulativeAttributeGroup</code> would be referenced,
 * we would have attribute "<code>root</code>" twice. To avoid this, only the declared group
 * <code>Json2XmlValidatorDeclaredAttributeGroup</code> is referenced and the non-duplicate
 * attributes of <code>XmlValidator</code> are repeated. Higher up the dependency
 * hierarchy, there are no duplicate attributes. Therefore, the list of attributes
 * can end with referencing group <code>FixedForwardPipeCumulativeAttributeGroup</code>.
 * <p>
 * Please note that <code>SoapValidator</code> has a deprecated method <code>setSchema()</code>
 * that it overrdes from <code>XmlValidator</code>. The algorithm takes care to not only omit
 * attribute <code>schema</code> as a declared attribute,
 * but also as an inherited attributre of <code>SoapValidator</code>. Other descendants
 * of <code>XmlValidator</code> are not influenced by the override by <code>SoapValidator</code>.
 * This part of the algorithm is handled by package-private class
 * <code>nl.nn.adapterframework.doc.model.ChildRejector</code>.
 * <p>
 * Finally, 'technical' overrides are ignored by this algorithm, which are
 * setters with an override annotation that are not deprecated and lack
 * IbisDoc or IbisDocRef annotations.
 * 
 * <h1> The options for a config child</h1>
 *
 * The {@link ConfigChild} class in the model determines what &lt;xs:element&gt; are allowed
 * as children of another &lt;xs:element&gt;. The containing &lt;xs:element&gt; is the
 * <code>owningElement</code> field, which is of type {@link FrankElement}. A {@link ConfigChild} is
 * characterized by the combination of an owning element and an {@link ElementType}. As an example
 * consider the {@link ConfigChild} that exists for the combination of owning element
 * "<code>Receiver</code>" and element type "<code>IListener</code>". It produces
 * the following XML schema:
 * <pre>
 * {@code
<xs:group ref="IListenerListenerElementGroup" minOccurs="0" maxOccurs="1" />
}
 * </pre>
 * This snippet appears within <code>&lt;xs:group name="ReceiverDeclaredChildGroup"&gt;&lt;xs:sequence&gt;</code>.
 * The snippet states that a Receiver can contain all elements related to a Java class implementing
 * {@link IListener}, playing the role of a listener. The duplication because of syntax 2 names becomes
 * visible here.
 *
 * @author martijn
 *
 */
public class DocWriterNew {
	private static Logger log = LogUtil.getLogger(DocWriterNew.class);

	private static final String CONFIGURATION = "nl.nn.adapterframework.configuration.Configuration";
	private static final String ELEMENT_GROUP = "ElementGroup";
	private static final String ELEMENT_ROLE = "elementRole";
	static final String MEMBER_CHILD_GROUP = "MemberChildGroup";
	
	private FrankDocModel model;
	private String startClassName;
	private List<XmlBuilder> xsdElements = new ArrayList<>();
	private List<XmlBuilder> xsdComplexItems = new ArrayList<>();
	private Set<String> namesCreatedFrankElements = new HashSet<>();
	private Set<ElementRole.Key> idsCreatedElementGroups = new HashSet<>();
	private Set<String> namesElementTypesWithChildMemberGroup = new HashSet<>();

	public DocWriterNew(FrankDocModel model) {
		this.model = model;
	}

	public void init() {
		init(CONFIGURATION);
	}

	public void init(String startClassName) {
		this.startClassName = startClassName;
		if(log.isTraceEnabled()) {
			log.trace(String.format("Initialized DocWriterNew with start element name [%s]", startClassName));
		}
	}

	public String getSchema() {
		XmlBuilder xsdRoot = getXmlSchema();
		if(log.isTraceEnabled()) {
			log.trace("Going to create XmlBuilder objects that will be added to the schema root builder afterwards");
		}
		FrankElement startElement = model.findFrankElement(startClassName);
		recursivelyDefineXsdElementOfRoot(startElement);
		if(log.isTraceEnabled()) {
			log.trace("Have the XmlBuilder objects. Going to add them in the right order to the schema root builder");
		}
		xsdElements.forEach(xsdRoot::addSubElement);
		xsdComplexItems.forEach(xsdRoot::addSubElement);
		if(log.isTraceEnabled()) {
			log.trace("Populating schema root builder is done. Going to create the XML string to return");
		}
		return xsdRoot.toXML(true);
	}

	private void recursivelyDefineXsdElementOfRoot(FrankElement frankElement) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Enter top FrankElement [%s]", frankElement.getFullName()));
		}
		if(checkNotDefined(frankElement)) {
			String xsdElementName = frankElement.getSimpleName();
			XmlBuilder attributeBuilder = recursivelyDefineXsdElementUnchecked(frankElement, xsdElementName);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding attribute className for FrankElement [%s]", frankElement.getFullName()));
			}
			addClassNameAttribute(attributeBuilder, frankElement);
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Leave top FrankElement [%s]", frankElement.getFullName()));
		}
	}

	private void addClassNameAttribute(XmlBuilder context, FrankElement frankElement) {
		addAttribute(context, "className", FIXED, frankElement.getFullName(), PROHIBITED);
	}

	private XmlBuilder recursivelyDefineXsdElementUnchecked(FrankElement frankElement, String xsdElementName) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("FrankElement [%s] has XSD element [%s]", frankElement.getFullName(), xsdElementName));
		}
		XmlBuilder elementBuilder = createElementWithType(xsdElementName);
		xsdElements.add(elementBuilder);
		XmlBuilder complexType = addComplexType(elementBuilder);
		XmlBuilder sequence = addSequence(complexType);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding cumulative config chidren of FrankElement [%s] to XSD element [%s]", frankElement.getFullName()), xsdElementName);
		}
		frankElement.getCumulativeConfigChildren(IN_XSD, DEPRECATED).forEach(c -> addConfigChild(sequence, c));
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding cumulative attributes of FrankElement [%s] to XSD element [%s]", frankElement.getFullName(), xsdElementName));
		}
		addAttributeList(complexType, frankElement.getCumulativeAttributes(IN_XSD, DEPRECATED));
		return complexType;
	}

	private void recursivelyDefineXsdElementType(FrankElement frankElement) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("XML Schema needs type definition (or only groups for config children or attributes) for FrankElement [%s]", frankElement.getFullName()));
		}
		if(checkNotDefined(frankElement)) {
			ElementBuildingStrategy elementBuildingStrategy = getElementBuildingStrategy(frankElement);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Visiting config children for FrankElement [%s]", frankElement.getFullName()));
			}
			addConfigChildren(elementBuildingStrategy, frankElement);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Visiting attributes for FrankElement [%s]", frankElement.getFullName()));
			}
			addAttributes(elementBuildingStrategy, frankElement);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Creating type definitions (or only groups) for Java ancestors of FrankElement [%s]", frankElement.getFullName()));
			}
			recursivelyDefineXsdElementType(frankElement.getNextAncestorThatHasConfigChildren(IN_XSD));
			recursivelyDefineXsdElementType(frankElement.getNextAncestorThatHasAttributes(IN_XSD));
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done with XSD type definition of FrankElement [%s]", frankElement.getFullName()));
			}
		} else if(log.isTraceEnabled()) {
			log.trace("Type definition was already included");
		}
	}

	/**
	 * @param frankElement The {@link FrankElement} for which an XSD element or XSD type is needed, or null
	 * @return true if the input is not null and if the element is not yet created.
	 */
	private boolean checkNotDefined(FrankElement frankElement) {
		if(frankElement == null) {
			return false;
		}
		if(namesCreatedFrankElements.contains(frankElement.getFullName())) {
			return false;
		} else {
			namesCreatedFrankElements.add(frankElement.getFullName());
			return true;
		}
	}

	/*
	 * This class is responsible for adding an xs:element in the XML schema if required.
	 * If a FrankElement corresponds to an abstract class, then no XML element
	 * should be added. This is achieved using the derived class ElementOmitter.
	 *
	 * For an abstract FrankElement, the config child declared/cumulative groups
	 * and the attribute declared/cumulative groups are still needed. Adding them is
	 * outside the scope of this class.
	 */
	private abstract class ElementBuildingStrategy {
		abstract void addGroupRef(String referencedGroupName);
		abstract void addAttributeGroupRef(String referencedGroupName);
	}

	private ElementBuildingStrategy getElementBuildingStrategy(FrankElement element) {
		if(element.isAbstract()) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("FrankElement [%s] is abstract, so we do not actually create an XSD type definition", element.getFullName()));
				log.trace("We only create config child or attribute groups to be referenced from other XSD types");
			}
			return new ElementOmitter();
		} else {
			if(log.isTraceEnabled()) {
				log.trace(String.format("FrankElement [%s] is not abstract. We really make the XSD type definition"));
			}
			return new ElementAdder(element);
		}
	}

	private class ElementAdder extends ElementBuildingStrategy {
		private final XmlBuilder complexType;
		
		ElementAdder(FrankElement frankElement) {
			complexType = createComplexType(xsdElementType(frankElement));
			xsdComplexItems.add(complexType);
		}

		@Override
		void addGroupRef(String referencedGroupName) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding reference to XSD group [%s] to XSD type definition", referencedGroupName));
			}
			DocWriterNewXmlUtils.addGroupRef(complexType, referencedGroupName);
		}

		@Override
		void addAttributeGroupRef(String referencedGroupName) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding reference to XSD group [%s] to XSD type definition", referencedGroupName));
			}
			DocWriterNewXmlUtils.addAttributeGroupRef(complexType, referencedGroupName);
		}
	}

	private class ElementOmitter extends ElementBuildingStrategy {
		@Override
		void addGroupRef(String referencedGroupName) {
		}
		@Override
		void addAttributeGroupRef(String referencedGroupName) {
		}
	}

	private void addConfigChildren(ElementBuildingStrategy elementBuildingStrategy, FrankElement frankElement) {
		Consumer<GroupCreator.Callback<ConfigChild>> cumulativeGroupTrigger =
				ca -> frankElement.walkCumulativeConfigChildren(ca, IN_XSD, DEPRECATED);
		new GroupCreator<ConfigChild>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<ConfigChild>() {
			private XmlBuilder cumulativeBuilder;
			private String cumulativeGroupName;

			@Override
			public List<ConfigChild> getChildrenOf(FrankElement elem) {
				return elem.getConfigChildren(IN_XSD);
			}
			
			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAncestorThatHasConfigChildren(IN_XSD);
			}
			
			@Override
			public void addDeclaredGroupRef(FrankElement referee) {
				elementBuildingStrategy.addGroupRef(xsdDeclaredGroupNameForChildren(referee));
			}
			
			@Override
			public void addCumulativeGroupRef(FrankElement referee) {
				elementBuildingStrategy.addGroupRef(xsdCumulativeGroupNameForChildren(referee));				
			}

			@Override
			public void addDeclaredGroup() {
				String groupName = xsdDeclaredGroupNameForChildren(frankElement);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Creating XSD group [%s]", groupName));
				}
				XmlBuilder group = createGroup(groupName);
				xsdComplexItems.add(group);
				XmlBuilder sequence = addSequence(group);
				frankElement.getConfigChildren(IN_XSD).forEach(c -> addConfigChild(sequence, c));
				if(log.isTraceEnabled()) {
					log.trace(String.format("Done creating XSD group [%s] on behalf of FrankElement [%s]", groupName, frankElement.getFullName()));
				}				
			}

			@Override
			public void addCumulativeGroup() {
				cumulativeGroupName = xsdCumulativeGroupNameForChildren(frankElement);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Start creating XSD group [%s]", cumulativeGroupName));
				}
				XmlBuilder group = createGroup(cumulativeGroupName);
				xsdComplexItems.add(group);
				cumulativeBuilder = addSequence(group);
			}

			@Override
			public void handleSelectedChildren(List<ConfigChild> children, FrankElement owner) {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Appending some of the config children of FrankElement [%s] to XSD group [%s]",
							owner.getFullName(), cumulativeGroupName));
				}
				children.forEach(c -> addConfigChild(cumulativeBuilder, c));
			}
			
			@Override
			public void handleChildrenOf(FrankElement elem) {
				String referencedGroupName = xsdDeclaredGroupNameForChildren(elem);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Appending XSD group [%s] with reference to [%s]", cumulativeGroupName, referencedGroupName));
				}
				DocWriterNewXmlUtils.addGroupRef(cumulativeBuilder, referencedGroupName);
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement elem) {
				String referencedGroupName = xsdCumulativeGroupNameForChildren(elem);
				if(log.isTraceEnabled()) {
					log.trace("Appending XSD group [%s] with reference to [%s]", cumulativeGroupName, referencedGroupName);
				}
				DocWriterNewXmlUtils.addGroupRef(cumulativeBuilder, referencedGroupName);
			}
		}).run();
	}

	private void addConfigChild(XmlBuilder context, ConfigChild child) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding config child [%s]", child.toString()));
		}
		ElementRole theRole = model.findElementRole(child);
		if(isNoElementTypeNeeded(theRole)) {
			addConfigChildSingleReferredElement(context, child);
		} else {
			addConfigChildWithElementGroup(context, child);
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done adding config child [%s]", child.toString()));
		}
	}

	private boolean isNoElementTypeNeeded(ElementRole role) {
		ElementType elementType = role.getElementType();
		if(elementType.isFromJavaInterface()) {
			return false;
		}
		else {
			return true;
		}
	}

	private void addConfigChildSingleReferredElement(XmlBuilder context, ConfigChild child) {
		ElementRole role = model.findElementRole(child);
		FrankElement elementInType = singleElementOf(role.getElementType());
		String referredXsdElement = elementInType.getXsdElementName(role);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Config child appears as element reference to FrankElement [%s], XSD element [%s]", elementInType, referredXsdElement));
		}
		addElementRef(context, referredXsdElement, getMinOccurs(child), getMaxOccurs(child));
		recursivelyDefineXsdElement(elementInType, role);
	}

	private FrankElement singleElementOf(ElementType elementType) {
		return elementType.getMembers().values().iterator().next();
	}

	private void recursivelyDefineXsdElement(FrankElement frankElement, ElementRole role) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("FrankElement [%s] is needed in XML Schema", frankElement));
		}
		if(checkNotDefined(frankElement)) {
			if(log.isTraceEnabled()) {
				log.trace("Not yet defined in XML Schema, going to define it");
			}
			String xsdElementName = frankElement.getXsdElementName(role);
			XmlBuilder attributeBuilder = recursivelyDefineXsdElementUnchecked(frankElement, xsdElementName);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding attributes className and %s for FrankElement [%s]", ELEMENT_GROUP, frankElement));
			}
			addExtraAttributesNotFromModel(attributeBuilder, frankElement, role);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done defining FrankElement [%s], XSD element [%s]", frankElement, xsdElementName));
			}
		} else if(log.isTraceEnabled()) {
			log.trace("Already defined in XML Schema");
		}
	}

	private void addExtraAttributesNotFromModel(XmlBuilder context, FrankElement frankElement, ElementRole role) {
		addAttribute(context, ELEMENT_ROLE, FIXED, role.getSyntax1Name(), PROHIBITED);
		addClassNameAttribute(context, frankElement);
	}

	private void addConfigChildWithElementGroup(XmlBuilder context, ConfigChild child) {
		if(log.isTraceEnabled()) {
			log.trace("Config child appears as element group reference");
		}
		ElementRole role = model.findElementRole(child);
		defineElementTypeGroup(role);
		DocWriterNewXmlUtils.addGroupRef(context, role.createXsdElementName(ELEMENT_GROUP), getMinOccurs(child), getMaxOccurs(child));
	}

	private void defineElementTypeGroup(ElementRole role) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Element group needed for ElementRole [%s]", role.toString()));
		}
		ElementRole.Key key = role.getKey();
		if(! idsCreatedElementGroups.contains(key)) {
			idsCreatedElementGroups.add(key);
			defineElementTypeGroupUnchecked(role);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done defining ElementGroup for ElementRole [%s]", role.toString()));
			}
		} else if(log.isTraceEnabled()) {
			log.trace("ElementGroup already defined");
		}
	}

	private void defineElementTypeGroupUnchecked(ElementRole role) {
		XmlBuilder group = createGroup(role.createXsdElementName(ELEMENT_GROUP));
		xsdComplexItems.add(group);
		XmlBuilder choice = addChoice(group);
		List<FrankElement> frankElementOptions = role.getElementType().getMembers().values().stream()
				.filter(f -> ! f.isDeprecated())
				.filter(f -> ! f.isAbstract())
				.collect(Collectors.toList());
		frankElementOptions.sort((o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName()));
		addGenericElementOption(choice, role, disambiguateGenericOptionElementName(role, frankElementOptions));
		for(FrankElement frankElement: frankElementOptions) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Append ElementGroup with FrankElement [%s]", frankElement.getFullName()));
			}
			addElementToElementGroup(choice, frankElement, role);
		}		
	}

	// TODO: Move this to the model.
	private String disambiguateGenericOptionElementName(ElementRole role, List<FrankElement> membersToInclude) {
		// Do not include sequence number that made the role name unique.
		String result = Utils.toUpperCamelCase(role.getSyntax1Name());
		Set<String> conflictCandidates = membersToInclude.stream()
				.map(f -> f.getXsdElementName(role))
				.collect(Collectors.toSet());
		if(conflictCandidates.contains(result)) {
			result = "Generic" + result;
		}
		return result;
	}

	private void addGenericElementOption(XmlBuilder choice, ElementRole role, String elementNameGenericOption) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding generic element option, XSD element is [%s]", elementNameGenericOption));
		}
		XmlBuilder genericElementOption = addElementWithType(choice, elementNameGenericOption);
		XmlBuilder complexType = addComplexType(genericElementOption);
		addElementTypeChildMembers(complexType, role);
		addAttribute(complexType, ELEMENT_ROLE, FIXED, role.getSyntax1Name(), PROHIBITED);
		addAttribute(complexType, "className", DEFAULT, null, REQUIRED);
		// The XSD is invalid if addAnyAttribute is added before attributes elementType and className.
		addAnyAttribute(complexType);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done adding generic element option, XSD element [%s]", elementNameGenericOption));
		}
	}

	private void addElementTypeChildMembers(XmlBuilder context, ElementRole role) {
		if(log.isTraceEnabled()) {
			log.trace("Generic element option appears as reference to child member group");
		}
		DocWriterNewXmlUtils.addGroupRef(context, xsdElementTypeMemberChildGroup(role.getElementType()), "0", "unbounded");
		addElementTypeMemberChildGroup(role);
	}

	private void addElementTypeMemberChildGroup(ElementRole role) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Child member group needed in XML Schema for role [%s]", role.toString()));
		}
		if(! namesElementTypesWithChildMemberGroup.contains(role.getElementType().getFullName())) {
			if(log.isTraceEnabled()) {
				log.trace("Defining child member group");
			}
			namesElementTypesWithChildMemberGroup.add(role.getElementType().getFullName());
			addElementTypeMemberChildGroupUnchecked(role);
			if(log.isTraceEnabled()) {
				log.trace("Done defining child member group");
			}
		} else if(log.isTraceEnabled()) {
			log.trace("Child member group already defined");
		}
	}

	private void addElementTypeMemberChildGroupUnchecked(ElementRole role) {
		XmlBuilder group = createGroup(xsdElementTypeMemberChildGroup(role.getElementType()));
		xsdComplexItems.add(group);
		XmlBuilder choice = addChoice(group);
		for(ElementRole childRole: new MemberChildrenCalculator(role, model).getMemberChildOptions()) {
			addElementTypeMemberChildGroupOption(choice, childRole);
		}
	}

	private void addElementTypeMemberChildGroupOption(XmlBuilder choice, ElementRole childRole) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding to child member group: role [%s]", childRole.toString()));
		}
		if(isNoElementTypeNeeded(childRole)) {
			FrankElement frankElement = singleElementOf(childRole.getElementType());
			String xsdElementName = frankElement.getXsdElementName(childRole);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Added as element reference to XSD element [%s]", xsdElementName));
			}
			addElementRef(choice, xsdElementName);
		} else {
			String referencedXsdGroup = childRole.createXsdElementName(ELEMENT_GROUP);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Added as group reference to [%s]", referencedXsdGroup));
			}
			DocWriterNewXmlUtils.addGroupRef(choice, referencedXsdGroup);
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done adding to child member group: role [%s]", childRole.toString()));
		}
	}

	private void addElementToElementGroup(XmlBuilder context, FrankElement frankElement, ElementRole role) {
		addElementTypeRefToElementGroup(context, frankElement, role);
		recursivelyDefineXsdElementType(frankElement);
	}

	private void addElementTypeRefToElementGroup(XmlBuilder context, FrankElement frankElement, ElementRole role) {
		XmlBuilder element = addElementWithType(context, frankElement.getXsdElementName(role));
		XmlBuilder complexType = addComplexType(element);
		XmlBuilder complexContent = addComplexContent(complexType);
		XmlBuilder extension = addExtension(complexContent, xsdElementType(frankElement));
		addExtraAttributesNotFromModel(extension, frankElement, role);
	}

	private void addAttributes(ElementBuildingStrategy elementBuildingStrategy, FrankElement frankElement) {
		Consumer<GroupCreator.Callback<FrankAttribute>> cumulativeGroupTrigger =
				ca -> frankElement.walkCumulativeAttributes(ca, IN_XSD, DEPRECATED);
		new GroupCreator<FrankAttribute>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<FrankAttribute>() {
			private XmlBuilder cumulativeBuilder;
			private String cumulativeGroupName;

			@Override
			public List<FrankAttribute> getChildrenOf(FrankElement elem) {
				return elem.getAttributes(IN_XSD);
			}

			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAncestorThatHasAttributes(IN_XSD);
			}

			@Override
			public void addDeclaredGroupRef(FrankElement referee) {
				elementBuildingStrategy.addAttributeGroupRef(xsdDeclaredGroupNameForAttributes(referee));
			}

			@Override
			public void addCumulativeGroupRef(FrankElement referee) {
				elementBuildingStrategy.addAttributeGroupRef(xsdCumulativeGroupNameForAttributes(referee));				
			}

			@Override
			public void addDeclaredGroup() {
				String groupName = xsdDeclaredGroupNameForAttributes(frankElement);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Creating XSD group [%s]", groupName));
				}
				XmlBuilder attributeGroup = createAttributeGroup(groupName);
				xsdComplexItems.add(attributeGroup);
				addAttributeList(attributeGroup, frankElement.getAttributes(IN_XSD));
				if(log.isTraceEnabled()) {
					log.trace(String.format("Done creating XSD group [%s] on behalf of FrankElement [%s]", groupName, frankElement.getFullName()));
				}				
			}

			@Override
			public void addCumulativeGroup() {
				cumulativeGroupName = xsdCumulativeGroupNameForAttributes(frankElement);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Start creating XSD group [%s]", cumulativeGroupName));
				}
				cumulativeBuilder = createAttributeGroup(cumulativeGroupName);
				xsdComplexItems.add(cumulativeBuilder);
			}

			@Override
			public void handleSelectedChildren(List<FrankAttribute> children, FrankElement owner) {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Appending some of the attributes of FrankElement [%s] to XSD group [%s]",
							owner.getFullName(), cumulativeGroupName));
				}
				addAttributeList(cumulativeBuilder, children);
			}

			@Override
			public void handleChildrenOf(FrankElement elem) {
				String referencedGroupName = xsdDeclaredGroupNameForAttributes(elem);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Appending XSD group [%s] with reference to [%s]", cumulativeGroupName, referencedGroupName));
				}
				DocWriterNewXmlUtils.addAttributeGroupRef(cumulativeBuilder, referencedGroupName);
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement elem) {
				String referencedGroupName = xsdCumulativeGroupNameForAttributes(elem);
				if(log.isTraceEnabled()) {
					log.trace("Appending XSD group [%s] with reference to [%s]", cumulativeGroupName, referencedGroupName);
				}
				DocWriterNewXmlUtils.addAttributeGroupRef(cumulativeBuilder, referencedGroupName);				
			}
		}).run();
	}

	private void addAttributeList(XmlBuilder context, List<FrankAttribute> frankAttributes) {
		for(FrankAttribute frankAttribute: frankAttributes) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding attribute [%s]", frankAttribute.getName()));
			}
			// The default value in the model is a *description* of the default value.
			// Therefore, it should be added to the description in the xs:attribute.
			// The "default" attribute of the xs:attribute should not be set.
			XmlBuilder attribute = addAttribute(context, frankAttribute.getName(), DEFAULT, null, OPTIONAL);
			if(needsDocumentation(frankAttribute)) {
				if(log.isTraceEnabled()) {
					log.trace("Attribute has documentation");
				}
				addDocumentation(attribute, getDocumentationText(frankAttribute));
			}
		}		
	}

	private boolean needsDocumentation(FrankAttribute frankAttribute) {
		return (! StringUtils.isEmpty(frankAttribute.getDescription())) || (! StringUtils.isEmpty(frankAttribute.getDefaultValue()));
	}

	private String getDocumentationText(FrankAttribute frankAttribute) {
		StringBuilder result = new StringBuilder();
		if(! StringUtils.isEmpty(frankAttribute.getDescription())) {
			result.append(frankAttribute.getDescription());
		}
		if(! StringUtils.isEmpty(frankAttribute.getDefaultValue())) {
			if(result.length() >= 1) {
				result.append(" ");
			}
			result.append("Default: ");
			result.append(frankAttribute.getDefaultValue());
		}
		return result.toString();
	}

	private String xsdElementType(FrankElement frankElement) {
		return frankElement.getSimpleName() + "Type";
	}

	private static String xsdDeclaredGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "DeclaredChildGroup";
	}

	private static String xsdCumulativeGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "CumulativeChildGroup";
	}

	private String xsdElementTypeMemberChildGroup(ElementType elementType) {
		return elementType.getSimpleName() + MEMBER_CHILD_GROUP;
	}

	private static String xsdDeclaredGroupNameForAttributes(FrankElement element) {
		return element.getSimpleName() + "DeclaredAttributeGroup";
	}

	private static String xsdCumulativeGroupNameForAttributes(FrankElement element) {
		return element.getSimpleName() + "CumulativeAttributeGroup";
	}

	private static String getMinOccurs(ConfigChild child) {
		if(child.isMandatory()) {
			return "1";
		} else {
			return "0";
		}
	}

	private static String getMaxOccurs(ConfigChild child) {
		if(child.isAllowMultiple()) {
			return "unbounded";
		} else {
			return "1";
		}
	}
}
