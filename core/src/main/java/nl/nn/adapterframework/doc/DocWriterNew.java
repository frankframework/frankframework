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

import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttribute;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttributeGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttributeGroupRef;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addChoice;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addDocumentation;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElementRef;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElementWithType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addGroupRef;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addSequence;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.getXmlSchema;
import static nl.nn.adapterframework.doc.model.ElementChild.SELECTED;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.doc.model.ElementChild;
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
 * <h1>Inheritance of attributes and config children</h1>
 * 
 * Below, a few implementation details are explained. Each XML tag that is
 * allowed in a Frank configuration appears as an &lt;xs:element&gt; in the XSD.
 * Each &lt;xs:element&gt; in the XSD has a corresponding
 * {@link FrankElement} object in the model. Each element in the 
 * XSD can have attributes or other elements. These correspond to {@link FrankAttribute}
 * objects or {@link ConfigChild} objects in the model.
 * <p>
 * In the model, a {@link FrankElement} only holds its declared attributes, but
 * the &lt;xs:element&gt; should allow both the declared attributes and the attributes
 * inherited from the ancestors of the {@link FrankElement} (the inherited attributes).
 * The same holds for configuration children. This similarity appears in the model
 * through the common base class {@link ElementChild}, which is a parent class of both
 * {@link FrankAttribute} and {@link ConfigChild}. An attribute defined high in the
 * class hierarchy of the Frank!Framework can be allowed for many &lt;xs:element&gt; tags in
 * the XSD, but we do not want to repeat the same &lt;xs:attribute&gt; tags in all these cases.
 * We solve this by grouping the attributes, and the config children, in the XSD, for example:
 * 
 * <pre>
 * {@code
<xs:element name="Configuration">
  <xs:complexType>
    <xs:group ref="ConfigurationDeclaredChildGroup" />
    <xs:attributeGroup ref="ConfigurationDeclaredAttributeGroup" />
  </xs:complexType>
</xs:element>
}
 * </pre>
 * <p>
 * The example shows a group named <code>ConfigurationDeclaredChildGroup</code>. This group
 * declares all &lt;xs:element&gt; that are allowed as children of Frank config tag
 * &lt;Configuration&gt;.
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
 * Java ancestor class. In this situation, only the &lt;xs:element&gt; corresponding
 * to the Java subclass is needed. The attribute
 * of the ancestor class is omitted. The following example illustrates this:
 * <pre>
 {@code
<xs:attributeGroup name="SoapValidatorCumulativeAttributeGroup">
  <xs:attributeGroup ref="SoapValidatorDeclaredAttributeGroup" />
  <xs:attributeGroup ref="Json2XmlValidatorDeclaredAttributeGroup" />
  <xs:attribute name="ignoreUnknownNamespaces" type="xs:string" />
  <xs:attribute name="schema" type="xs:string" default="">
    <xs:annotation>
      <xs:documentation>the filename of the schema on the classpath. see doc on the method. (effectively the same as noNamespaceSchemaLocation)</xs:documentation>
    </xs:annotation>
  </xs:attribute>
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
 * Please note that in this example the "<code>schema</code>" attribute appears with
 * the grand parent, even though Java class <code>SoapValidator</code> overrides
 * method <code>setSchema()</code>. This is the case because
 * <code>SoapValidator.setSchema()</code> does neither have an IbisDoc nor an
 * IbisDocRef annotation. This is a technical override that is ignored when
 * writing the XSD. This is implemented using the {@link ElementChild}
 * properties "documented", "overriddenFrom" and "deprecated".
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
<xs:element name="Listener" minOccurs="0" maxOccurs="1">
  <xs:complexType>
    <xs:group ref="IListenerElementGroup" minOccurs="1" maxOccurs="1" />
  </xs:complexType>
</xs:element>
}
 * </pre>
 * This snippet appears within <code>&lt;xs:group name="ReceiverDeclaredChildGroup"&gt;&lt;xs:sequence&gt;</code>.
 * The snippet states that a Receiver can contain a &lt;Listener&gt; that in turn can contain any
 * &lt;xs:element&gt; that is related to a Java class that implements Java interface <code>IListener</code>.
 * To summarize, a Frank config is allowed to contain <code>&lt;Receiver&gt;&lt;Listener&gt;&ltJavaListener...&gt;</code>.
 * <p>
 * A simpler syntax is required for config children that allow only one option, for example
 * "<code>PipeForward</code>". The {@link ConfigChild} for the combination of owning element "<code>AbstractPipe</code>"
 * and {@link ElementType} "<code>PipeForward</code>" produces just the following:
 * <pre>
 * {@code
<xs:element ref="Forward" minOccurs="0" maxOccurs="unbounded" />
}
 * </pre>
 * This snippet is contained in <code>&lt;xs:group name="AbstractPipeDeclaredChildGroup"&gt;&lt;xs:sequence&gt;</code>.
 * It states that any pipe (all inherit <code>AbstractPipe</code>) can hold <code>&lt;Forward ... &gt;</code> instead of
 * <code>&lt;Forward&gt;&lt;PipeForward ... &gt;</code>. The XML tag <code>&lt;Forward&gt;</code> does not come
 * from the Frank!Framework Java code. Instead, it comes from the file <code>digester-rules.xml</code> that
 * is a resource within the Frank!Framework source code. The model supports producing this XML schema
 * by the method <code>FrankElement.getAlias()</code>.
 * <p>
 * Finally, the XSD allows Frank config like:
 * <pre>
 * {@code
<Pipes>
  <FixedResult returnString="HelloWorld">
    <Forward name="success" path="next" />
  </FixedResult>
  <FixedResult returnString="HelloWorld again">
    <Forward name="success" path="EXIT" />
  </FixedResult>
</Pipes>
}
 * </pre>
 * The tag <code>&lt;Pipes&gt;</code> is obtained as follows. The word "<code>Pipe</code>" is found in
 * resource <code>digester-rules.xml</code>, available through the method
 * <code>ConfigChild.getSyntax1Name()</code>. This word is made plural by adding an "s", which
 * is implemented in method <code>ConfigChild.getSyntax1NamePlural()</code>.
 *
 * @author martijn
 *
 */
