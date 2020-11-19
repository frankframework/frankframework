package nl.nn.adapterframework.doc;

import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttribute;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttributeGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttributeGroupRef;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addChoice;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addDocumentation;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElement;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addGroupRef;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addSequence;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.getXmlSchema;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public class DocWriterNew {
	private static final String CONFIGURATION = "nl.nn.adapterframework.configuration.Configuration";

	private static Logger log = LogUtil.getLogger(DocWriterNew.class);

	private FrankDocModel model;
	private Map<String, String> syntax2Names;
	List<SortKeyForXsd> xsdSortOrder;
	private XmlBuilder xsdRoot;
	private String startClassName;

	public DocWriterNew(FrankDocModel model) {
		this.model = model;
		Map<String, List<FrankElement>> simpleNamePartitions = model.getAllElements().values().stream()
				.collect(Collectors.groupingBy(
						FrankElement::getSimpleName,
						Collectors.toList()));
		syntax2Names = new HashMap<>();
		for(List<FrankElement> partition: simpleNamePartitions.values()) {
			syntax2Names.putAll(chooseSyntax2Names(partition));
		}
	}

	public void init() {
		init(CONFIGURATION);
	}

	public void init(String startClassName) {
		this.startClassName = startClassName;
		xsdSortOrder = breadthFirstSort(startClassName);
	}

	static Map<String, String> chooseSyntax2Names(List<FrankElement> elementPartition) {
		Map<String, String> result = new HashMap<>();
		if(elementPartition.size() == 1) {
			FrankElement theElement = elementPartition.get(0);
			result.put(theElement.getFullName(), theElement.getSimpleName());
			return result;
		}
		else {
			List<List<String>> nameComponents = new ArrayList<>();
			for(FrankElement element: elementPartition) {
				List<String> packageNameComponents = Arrays.asList(element.getFullName().split("\\.", -1));
				if(! element.getSimpleName().equals(packageNameComponents.get(packageNameComponents.size() - 1))) {
					log.warn(String.format("Syntax 2 names may be wrong because there is a FrankElement with full name [%s] but simple name [%s]",
							element.getFullName(), element.getSimpleName()));
				}
				nameComponents.add(packageNameComponents);
			}
			List<String> fullNames = elementPartition.stream().map(elem -> elem.getFullName()).collect(Collectors.toList());
			int numComponents = 2;
			while(true) {
				result = chooseSyntax2Names(fullNames, nameComponents, numComponents);
				Map<String, Long> multiplicities = result.values().stream()
						.collect(Collectors.groupingBy(s -> s, Collectors.counting()));
				Optional<Long> maxMultiplicity = multiplicities.values().stream().max((l1, l2) -> l1.compareTo(l2));
				if(maxMultiplicity.get() == 1) {
					break;
				}
			}
			return result;
		}
	}

	private static Map<String, String> chooseSyntax2Names(List<String> fullNames, List<List<String>> nameComponents, int numUsed) {
		Map<String, String> result = new HashMap<>();
		for(int i = 0; i < fullNames.size(); i++) {
			result.put(fullNames.get(i), getSyntax2Name(nameComponents.get(i), numUsed));
		}
		return result;
	}

	private static String getSyntax2Name(List<String> components, int numUsed) {
		List<String> items = new ArrayList<>(components);
		Collections.reverse(items);
		items = items.subList(0, Math.min(items.size(), numUsed));
		Collections.reverse(items);
		return items.stream().map(InfoBuilderSource::toUpperCamelCase).collect(Collectors.joining(""));
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
			members.sort((c1, c2) -> syntax2Names.get(c1.getFullName())
					.compareTo(syntax2Names.get(c2.getFullName())));
			return members.stream().map(SortKeyForXsd::getInstance).collect(Collectors.toList());
		}

		private List<SortKeyForXsd> getSortedElementChildren(String elementName) {
			FrankElement element = model.getAllElements().get(elementName);
			List<ConfigChild> configChildren = element.getSelectedChildren(FrankElement::getConfigChildren);
			configChildren.sort((c1, c2) -> c1.getSyntax1Name().compareTo(c2.getSyntax1Name()));
			List<SortKeyForXsd> result = configChildren.stream()
					.map(c -> getTypeKey(c))
					.collect(Collectors.toList());
			if(isParentSortElementChild(element, result)) {
				result.add(SortKeyForXsd.getInstance(element.getParent()));
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

		private boolean isParentSortElementChild(FrankElement container, List<SortKeyForXsd> otherChildren) {
			FrankElement parent = container.getParent();
			if(parent == null) {
				return false;
			}
			Set<String> otherConfigChildNames = otherChildren.stream()
					.map(SortKeyForXsd::getName).collect(Collectors.toSet());
			return ! otherConfigChildNames.contains(parent.getFullName());
		}
	}


	public String getSchema() {
		xsdRoot = getXmlSchema();
		FrankElement rootElement = model.getAllElements().get(startClassName);
		addElement(xsdRoot, rootElement.getSimpleName(), xsdTypeOf(rootElement));
		defineAllTypes();
		return xsdRoot.toXML(true);
	}

	private void defineAllTypes() {
		for(SortKeyForXsd item: xsdSortOrder) {
			switch(item.getKind()) {
			case ELEMENT:
				defineElementType(model.getAllElements().get(item.getName()));
				break;
			case TYPE:
				defineTypeType(model.getAllTypes().get(item.getName()));
				break;
			}
		}
	}

	private void defineElementType(FrankElement frankElement) {
		XmlBuilder complexType = addComplexType(xsdRoot, xsdTypeOf(frankElement));
		addConfigChildren(complexType, frankElement);
		addAttributes(complexType, frankElement);		
	}

	private String xsdTypeOf(FrankElement element) {
		return syntax2Names.get(element.getFullName()) + "Type";
	}

	private void addConfigChildren(final XmlBuilder complexType, FrankElement frankElement) {
		Consumer<GroupCreator.Callback> cumulativeGroupTrigger = ca -> frankElement.walkSelectedCumulativeConfigChildren(ca);
		new GroupCreator(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback() {
			private XmlBuilder cumulativeBuilder;
			
			@Override
			public List<? extends ElementChild> getChildrenOf(FrankElement elem) {
				return elem.getSelectedChildren(FrankElement::getConfigChildren);
			}
			
			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextSelectedConfigChildAncestor();
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
				frankElement.getSelectedChildren(FrankElement::getConfigChildren).forEach(
						c -> addConfigChild(sequence, c));
			}

			@Override
			public void addCumulativeGroup() {
				XmlBuilder group = addGroup(xsdRoot, xsdCumulativeGroupNameForChildren(frankElement));
				cumulativeBuilder = addSequence(group);
			}

			@Override
			public void handleSelectedChildren(List<? extends ElementChild> children, FrankElement owner) {
				children.forEach(c -> addConfigChild(cumulativeBuilder, (ConfigChild) c));
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
		return element.getSimpleName() + "DeclaredChildGroup";
	}

	private static String xsdCumulativeGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "CumulativeChildGroup";
	}

	private void addConfigChild(XmlBuilder context, ConfigChild child) {
		ElementType elementType = child.getElementType();
		if(elementType.isFromJavaInterface()) {
			addElement(context, xsdFieldName(child), xsdTypeOf(elementType),
					getMinOccurs(child), getMaxOccurs(child));
		}
		else {
			FrankElement containedFrankElement = elementType.getMembers().values().iterator().next();
			addElement(context, xsdFieldName(child), xsdTypeOf(containedFrankElement),
					getMinOccurs(child), getMaxOccurs(child));
		}
	}

	private static String xsdFieldName(ConfigChild configChild) {
		return InfoBuilderSource.toUpperCamelCase(configChild.getSyntax1Name());
	}

	private static String xsdTypeOf(ElementType elementType) {
		return elementType.getSimpleName() + "CombinationType";
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
		Consumer<GroupCreator.Callback> cumulativeGroupTrigger = ca -> frankElement.walkSelectedCumulativeAttributes(ca);
		new GroupCreator(frankElement, cumulativeGroupTrigger, new GroupCreator.Callback() {
			private XmlBuilder cumulativeBuilder;

			@Override
			public List<? extends ElementChild> getChildrenOf(FrankElement elem) {
				return elem.getSelectedChildren(FrankElement::getAttributes);
			}

			@Override
			public FrankElement getAncestorOf(FrankElement elem) {
				return elem.getNextSelectedAttributeAncestor();
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
				addAttributeList(attributeGroup, frankElement.getSelectedChildren(FrankElement::getAttributes));
			}

			@Override
			public void addCumulativeGroup() {
				cumulativeBuilder = addAttributeGroup(xsdRoot, xsdCumulativeGroupNameForAttributes(frankElement));				
			}

			@Override
			public void handleSelectedChildren(List<? extends ElementChild> children, FrankElement owner) {
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
		return element.getSimpleName() + "DeclaredAttributeGroup";
	}

	private static String xsdCumulativeGroupNameForAttributes(FrankElement element) {
		return element.getSimpleName() + "CumulativeAttributeGroup";
	}

	private void addAttributeList(XmlBuilder context, List<? extends ElementChild> originalAttributes) {
		List<ElementChild> frankAttributes = new ArrayList<>(originalAttributes);
		for(ElementChild child: frankAttributes) {
			FrankAttribute frankAttribute = (FrankAttribute) child;
			XmlBuilder attribute = addAttribute(context, frankAttribute.getName(), frankAttribute.getDefaultValue());
			if(! StringUtils.isEmpty(frankAttribute.getDescription())) {
				addDocumentation(attribute, frankAttribute.getDescription());
			}
		}		
	}

	private void defineTypeType(ElementType elementType) {
		XmlBuilder complexType = addComplexType(xsdRoot, xsdTypeOf(elementType));
		XmlBuilder choice = addChoice(complexType);
		List<FrankElement> frankElementOptions = new ArrayList<>(elementType.getMembers().values());
		frankElementOptions.sort((o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName()));
		for(FrankElement frankElement: frankElementOptions) {
			String syntax2Name = syntax2Names.get(frankElement.getFullName());
			String xsdType = xsdTypeOf(frankElement);
			addElement(choice, syntax2Name, xsdType);
		}		
	}
}
