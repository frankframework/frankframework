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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.doc.model.ConfigChildKey;
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
			List<ConfigChild> configChildren = getNonDeprecatedConfigChildren(element);
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

	private static List<ConfigChild> getNonDeprecatedConfigChildren(FrankElement element) {
		List<ConfigChild> result = element.getConfigChildren().stream()
				.filter(c -> ! c.isDeprecated())
				.collect(Collectors.toList());
		return result;
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
		addConfigChildren(complexType, frankElement);
		addAttributes(complexType, frankElement);		
	}

	private String xsdTypeOf(FrankElement element) {
		return syntax2Names.get(element.getFullName()) + "Type";
	}

	private void addConfigChildren(XmlBuilder complexType, FrankElement frankElement) {
		boolean hasNoConfigChildren = getNonDeprecatedConfigChildren(frankElement).isEmpty();
		FrankElement ancestor = getFirstAncestorWithChildren(frankElement);
		if(hasNoConfigChildren) {
			if(ancestor == null) {
				return;
			}
			else {
				FrankElement superAncestor = getFirstAncestorWithChildren(ancestor);
				if(superAncestor == null) {
					addGroupRef(complexType, xsdDeclaredGroupNameForChildren(ancestor));
				}
				else {
					addGroupRef(complexType, xsdCumulativeGroupNameForChildren(ancestor));
				}
			}
		}
		else {
			if(ancestor == null) {
				addGroupRef(complexType, xsdDeclaredGroupNameForChildren(frankElement));
			}
			else {
				addGroupRef(complexType, xsdCumulativeGroupNameForChildren(frankElement));
				addCumulativeChildGroup(frankElement);
			}
			addDeclaredChildGroup(frankElement);
		}
	}

	private static FrankElement getFirstAncestorWithChildren(FrankElement element) {
		return getFirstAncestorSatisfying(element, elem -> getNonDeprecatedConfigChildren(elem).size() >= 1);
	}

	private static FrankElement getFirstAncestorSatisfying(FrankElement element, Predicate<FrankElement> predicate) {
		FrankElement result = element;
		while(result.getParent() != null) {
			result = result.getParent();
			if(predicate.test(result)) {
				return result;
			}
		}
		return null;
	}

	private static String xsdDeclaredGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "DeclaredChildGroup";
	}

	private static String xsdCumulativeGroupNameForChildren(FrankElement element) {
		return element.getSimpleName() + "CumulativeChildGroup";
	}

	private void addCumulativeChildGroup(FrankElement frankElement) {
		XmlBuilder group = addGroup(xsdRoot, xsdCumulativeGroupNameForChildren(frankElement));
		final XmlBuilder sequence = addSequence(group);
		new OverrideHandler<ConfigChildKey>() {
			@Override
			public FrankElement nextAncestor(FrankElement element) {
				return getFirstAncestorWithChildren(element);
			}

			@Override
			public Map<ConfigChildKey, Boolean> itemsOf(final FrankElement groupOwner) {
				Map<ConfigChildKey, Boolean> result = new HashMap<>();
				for(ConfigChild c: groupOwner.getConfigChildren()) {
					result.put(new ConfigChildKey(c), c.getOverriddenFrom() != null);
				}
				return result;
			}

			@Override
			public void addItemsOf(Set<ConfigChildKey> items, FrankElement itemOwner) {
				List<ConfigChild> children = getNonDeprecatedConfigChildren(itemOwner);
				children = children.stream()
						.filter(c -> items.contains(new ConfigChildKey(c)))
						.collect(Collectors.toList());
				children.sort(
						(c1, c2) -> new Integer(c1.getSequenceInConfig()).compareTo(new Integer(c2.getSequenceInConfig())));
				children.forEach(c -> addConfigChild(sequence, c));
			}

			@Override
			public void addDeclaredGroup(FrankElement groupOwner) {
				addGroupRef(sequence, xsdDeclaredGroupNameForChildren(groupOwner));
			}

			@Override
			public void addCumulativeGroup(FrankElement groupOwner) {
				addGroupRef(sequence, xsdCumulativeGroupNameForChildren(groupOwner));
			}

			@Override
			public void notifyGroupRefRepeated(FrankElement groupOwner) {
				log.info(String.format("Repeating reference to conig child group of [%s] due to overrides in [%s]",
						groupOwner.getFullName(), frankElement.getFullName()));
			}

			@Override
			public void notifyItemsRepeated(FrankElement groupOwner) {
				log.info(String.format("Repeating config children of [%s] due to overrides in [%s]",
						groupOwner.getFullName(), frankElement.getFullName()));				
			}
		}.run(frankElement);
	}

	private void addDeclaredChildGroup(FrankElement frankElement) {
		XmlBuilder group = addGroup(xsdRoot, xsdDeclaredGroupNameForChildren(frankElement));
		XmlBuilder sequence = addSequence(group);
		List<ConfigChild> children = getNonDeprecatedConfigChildren(frankElement);
		children.sort(
				(c1, c2) -> new Integer(c1.getSequenceInConfig()).compareTo(new Integer(c2.getSequenceInConfig())));
		children.forEach(c -> addConfigChild(sequence, c));
	}

	private void addConfigChild(XmlBuilder context, ConfigChild child) {
		addElement(context, xsdFieldName(child), xsdTypeOf(child.getElementType()),
				getMinOccurs(child), getMaxOccurs(child));
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
		boolean hasNoAttributes = getNonDeprecatedAttributes(frankElement).isEmpty();
		FrankElement ancestor = getFirstAncestorWithAttributes(frankElement);
		if(hasNoAttributes) {
			if(ancestor == null) {
				return;
			}
			else {
				FrankElement superAncestor = getFirstAncestorWithAttributes(ancestor);
				if(superAncestor == null) {
					addAttributeGroupRef(complexType, xsdDeclaredGroupNameForAttributes(ancestor));
				}
				else {
					addAttributeGroupRef(complexType, xsdCumulativeGroupNameForAttributes(ancestor));
				}
			}
		}
		else {
			if(ancestor == null) {
				addAttributeGroupRef(complexType, xsdDeclaredGroupNameForAttributes(frankElement));
			}
			else {
				addAttributeGroupRef(complexType, xsdCumulativeGroupNameForAttributes(frankElement));
				addCumulativeAttributeGroup(frankElement);
			}
			addDeclaredAttributeGroup(frankElement);
		}		
	}

	private static List<FrankAttribute> getNonDeprecatedAttributes(FrankElement element) {
		return element.getAttributes().stream()
				.filter(a -> ! a.isDeprecated())
				.collect(Collectors.toList());
	}

	private static FrankElement getFirstAncestorWithAttributes(FrankElement element) {
		return getFirstAncestorSatisfying(element, elem -> getNonDeprecatedAttributes(elem).size() >= 1);
	}

	private static String xsdDeclaredGroupNameForAttributes(FrankElement element) {
		return element.getSimpleName() + "DeclaredAttributeGroup";
	}

	private static String xsdCumulativeGroupNameForAttributes(FrankElement element) {
		return element.getSimpleName() + "CumulativeAttributeGroup";
	}

	private void addCumulativeAttributeGroup(final FrankElement frankElement) {
		final XmlBuilder group = addAttributeGroup(xsdRoot, xsdCumulativeGroupNameForAttributes(frankElement));
		new OverrideHandler<String>() {
			@Override
			public FrankElement nextAncestor(FrankElement attributeOwner) {
				return getFirstAncestorWithAttributes(attributeOwner);
			}

			@Override
			public Map<String, Boolean> itemsOf(FrankElement attributeOwner) {
				Map<String, Boolean> result = new HashMap<>();
				for(FrankAttribute attribute: getNonDeprecatedAttributes(attributeOwner)) {
					result.put(attribute.getName(), attribute.getOverriddenFrom() != null);
				}
				return result;
			}

			@Override
			public void addItemsOf(Set<String> items, FrankElement attributeOwner) {
				List<FrankAttribute> attributes = getNonDeprecatedAttributes(attributeOwner).stream()
						.filter(a -> items.contains(a.getName()))
						.collect(Collectors.toList());
				addAttributeList(group, attributes);
			}

			@Override
			public void addDeclaredGroup(FrankElement attributeOwner) {
				addAttributeGroupRef(group, xsdDeclaredGroupNameForAttributes(attributeOwner));
			}

			@Override
			public void addCumulativeGroup(FrankElement attributeOwner) {
				addAttributeGroupRef(group, xsdCumulativeGroupNameForAttributes(attributeOwner));
			}

			@Override
			public void notifyGroupRefRepeated(FrankElement attributeOwner) {
				log.info(String.format("Repeating reference to attribute group of [%s] due to overrides in [%s]",
						attributeOwner.getFullName(), frankElement.getFullName()));
			}

			@Override
			public void notifyItemsRepeated(FrankElement attributeOwner) {
				log.info(String.format("Repeating attributes of [%s] due to overrides in [%s]",
						attributeOwner.getFullName(), frankElement.getFullName()));				
			}
		}.run(frankElement);
	}

	private void addDeclaredAttributeGroup(FrankElement frankElement) {
		XmlBuilder group = addAttributeGroup(xsdRoot, xsdDeclaredGroupNameForAttributes(frankElement));
		addAttributeList(group, getNonDeprecatedAttributes(frankElement));
	}

	private void addAttributeList(XmlBuilder context, List<FrankAttribute> originalAttributes) {
		List<FrankAttribute> frankAttributes = new ArrayList<>(originalAttributes);
		frankAttributes.sort((a1, a2) -> new Integer(a1.getOrder()).compareTo(new Integer(a2.getOrder())));
		for(FrankAttribute frankAttribute: frankAttributes) {
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