public class DocWriterNew {
	private static final String CONFIGURATION = "nl.nn.adapterframework.configuration.Configuration";

	private static Logger log = LogUtil.getLogger(DocWriterNew.class);

	private static Comparator<FrankElement> FIX_ELEMENT_SEQUENCE =
			Comparator.comparing(FrankElement::getSimpleName).thenComparing(FrankElement::getFullName);

	private FrankDocModel model;
	List<SortKeyForXsd> xsdSortOrder;
	private XmlBuilder xsdRoot;

	public DocWriterNew(FrankDocModel model) {
		this.model = model;
	}

	public void init() {
		init(CONFIGURATION);
	}

	public void init(String startClassName) {
		xsdSortOrder = breadthFirstSort(startClassName);
	}

	List<SortKeyForXsd> breadthFirstSort(String sourceFullName) {
		return new FrankElementBreadthFirstSorter().sort(sourceFullName);
	}

	private class FrankElementBreadthFirstSorter {		
		private Set<SortKeyForXsd> available;
		private Deque<SortKeyForXsd> processing;
		private List<SortKeyForXsd> result;

		FrankElementBreadthFirstSorter() {
			available = new HashSet<>();
			available.addAll(model.getAllTypes().values().stream()
					.filter(t -> t.isFromJavaInterface())
					.map(SortKeyForXsd::getInstance)
					.collect(Collectors.toSet()));
			available.addAll(model.getAllElements().values().stream()
					.map(SortKeyForXsd::getInstance)
					.collect(Collectors.toSet()));
			processing = new ArrayDeque<>();
			result = new ArrayList<>();
		}

		List<SortKeyForXsd> sort(String sourceFullName) {
			if(! model.getAllElements().containsKey(sourceFullName)) {
				log.warn(String.format("Cannot sort FrankElements because element [%s] is not known", sourceFullName));
				return null;
			}
			SortKeyForXsd sourceKey = SortKeyForXsd.getInstance(model.getAllElements().get(sourceFullName));
			take(sourceKey);
			while(! processing.isEmpty()) {
				SortKeyForXsd current = processing.removeFirst();
				result.add(current);
				List<SortKeyForXsd> children = getSortedChildren(current);
				children.stream().filter(available::contains).forEach(this::take);
			}
			return result;
		}

		private void take(SortKeyForXsd sortKey) {
			processing.addLast(sortKey);
			available.remove(sortKey);
		}

		private List<SortKeyForXsd> getSortedChildren(SortKeyForXsd parent) {
			switch(parent.getKind()) {
			case TYPE:
				return getSortedTypeChildren(parent.getName());
			default:
				return getSortedElementChildren(parent.getName());
			}
		}

		private List<SortKeyForXsd> getSortedTypeChildren(String typeName) {
			ElementType elementType = model.getAllTypes().get(typeName);
			List<FrankElement> members = new ArrayList<>(elementType.getMembers().values());
			members.sort(FIX_ELEMENT_SEQUENCE);
			return members.stream().map(SortKeyForXsd::getInstance).collect(Collectors.toList());
		}

