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
import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

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
		XmlBuilder element = addElementWithType(xsdRoot, frankElement.getAlias());
		XmlBuilder complexType = addComplexType(element);
		addConfigChildren(complexType, frankElement);
		addAttributes(complexType, frankElement);		
	}

	private void addConfigChildren(final XmlBuilder complexType, FrankElement frankElement) {
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
				addGroupRef(complexType, xsdDeclaredGroupNameForChildren(referee));
			}
			
			@Override
			public void addCumulativeGroupRef(FrankElement referee) {
				addGroupRef(complexType, xsdCumulativeGroupNameForChildren(referee));				
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

	private void addAttributes(XmlBuilder complexType, FrankElement frankElement) {
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
				addAttributeGroupRef(complexType, xsdDeclaredGroupNameForAttributes(referee));
			}

			@Override
			public void addCumulativeGroupRef(FrankElement referee) {
				addAttributeGroupRef(complexType, xsdCumulativeGroupNameForAttributes(referee));				
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
