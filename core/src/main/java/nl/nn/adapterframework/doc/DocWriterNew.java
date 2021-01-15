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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

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
 * This class writes the XML schema document (XSD) that checks the validity of a
 * Frank configuration XML file. The XML schema is written based on the information
 * in a {@link FrankDocModel} object (the model). This class is still under construction.
 * Presently, class {@link DocWriter} is in use for writing the XML schema file.
 * <p>
 * Below, a few implementation details are explained.
 *
 * <h1>How FrankElement is expressed in the XSD</h1>
 *
 * First, the integration specialist
 * references an element by a name that reveals both the requested Java class
 * (expressed as a {@link FrankElement} in the model) and the role it plays
 * (e.g. sender or error sender). These requirements are implemented by model
 * method {@link FrankElement#getXsdElementName}. This method takes as an
 * argument the relevant {@link ElementRole}. This applies to XSD elements that
 * are nested in some other XSD element. The root XSD element has as its name
 * the simple name of the corresponding {@link FrankElement}, which is
 * {@link nl.nn.adapterframework.configuration.Configuration}.
 * <p>
 * This class <code>DocWriterNew</code> can apply two different strategies to express
 * an {@link ElementRole}. The strategy is chosen based on the {@link ElementType}
 * referenced by the {@link ElementRole}. If the {@link ElementType} models a Java
 * class, then it has the {@link FrankElement} of that class as the only member.
 * In this case, an XSD element definition is added as a child of the XML schema root
 * element. The second strategy is applied when the {@link ElementType} models a
 * Java interface. In this case, the {@link FrankElement} is expressed as an XSD
 * element <em>type</em> definition under the XML schema root element. Using a type definition
 * allows definitions to be reused when the same {@link FrankElement} can play
 * different roles. We also reuse XSD definitions for {@link FrankElement} objects
 * modeling Java classes with an inheritance relation.
 * <p>
 * The XML schema type (<code>xs:complexType</code>) references a group of config children and a group
 * of attributes. Below, this is shown for the XSD type definition for {@link FrankElement}
 * {@link nl.nn.adapterframework.pipes.SenderPipe}:
 * 
 * <pre>
 * {@code
<xs:complexType name="SenderPipeType">
  <xs:group ref="SenderPipeCumulativeChildGroup" />
  <xs:attributeGroup ref="MessageSendingPipeCumulativeAttributeGroup" />
</xs:complexType>
} 
 * </pre>
 *  
 * The {@link ConfigChild} instances in {@link FrankElement}
 * {@link nl.nn.adapterframework.pipes.SenderPipe} appear in an XSD group
 * <code>SenderPipeDeclaredChildGroup</code>, as follows:
 * <pre>
 * {@code
<xs:group name="SenderPipeDeclaredChildGroup">
  <xs:sequence>
    <xs:group ref="SenderElementGroup" minOccurs="0" maxOccurs="1" />
    <xs:group ref="ListenerElementGroup_3" minOccurs="0" maxOccurs="1" />
  </xs:sequence>
</xs:group>
}
 * </pre>
 * This snippet shows <code>SenderPipeDeclaredChildGroup</code>, not
 * <code>SenderPipeCumulativeChildGroup</code>. This has to do with reusing XSD code
 * when the Java classes modeled by {@link FrankElement} objects have inheritance
 * relations. This is explained later.
 * <p>
 * A list of allowed child tags appears in an <code>ElementGroup</code>, for example:
 * <pre>
 * {@code
  <xs:group name="SenderElementGroup">
    <xs:choice>
      <xs:element name="Sender">
        <xs:complexType>
          <xs:group ref="ISenderMemberChildGroup" minOccurs="0" maxOccurs="unbounded" />
          <xs:attribute name="elementRole" type="xs:string" fixed="sender" use="prohibited" />
          <xs:attribute name="className" type="xs:string" use="required" />
          <xs:anyAttribute />
        </xs:complexType>
      </xs:element>
      <xs:element name="Afm2EdiFactSender">
        <xs:complexType>
          <xs:complexContent>
            <xs:extension base="Afm2EdiFactSenderType">
              <xs:attribute name="elementRole" type="xs:string" fixed="sender" use="prohibited" />
              <xs:attribute name="className" type="xs:string" fixed="nl.nn.adapterframework.extensions.afm.Afm2EdiFactSender" use="prohibited" />
            </xs:extension>
          </xs:complexContent>
        </xs:complexType>
      </xs:element>
      ...
    </xs:choice>
  </xs:group>
}
 * </pre>
 * 
 * This example shows how an interface-based {@link ElementRole} is used to put
 * an entry in a <code>ChildGroup</code>. A class-based {@link ElementRole} appears just as
 * an element reference, for example:
 * <pre>
 * {@code
<xs:group name="AbstractPipeDeclaredChildGroup">
  <xs:sequence>
    <xs:element ref="Param" minOccurs="0" maxOccurs="unbounded" />
    <xs:element ref="Locker" minOccurs="0" maxOccurs="1" />
    <xs:element ref="Forward" minOccurs="0" maxOccurs="unbounded" />
  </xs:sequence>
</xs:group>
}
 * </pre>
 *
 * <h1>Inheritance of attributes and config children</h1>
 * 
 * In the model, a {@link FrankElement} only holds its declared attributes,
 * but a corresponding top-level &lt;xs:complexType&gt; should allow both the declared
 * attributes and the attributes inherited from the ancestors of the {@link FrankElement}
 * (the inherited attributes). The same holds for configuration children. This similarity
 * appears in the model through the common base class {@link ElementChild}, which is a
 * parent class of both {@link FrankAttribute} and {@link ConfigChild}. An attribute
 * defined high in the class hierarchy of the Frank!Framework can be allowed for many
 * FF! elements, but we do not want to repeat the same &lt;xs:attribute&gt; tags in all
 * these cases. We solve this by grouping the attributes, and the config children,
 * in the XSD, for example:
 * <pre>
 * {@code
<xs:attributeGroup name="FixedResultPipeCumulativeAttributeGroup">
  <xs:attributeGroup ref="FixedResultPipeDeclaredAttributeGroup" />
  <xs:attributeGroup ref="FixedForwardPipeCumulativeAttributeGroup" />
</xs:attributeGroup>
}
 * </pre>
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
 * <h1>Summary of XSD definitions</h1>
 *
 * Here is a summary of all definitions that appear in the XSD:
 * <p>
 * <table>
 *   <tr>
 *     <th style="text-align:left">Kind</th>
 *     <th style="text-align:left">Name suffix</th>
 *     <th style="text-align:left">Explanation</th>
 *   </tr>
 *   <tr>
 *     <td><code>xs:element</code></td>
 *     <td>n/a</td>
 *     <td>Allows the integration specialist to use a tag.</td>
 *   </tr>
 *   <tr>
 *     <td><code>xs:complexType</code>
 *     <td><code>Type</code></td>
 *     <td>Expresses a {@link FrankElement} that can be used in multiple roles.</td>
 *   </tr>
 *   <tr>
 *     <td><code>xs:attributeGroup</code></td>
 *     <td><code>DeclaredAttributeGroup</code></td>
 *     <td>Groups the attributes that a {@link FrankElement} allows, omitting inherited attributes.
 *   </tr>
 *   <tr>
 *     <td><code>xs:attributeGroup</code></td>
 *     <td><code>CumulativeAttributeGroup</code></td>
 *     <td>Groups the attributes that a {@link FrankElement} allows, including inherited attributes.
 *   </tr>
 *   <tr>
 *     <td><code>xs:group</code></td>
 *     <td><code>DeclaredChildGroup</code></td>
 *     <td>Defines a list of tags that is allowed within a parent tag, disregarding inheritance.</td>
 *   </tr>
 *   <tr>
 *     <td><code>xs:group</code></td>
 *     <td><code>CumulativeChildGroup</code></td>
 *     <td>Defines a list of tags that is allowed within a parent tag, including inheritance.</td>
 *   </tr>
 *     <td><code>xs:group</code></td>
 *     <td><code>ElementGroup</code></td>
 *     <td>Lists all choices that are allowed for a child tag.</td>
 *   </tr>
 *   <tr>
 *     <td><code>xs:group</code></td>
 *     <td><code>MemberChildGroup</code></td>
 *     <td>Lists all choices that are allowed for a child tag of a syntax 1 parent tag
 *     (e.g. <code>&lt;Listener className="nl.nn.adapterframework.http.rest.ApiListener"&gt;</code>.</td>
 *   </tr>
 * </table>
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
	private XmlSchemaVersionImpl version;
	private List<XmlBuilder> xsdElements = new ArrayList<>();
	private List<XmlBuilder> xsdComplexItems = new ArrayList<>();
	private Set<String> namesCreatedFrankElements = new HashSet<>();
	private Set<ElementRole.Key> idsCreatedElementGroups = new HashSet<>();
	private Set<String> namesElementTypesWithChildMemberGroup = new HashSet<>();

	public DocWriterNew(FrankDocModel model) {
		this.model = model;
	}

	public void init(XmlSchemaVersion versionTag) {
		init(CONFIGURATION, versionTag);
	}

	void init(String startClassName, XmlSchemaVersion versionTag) {
		this.startClassName = startClassName;
		this.version = versionTag.getStrategy();
		if(log.isTraceEnabled()) {
			log.trace(String.format("Initialized DocWriterNew with start element name [%s]", startClassName));
			log.trace(String.format("Writing version [%s]", versionTag.toString()));
			log.trace(String.format("File name is [%s]", version.getFileName()));
		}
	}

	public String getOutputFileName() {
		return version.getFileName();
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
			log.trace(String.format("Adding cumulative config chidren of FrankElement [%s] to XSD element [%s]", frankElement.getFullName(), xsdElementName));
		}
		frankElement.getCumulativeConfigChildren(version.childSelector(), version.childRejector()).forEach(c -> addConfigChild(sequence, c));
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding cumulative attributes of FrankElement [%s] to XSD element [%s]", frankElement.getFullName(), xsdElementName));
		}
		addAttributeList(complexType, frankElement.getCumulativeAttributes(version.childSelector(), version.childRejector()));
		return complexType;
	}

	private void recursivelyDefineXsdElementType(FrankElement frankElement) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("XML Schema needs type definition (or only groups for config children or attributes) for FrankElement [%s]",
					frankElement == null ? "null" : frankElement.getFullName()));
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
			recursivelyDefineXsdElementType(frankElement.getNextAncestorThatHasConfigChildren(version.childSelector()));
			recursivelyDefineXsdElementType(frankElement.getNextAncestorThatHasAttributes(version.childSelector()));
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
				log.trace(String.format("FrankElement [%s] is not abstract. We really make the XSD type definition", element.getFullName()));
			}
			return new ElementAdder(element);
		}
	}

	private class ElementAdder extends ElementBuildingStrategy {
		private final XmlBuilder complexType;
		private final FrankElement addingTo;

		ElementAdder(FrankElement frankElement) {
			complexType = createComplexType(xsdElementType(frankElement));
			xsdComplexItems.add(complexType);
			this.addingTo = frankElement;
		}

		@Override
		void addGroupRef(String referencedGroupName) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Appending XSD type def of [%s] with reference to XSD group [%s]", addingTo.getFullName(), referencedGroupName));
			}
			DocWriterNewXmlUtils.addGroupRef(complexType, referencedGroupName);
		}

		@Override
		void addAttributeGroupRef(String referencedGroupName) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Appending XSD type def of [%s] with reference to XSD group [%s]", addingTo.getFullName(), referencedGroupName));
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
				ca -> frankElement.walkCumulativeConfigChildren(ca, version.childSelector(), version.childRejector());
		new GroupCreator<ConfigChild>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<ConfigChild>() {
			private XmlBuilder cumulativeBuilder;
			private String cumulativeGroupName;

			@Override
			public List<ConfigChild> getChildrenOf(FrankElement elem) {
				return elem.getConfigChildren(version.childSelector());
			}
			
			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAncestorThatHasConfigChildren(version.childSelector());
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
				frankElement.getConfigChildren(version.childSelector()).forEach(c -> addConfigChild(sequence, c));
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
		String referredXsdElementName = elementInType.getXsdElementName(role);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Config child appears as element reference to FrankElement [%s], XSD element [%s]",
					elementInType.getFullName(), referredXsdElementName));
		}
		addElementRef(context, referredXsdElementName, getMinOccurs(child), getMaxOccurs(child));
		recursivelyDefineXsdElement(elementInType, role);
	}

	private FrankElement singleElementOf(ElementType elementType) {
		return elementType.getMembers().values().iterator().next();
	}

	private void recursivelyDefineXsdElement(FrankElement frankElement, ElementRole role) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("FrankElement [%s] is needed in XML Schema", frankElement.getFullName()));
		}
		if(checkNotDefined(frankElement)) {
			if(log.isTraceEnabled()) {
				log.trace("Not yet defined in XML Schema, going to define it");
			}
			String xsdElementName = frankElement.getXsdElementName(role);
			XmlBuilder attributeBuilder = recursivelyDefineXsdElementUnchecked(frankElement, xsdElementName);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding attributes className and %s for FrankElement [%s]", ELEMENT_GROUP, frankElement.getFullName()));
			}
			addExtraAttributesNotFromModel(attributeBuilder, frankElement, role);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done defining FrankElement [%s], XSD element [%s]", frankElement.getFullName(), xsdElementName));
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
		String elementGroupName = role.createXsdElementName(ELEMENT_GROUP);
		XmlBuilder group = createGroup(elementGroupName);
		xsdComplexItems.add(group);
		XmlBuilder choice = addChoice(group);
		addGenericElementOption(choice, role);
		for(FrankElement frankElement: role.getOptions(version.frankElementFilter())) {
			if(frankElement.isCausesNameConflict()) {
				log.info(String.format("Omitting FrankElement [%s] from element group [%s] to avoid name conflict",
						frankElement.getFullName(), elementGroupName));
			} else {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Append ElementGroup with FrankElement [%s]", frankElement.getFullName()));
				}
				addElementToElementGroup(choice, frankElement, role);
			}
		}		
	}

	private void addGenericElementOption(XmlBuilder choice, ElementRole role) {
		String elementName = role.getGenericOptionElementName();
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding generic element option, XSD element is [%s]", elementName));
		}
		XmlBuilder genericElementOption = addElementWithType(choice, elementName);
		XmlBuilder complexType = addComplexType(genericElementOption);
		addElementTypeChildMembers(complexType, role);
		addAttribute(complexType, ELEMENT_ROLE, FIXED, role.getSyntax1Name(), PROHIBITED);
		addAttribute(complexType, "className", DEFAULT, null, REQUIRED);
		// The XSD is invalid if addAnyAttribute is added before attributes elementType and className.
		addAnyAttribute(complexType);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done adding generic element option, XSD element [%s]", role.getGenericOptionElementName()));
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
		for(ElementRole childRole: new MemberChildrenCalculator(role, model, version).getMemberChildOptions()) {
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
			String referencedXsdGroupName = childRole.createXsdElementName(ELEMENT_GROUP);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Added as group reference to [%s]", referencedXsdGroupName));
			}
			DocWriterNewXmlUtils.addGroupRef(choice, referencedXsdGroupName);
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
				ca -> frankElement.walkCumulativeAttributes(ca, version.childSelector(), version.childRejector());
		new GroupCreator<FrankAttribute>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<FrankAttribute>() {
			private XmlBuilder cumulativeBuilder;
			private String cumulativeGroupName;

			@Override
			public List<FrankAttribute> getChildrenOf(FrankElement elem) {
				return elem.getAttributes(version.childSelector());
			}

			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAncestorThatHasAttributes(version.childSelector());
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
				addAttributeList(attributeGroup, frankElement.getAttributes(version.childSelector()));
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