		private List<SortKeyForXsd> getSortedElementChildren(String elementName) {
			FrankElement element = model.getAllElements().get(elementName);
			List<ConfigChild> configChildren = element.getConfigChildren(SELECTED);
			configChildren.sort((c1, c2) -> c1.getSyntax1Name().compareTo(c2.getSyntax1Name()));
			List<SortKeyForXsd> result = configChildren.stream()
					.map(c -> getTypeKey(c))
					.collect(Collectors.toList());
			FrankElement candidateParent = element.getNextAncestor(SELECTED);
			if(candidateParent != null) {
				result.add(SortKeyForXsd.getInstance(candidateParent));
			}
			return result;
		}

		private SortKeyForXsd getTypeKey(ConfigChild configChild) {
			ElementType elementType = configChild.getElementType();
			if(elementType.isFromJavaInterface()) {
				return SortKeyForXsd.getInstance(elementType);
			}
			else {
				FrankElement content = elementType.getMembers().values().iterator().next();
				return SortKeyForXsd.getInstance(content);
			}
		}
	}


	public String getSchema() {
		xsdRoot = getXmlSchema();
		defineAllTypes();
		return xsdRoot.toXML(true);
	}

	private void defineAllTypes() {
		for(SortKeyForXsd item: xsdSortOrder) {
			switch(item.getKind()) {
			case ELEMENT:
				defineElement(model.getAllElements().get(item.getName()));
				break;
			case TYPE:
				defineElementTypeGroup(model.getAllTypes().get(item.getName()));
				break;
			}
		}
	}

