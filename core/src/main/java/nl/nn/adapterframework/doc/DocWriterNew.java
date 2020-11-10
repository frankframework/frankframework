package nl.nn.adapterframework.doc;

import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElement;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.getXmlSchema;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addChoice;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addSequence;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttribute;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addDocumentation;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addGroup;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addGroupRef;

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

	private FrankDocModel model;
	private Map<String, String> syntax2Names;
	List<SortKeyForXsd> xsdSortOrder;
	private XmlBuilder xsdRoot;

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
		xsdSortOrder = breadthFirstSort(CONFIGURATION);
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
			available.forEach(result::add);
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
			List<ConfigChild> configChildren = getNewConfigChildren(element);
			configChildren.sort((c1, c2) -> c1.getSyntax1Name().compareTo(c2.getSyntax1Name()));
			List<SortKeyForXsd> result = configChildren.stream()
					.map(ConfigChild::getElementType)
					.map(SortKeyForXsd::getInstance)
					.collect(Collectors.toList());
			if(isParentSortElementChild(element, result)) {
				result.add(SortKeyForXsd.getInstance(element.getParent()));
			}
			return result;
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

	private static List<ConfigChild> getNewConfigChildren(FrankElement element) {
		return element.getConfigChildren().stream()
				.filter(c -> c.getOverriddenFrom() == null)
				.filter(c -> ! c.isDeprecated())
				.collect(Collectors.toList());
	}

	public String getSchema() {
		xsdRoot = getXmlSchema();
		addElement(xsdRoot, "Configuration", "ConfigurationType");
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
		List<ConfigChild> configChildren = getNewConfigChildren(frankElement);
		configChildren.sort(
				(c1, c2) -> new Integer(c1.getSequenceInConfig()).compareTo(new Integer(c2.getSequenceInConfig())));
		addConfigChildren(complexType, configChildren, getFirstAncestorWithChildren(frankElement));
		addAttributes(complexType, frankElement);		
	}

	private static FrankElement getFirstAncestorWithChildren(FrankElement element) {
		FrankElement result = element;
		while(result.getParent() != null) {
			result = result.getParent();
			if(getNewConfigChildren(result).size() >= 1) {
				return result;
			}
		}
		return null;
	}

	private void addConfigChildren(XmlBuilder complexType, List<ConfigChild> children, FrankElement ancestorWithChildren) {
		if(children.size() == 0) {
			if(ancestorWithChildren == null) {
				return;
			}
			else {
				addGroupRef(complexType, xsdGroupNameForChildren(ancestorWithChildren));
			}
		}
		else {
			FrankElement container = children.get(0).getConfigParent();
			addGroupRef(complexType, xsdGroupNameForChildren(container));
			XmlBuilder group = addGroup(xsdRoot, xsdGroupNameForChildren(container));
			if(ancestorWithChildren == null) {
				XmlBuilder sequence = addSequence(group);
				children.forEach(c -> addConfigChild(sequence, c));
			}
			else {
				XmlBuilder outerSequence = addSequence(group);
				XmlBuilder innerSequence = addSequence(outerSequence);
				children.forEach(c -> addConfigChild(innerSequence, c));
				addGroupRef(outerSequence, xsdGroupNameForChildren(ancestorWithChildren));
			}
		}
	}

	private String xsdGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "ChildGroup";
	}

	private void addConfigChild(XmlBuilder context, ConfigChild child) {
		addElement(context, xsdFieldName(child), xsdTypeOf(child.getElementType()),
				getMinOccurs(child), getMaxOccurs(child));
	}

	private String xsdFieldName(ConfigChild configChild) {
		return InfoBuilderSource.toUpperCamelCase(configChild.getSyntax1Name());
	}

	private String xsdTypeOf(ElementType elementType) {
		return elementType.getSimpleName() + "CombinationType";
	}

	private String getMinOccurs(ConfigChild child) {
		if(child.isMandatory()) {
			return "1";
		} else {
			return "0";
		}
	}

	private String getMaxOccurs(ConfigChild child) {
		if(child.isAllowMultiple()) {
			return "unbounded";
		} else {
			return "1";
		}
	}

	private void addAttributes(XmlBuilder complexType, FrankElement frankElement) {
		List<FrankAttribute> frankAttributes = frankElement.getAttributes().stream()
				.filter(a -> ! a.isDeprecated()).collect(Collectors.toList());
		frankAttributes.sort((a1, a2) -> new Integer(a1.getOrder()).compareTo(new Integer(a2.getOrder())));
		for(FrankAttribute frankAttribute: frankAttributes) {
			XmlBuilder attribute = addAttribute(complexType, frankAttribute.getName(), frankAttribute.getDefaultValue());
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

	private String xsdTypeOf(FrankElement element) {
		return syntax2Names.get(element.getFullName()) + "Type";
	}
}
