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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.model.GenericRole;
import nl.nn.adapterframework.doc.model.XsdVersion;
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

	private static Map<XsdVersion, String> outputFileNames = new HashMap<>();
	static {
		outputFileNames.put(XsdVersion.STRICT, "strict.xsd");
		outputFileNames.put(XsdVersion.COMPATIBILITY, "compatibility.xsd");
	}

	private static final String CONFIGURATION = "nl.nn.adapterframework.configuration.Configuration";
	private static final String ELEMENT_GROUP = "ElementGroup";
	private static final String MULTI_GROUP = "MultiGroup";
	private static final String ELEMENT_GROUP_BASE = "ElementGroupBase";
	private static final String ELEMENT_ROLE = "elementRole";
	
	private FrankDocModel model;
	private String startClassName;
	private XsdVersion version;
	private List<XmlBuilder> xsdElements = new ArrayList<>();
	private List<XmlBuilder> xsdComplexItems = new ArrayList<>();
	private Set<String> namesCreatedFrankElements = new HashSet<>();
	private Set<ElementRole.Key> idsCreatedElementGroups = new HashSet<>();
	private Set<GenericRole.Key> idsGenericRolesAdded = new HashSet<>();

	public DocWriterNew(FrankDocModel model) {
		this.model = model;
	}

	public void init(XsdVersion versionTag) {
		init(CONFIGURATION, versionTag);
	}

	void init(String startClassName, XsdVersion version) {
		this.startClassName = startClassName;
		this.version = version;
		if(log.isTraceEnabled()) {
			log.trace(String.format("Initialized DocWriterNew with start element name [%s]", startClassName));
			log.trace(String.format("Writing version [%s]", version.toString()));
			log.trace(String.format("File name is [%s]", outputFileNames.get(version)));
		}
	}

	public String getOutputFileName() {
		return outputFileNames.get(version);
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
		List<GenericRole> childRoles = model.findOrCreateCumulativeChildren(version, frankElement);
		if(childRoles.stream().anyMatch(gr -> gr.getNumRoles() >= 2)) {
			XmlBuilder sequence = addSequence(complexType);
			XmlBuilder choice = addChoice(sequence, "0", "unbounded");
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding unordered cumulative config children with shared generic element options for FrankElement [%s], XSD element [%s]",
						frankElement.getFullName(), xsdElementName));
			}
			childRoles.forEach(gr -> addGenericRole(choice, gr));
		} else {
			XmlBuilder sequence = addSequence(complexType);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding ordered cumulative config chidren of FrankElement [%s] to XSD element [%s]", frankElement.getFullName(), xsdElementName));
			}
			frankElement.getCumulativeConfigChildren(version.getChildSelector(), version.getChildRejector()).forEach(c -> addConfigChild(sequence, c));			
		}
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding cumulative attributes of FrankElement [%s] to XSD element [%s]", frankElement.getFullName(), xsdElementName));
		}
		addAttributeList(complexType, frankElement.getCumulativeAttributes(version.getChildSelector(), version.getChildRejector()));
		return complexType;
	}

	private void recursivelyDefineXsdElementType(FrankElement frankElement) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("XML Schema needs type definition (or only groups for config children or attributes) for FrankElement [%s]",
					frankElement == null ? "null" : frankElement.getFullName()));
		}
		if(checkNotDefined(frankElement)) {
			ElementBuildingStrategy elementBuildingStrategy = getElementBuildingStrategy(frankElement);
			addConfigChildrenGeneral(elementBuildingStrategy, frankElement);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Visiting attributes for FrankElement [%s]", frankElement.getFullName()));
			}
			addAttributes(elementBuildingStrategy, frankElement);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Creating type definitions (or only groups) for Java ancestors of FrankElement [%s]", frankElement.getFullName()));
			}
			recursivelyDefineXsdElementType(frankElement.getNextAncestorThatHasConfigChildren(version.getChildSelector()));
			recursivelyDefineXsdElementType(frankElement.getNextAncestorThatHasAttributes(version.getChildSelector()));
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done with XSD type definition of FrankElement [%s]", frankElement.getFullName()));
			}
		} else if(log.isTraceEnabled()) {
			log.trace("Type definition was already included");
		}
	}

	private void addConfigChildrenGeneral(ElementBuildingStrategy elementBuildingStrategy, FrankElement frankElement) {
		List<GenericRole> genericRoleChildren = model.findOrCreateCumulativeChildren(version, frankElement);
		if(genericRoleChildren.stream().anyMatch(gr -> gr.getNumRoles() >= 2)) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Writing unordered config children for FrankElement [%s]", frankElement.getFullName()));
			}
			addConfigChildrenUnordered(elementBuildingStrategy, frankElement, genericRoleChildren);
		}
		else {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Visiting config children for FrankElement [%s]", frankElement.getFullName()));
			}
			addConfigChildren(elementBuildingStrategy, frankElement);
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
				ca -> frankElement.walkCumulativeConfigChildren(ca, version.getChildSelector(), version.getChildRejector());
		new GroupCreator<ConfigChild>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<ConfigChild>() {
			private XmlBuilder cumulativeBuilder;
			private String cumulativeGroupName;

			@Override
			public List<ConfigChild> getChildrenOf(FrankElement elem) {
				return elem.getConfigChildren(version.getChildSelector());
			}
			
			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAncestorThatHasConfigChildren(version.getChildSelector());
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
				frankElement.getConfigChildren(version.getChildSelector()).forEach(c -> addConfigChild(sequence, c));
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
		if(role.isSuperseded()) {
			log.warn(String.format("Omitting config child with ElementRole [%s] to avoid conflict", role.toString()));
			return;
		}
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
		GenericRole gr = model.findOrCreate(version, model.findElementRole(child));
		addGenericRole(context, gr);
	}

	private void addGenericRole(XmlBuilder context, GenericRole gr) {
		if((gr.getNumRoles() == 1) && isNoElementTypeNeeded(gr.getRoles().get(0))) {
			addOption(context, gr.getRoles().get(0));
		} else {
			addGenericRoleAsGroup(context, gr);
		}
	}

	private void addGenericRoleAsGroup(XmlBuilder context, GenericRole gr) {
		String groupName = gr.getXsdGroupName(ELEMENT_GROUP, MULTI_GROUP);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Generic role group [%s] requested", groupName));
			log.trace(String.format("Key is [%s]", gr.getKey().toString()));
		}
		DocWriterNewXmlUtils.addGroupRef(context, groupName);
		if(! idsGenericRolesAdded.contains(gr.getKey())) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Defining group for generic role with key [%s]", gr.getKey().toString()));
			}
			idsGenericRolesAdded.add(gr.getKey());
			if(log.isTraceEnabled()) {
				log.trace("Group does not exist yet, creating it");
			}
			XmlBuilder genericRoleGroup = createGroup(gr.getXsdGroupName(ELEMENT_GROUP, MULTI_GROUP));
			xsdComplexItems.add(genericRoleGroup);
			XmlBuilder sequence = addSequence(genericRoleGroup);
			XmlBuilder choice = addChoice(sequence, "0", "unbounded");
			addGenericElementOption(choice, gr);
			List<ElementRole> syntax2Roles = gr.getRoles().stream()
					.filter(role -> ! role.isSuperseded())
					.map(role -> GenericRole.promoteToHighestCommonInterface(role, model))
					.distinct()
					.collect(Collectors.toList());
			for(ElementRole role: syntax2Roles) {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Adding role [%s] to group [%s]", role.toString(), groupName));
				}
				addOption(choice, role);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Done adding role [%s] to group [%s]", role.toString(), groupName));
				}
			}
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done creating generic role group [%s]", groupName));
			}
		} else if(log.isTraceEnabled()) {
			log.trace("Generic role group already exists");
		}
	}

	private void addOption(XmlBuilder context, ElementRole role) {
		if(isNoElementTypeNeeded(role)) {
			FrankElement elementInType = singleElementOf(role.getElementType());
			String referredXsdElementName = elementInType.getXsdElementName(role);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Adding role as a reference to FrankElement [%s], XSD element [%s]",
						elementInType.getFullName(), referredXsdElementName));
			}
			addElementRef(context, referredXsdElementName);
			recursivelyDefineXsdElement(elementInType, role);			
		} else {
			if(log.isTraceEnabled()) {
				log.trace("Adding the role as a group reference");
			}
			DocWriterNewXmlUtils.addGroupRef(context, role.createXsdElementName(ELEMENT_GROUP_BASE));
			defineElementTypeGroup(role);
		}
	}

	private void defineElementTypeGroup(ElementRole role) {
		if(log.isTraceEnabled()) {
			log.trace(String.format("Element group needed for ElementRole [%s]", role.toString()));
		}
		ElementRole.Key key = role.getKey();
		if(! idsCreatedElementGroups.contains(key)) {
			idsCreatedElementGroups.add(key);
			defineElementTypeGroupBase(role);
			if(log.isTraceEnabled()) {
				log.trace(String.format("Done defining ElementGroup for ElementRole [%s]", role.toString()));
			}
		} else if(log.isTraceEnabled()) {
			log.trace("ElementGroup already defined");
		}
	}

	private void defineElementTypeGroupBase(ElementRole role) {
		String elementGroupName = role.createXsdElementName(ELEMENT_GROUP_BASE);
		XmlBuilder group = createGroup(elementGroupName);
		xsdComplexItems.add(group);
		XmlBuilder choice = addChoice(group);
		for(FrankElement frankElement: role.getOptions(version.getElementFilter())) {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Append ElementGroupBase with FrankElement [%s]", frankElement.getFullName()));
			}
			if(frankElement == role.getConflictingElement(version.getElementFilter())) {
				log.info(String.format("Omitting FrankElement [%s] from [%s] because it conflicts with the generic option",
						frankElement.getFullName(), elementGroupName));
			} else {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Adding FrankElement [%s] to [%s]", frankElement, elementGroupName));
				}
				addElementToElementGroup(choice, frankElement, role);
			}
		}				
	}

	private void addGenericElementOption(XmlBuilder choice, GenericRole gr) {
		String elementName = Utils.toUpperCamelCase(gr.getSyntax1Name());
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding generic element option, XSD element is [%s]", elementName));
		}
		XmlBuilder genericElementOption = addElementWithType(choice, elementName);
		XmlBuilder complexType = addComplexType(genericElementOption);
		addElementTypeChildMembers(complexType, gr);
		addAttribute(complexType, ELEMENT_ROLE, FIXED, gr.getSyntax1Name(), PROHIBITED);
		if(gr.getConflictingFrankElement() == null) {
			addAttribute(complexType, "className", DEFAULT, null, REQUIRED);
		} else {
			addAttribute(complexType, "className", DEFAULT, gr.getConflictingFrankElement().getFullName(), OPTIONAL);
		}
		// The XSD is invalid if addAnyAttribute is added before attributes elementType and className.
		addAnyAttribute(complexType);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done adding generic element option, XSD element [%s]", elementName));
		}
	}

	private void addElementTypeChildMembers(XmlBuilder context, GenericRole gr) {
		String groupName = xsdGenericElementOptionGroup(gr);
		if(log.isTraceEnabled()) {
			log.trace(String.format("Adding options to generic option group [%s]", groupName));
		}
		XmlBuilder choice = addChoice(context);
		List<GenericRole> children = model.findOrCreateChildren(version, gr);
		children.forEach(child -> addGenericRole(choice, child));
		if(log.isTraceEnabled()) {
			log.trace(String.format("Done adding options to generic option group [%s]", groupName));
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

	private void addConfigChildrenUnordered(ElementBuildingStrategy elementBuildingStrategy, FrankElement frankElement, List<GenericRole> genericRoleChildren) {
		if(frankElement.getConfigChildren(version.getChildSelector()).isEmpty()) {
			FrankElement parent = frankElement.getNextAncestorThatHasConfigChildren(version.getChildSelector());
			if(log.isTraceEnabled()) {
				log.trace(String.format("Reusing unordered children of [%s]", parent.getFullName()));
			}
			elementBuildingStrategy.addGroupRef(xsdUnorderedGroupNameForChildren(parent));
		} else {
			if(log.isTraceEnabled()) {
				log.trace(String.format("Have new unordered children for FrankElement [%s]", frankElement.getFullName()));
			}
			String groupName = xsdUnorderedGroupNameForChildren(frankElement);
			elementBuildingStrategy.addGroupRef(groupName);
			defineUnorderedGroup(groupName, genericRoleChildren);
		}
	}

	private void defineUnorderedGroup(String groupName, List<GenericRole> groupMembers) {
		XmlBuilder group = createGroup(groupName);
		xsdComplexItems.add(group);
		XmlBuilder sequence = addSequence(group);
		XmlBuilder choice = addChoice(sequence, "0", "unbounded");
		groupMembers.forEach(gr -> addGenericRole(choice, gr));
	}

	private void addAttributes(ElementBuildingStrategy elementBuildingStrategy, FrankElement frankElement) {
		Consumer<GroupCreator.Callback<FrankAttribute>> cumulativeGroupTrigger =
				ca -> frankElement.walkCumulativeAttributes(ca, version.getChildSelector(), version.getChildRejector());
		new GroupCreator<FrankAttribute>(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback<FrankAttribute>() {
			private XmlBuilder cumulativeBuilder;
			private String cumulativeGroupName;

			@Override
			public List<FrankAttribute> getChildrenOf(FrankElement elem) {
				return elem.getAttributes(version.getChildSelector());
			}

			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextAncestorThatHasAttributes(version.getChildSelector());
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
				addAttributeList(attributeGroup, frankElement.getAttributes(version.getChildSelector()));
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

	private static String xsdUnorderedGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "UnorderedChildGroup";
	}

	private String xsdGenericElementOptionGroup(GenericRole gr) {
		return gr.getXsdGroupName("GenericElementOptions", "GenericElementOptions");
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