	private void defineElement(FrankElement frankElement) {
		ElementBuildingStrategy elementBuildingStrategy = getElementBuildingStrategy(frankElement);
		addConfigChildren(frankElement, elementBuildingStrategy);
		addAttributes(frankElement, elementBuildingStrategy);		
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
			return new ElementOmitter();
		} else {
			return new ElementAdder(element);
		}
	}

	private class ElementAdder extends ElementBuildingStrategy {
		private final XmlBuilder complexType;

		ElementAdder(FrankElement frankElement) {
			XmlBuilder element = addElementWithType(xsdRoot, frankElement.getAlias());
			complexType = addComplexType(element);
		}

		@Override
		void addGroupRef(String referencedGroupName) {
			DocWriterNewXmlUtils.addGroupRef(complexType, referencedGroupName);
		}
		
		@Override
		void addAttributeGroupRef(String referencedGroupName) {
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

	private void addConfigChildren(FrankElement frankElement, ElementBuildingStrategy xsdElementStrategy) {
		Consumer<GroupCreator.Callback<ConfigChild>> cumulativeGroupTrigger =
				ca -> frankElement.walkCumulativeConfigChildren(ca, SELECTED);
		new GroupCreator<ConfigChild>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<ConfigChild>() {
			private XmlBuilder cumulativeBuilder;
			
			@Override
			public List<ConfigChild> getChildrenOf(FrankElement elem) {
				return elem.getConfigChildren(SELECTED);
			}
			
			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextConfigChildAncestor(SELECTED);
			}
			
			@Override
			public void addDeclaredGroupRef(FrankElement referee) {
				xsdElementStrategy.addGroupRef(xsdDeclaredGroupNameForChildren(referee));
			}
			
			@Override
			public void addCumulativeGroupRef(FrankElement referee) {
				xsdElementStrategy.addGroupRef(xsdCumulativeGroupNameForChildren(referee));				
			}

			@Override
			public void addDeclaredGroup() {
				XmlBuilder group = addGroup(xsdRoot, xsdDeclaredGroupNameForChildren(frankElement));
				XmlBuilder sequence = addSequence(group);
				frankElement.getConfigChildren(SELECTED).forEach(
						c -> addConfigChild(sequence, c));
			}

			@Override
			public void addCumulativeGroup() {
				XmlBuilder group = addGroup(xsdRoot, xsdCumulativeGroupNameForChildren(frankElement));
				cumulativeBuilder = addSequence(group);
			}

			@Override
			public void handleSelectedChildren(List<ConfigChild> children, FrankElement owner) {
				children.forEach(c -> addConfigChild(cumulativeBuilder, c));
			}
			
			@Override
			public void handleChildrenOf(FrankElement elem) {
				addGroupRef(cumulativeBuilder, xsdDeclaredGroupNameForChildren(elem));
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement elem) {
				addGroupRef(cumulativeBuilder, xsdCumulativeGroupNameForChildren(elem));
			}
		}).run();
	}

	private static String xsdDeclaredGroupNameForChildren(FrankElement element) {
		return element.getAlias() + "DeclaredChildGroup";
	}

	private static String xsdCumulativeGroupNameForChildren(FrankElement element) {
		return element.getAlias() + "CumulativeChildGroup";
	}

	private void addConfigChild(XmlBuilder context, ConfigChild child) {
		ElementType elementType = child.getElementType();
		if(elementType.isFromJavaInterface()) {
			XmlBuilder xsdElement = addElementWithType(
					context, Utils.toUpperCamelCase(xsdFieldName(child)), getMinOccurs(child), "1");
			XmlBuilder complexType = addComplexType(xsdElement);
			addGroupRef(complexType, xsdGroupOf(elementType), "1", getMaxOccurs(child));
		}
		else {
			FrankElement containedFrankElement = elementType.getMembers().values().iterator().next();
			addElementRef(context, containedFrankElement.getAlias(),
					getMinOccurs(child), getMaxOccurs(child));
		}
	}

	String xsdFieldName(final ConfigChild child) {
		if(child.isAllowMultiple()) {
			return child.getSyntax1NamePlural();
		} else {
			return child.getSyntax1Name();
		}
	}

	private static String xsdGroupOf(ElementType elementType) {
		return elementType.getSimpleName() + "ElementGroup";
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

	private void addAttributes(FrankElement frankElement, ElementBuildingStrategy xsdElementStrategy) {
		Consumer<GroupCreator.Callback<FrankAttribute>> cumulativeGroupTrigger =
				ca -> frankElement.walkCumulativeAttributes(ca, SELECTED);
		new GroupCreator<FrankAttribute>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<FrankAttribute>() {
			private XmlBuilder cumulativeBuilder;

			@Override
			public List<FrankAttribute> getChildrenOf(FrankElement elem) {
				return elem.getAttributes(SELECTED);
			}

			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAttributeAncestor(SELECTED);
			}

			@Override
			public void addDeclaredGroupRef(FrankElement referee) {
				xsdElementStrategy.addAttributeGroupRef(xsdDeclaredGroupNameForAttributes(referee));
			}

			@Override
			public void addCumulativeGroupRef(FrankElement referee) {
				xsdElementStrategy.addAttributeGroupRef(xsdCumulativeGroupNameForAttributes(referee));				
			}

			@Override
			public void addDeclaredGroup() {
				XmlBuilder attributeGroup = addAttributeGroup(xsdRoot, xsdDeclaredGroupNameForAttributes(frankElement));
				addAttributeList(attributeGroup, frankElement.getAttributes(SELECTED));
			}

			@Override
			public void addCumulativeGroup() {
				cumulativeBuilder = addAttributeGroup(xsdRoot, xsdCumulativeGroupNameForAttributes(frankElement));				
			}

			@Override
			public void handleSelectedChildren(List<FrankAttribute> children, FrankElement owner) {
				addAttributeList(cumulativeBuilder, children);
			}

			@Override
			public void handleChildrenOf(FrankElement elem) {
				addAttributeGroupRef(cumulativeBuilder, xsdDeclaredGroupNameForAttributes(elem));
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement elem) {
				addAttributeGroupRef(cumulativeBuilder, xsdCumulativeGroupNameForAttributes(elem));				
			}
		}).run();
	}

	private static String xsdDeclaredGroupNameForAttributes(FrankElement element) {
		return element.getAlias() + "DeclaredAttributeGroup";
	}

	private static String xsdCumulativeGroupNameForAttributes(FrankElement element) {
		return element.getAlias() + "CumulativeAttributeGroup";
	}

	private void addAttributeList(XmlBuilder context, List<FrankAttribute> frankAttributes) {
		for(FrankAttribute frankAttribute: frankAttributes) {
			XmlBuilder attribute = addAttribute(context, frankAttribute.getName(), frankAttribute.getDefaultValue());
			if(! StringUtils.isEmpty(frankAttribute.getDescription())) {
				addDocumentation(attribute, frankAttribute.getDescription());
			}
		}		
	}

	private void defineElementTypeGroup(ElementType elementType) {
		XmlBuilder group = addGroup(xsdRoot, xsdGroupOf(elementType));
		XmlBuilder choice = addChoice(group);
		List<FrankElement> frankElementOptions = new ArrayList<>(elementType.getMembers().values());
		frankElementOptions.sort((o1, o2) -> o1.getAlias().compareTo(o2.getAlias()));
		for(FrankElement frankElement: frankElementOptions) {
			addElementRef(choice, frankElement.getAlias());
		}		
	}
}
