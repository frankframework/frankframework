package nl.nn.adapterframework.doc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

public class DocWriterNew {
	private static final String XML_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
	private static final String CONFIGURATION = "nl.nn.adapterframework.configuration.Configuration";

	private static Logger log = LogUtil.getLogger(DocWriterNew.class);

	private FrankDocModel model;
	private Map<String, String> syntax2Names;
	private List<FrankElement> sortedFrankElements;

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
		sortedFrankElements = breadthFirstSort(model.getAllElements(), CONFIGURATION);
		runTimeTest();
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

	static List<FrankElement> breadthFirstSort(Map<String, FrankElement> elements, String sourceFullName) {
		return new FrankElementBreadthFirstSorter(elements).sort(sourceFullName);
	}

	private static class FrankElementBreadthFirstSorter {		
		private Map<String, FrankElement> elements;
		private Set<String> available;
		private Deque<FrankElement> processing;
		private List<FrankElement> result;

		FrankElementBreadthFirstSorter(Map<String, FrankElement> elements) {
			this.elements = elements;
			available = new TreeSet<>(elements.keySet());
			processing = new ArrayDeque<>();
			result = new ArrayList<>();
		}

		List<FrankElement> sort(String sourceFullName) {
			if(! elements.containsKey(sourceFullName)) {
				log.warn(String.format("Cannot sort FrankElements because element [%s] is not known", sourceFullName));
				return null;
			}
			FrankElement source = elements.get(sourceFullName);
			take(source);
			while(! processing.isEmpty()) {
				FrankElement current = processing.removeFirst();
				result.add(current);
				List<ConfigChild> configChildren = new ArrayList<>(current.getConfigChildren());
				configChildren.sort((c1, c2) ->
					Integer.valueOf(c1.getSequenceInConfig()).compareTo(c2.getSequenceInConfig()));
				for(ConfigChild configChild: configChildren) {
					Set<String> candidateFullNames = new TreeSet<>(configChild.getElementType().getMembers().keySet());
					candidateFullNames.retainAll(available);
					candidateFullNames.forEach(n -> take(elements.get(n)));
				}
			}
			result.addAll(available.stream().map(n -> elements.get(n)).collect(Collectors.toList()));
			return result;
		}

		private void take(FrankElement element) {
			processing.addLast(element);
			available.remove(element.getFullName());
		}
	}

	private void runTimeTest() {
		Set<String> originalElementFullNames = new TreeSet<>(model.getAllElements().values().stream()
				.map(FrankElement::getFullName).collect(Collectors.toList()));
		Set<String> sortedNames = new TreeSet<>(sortedFrankElements.stream().map(FrankElement::getFullName).collect(Collectors.toList()));
		if(! sortedNames.equals(originalElementFullNames)) {
			throw new IllegalStateException("Programming error detected, please debug");
		}
	}

	public String getSchema() {
		XmlBuilder schema;
		schema = new XmlBuilder("schema", "xs", XML_SCHEMA_URI);
		schema.addAttribute("xmlns:xs", XML_SCHEMA_URI);
		schema.addAttribute("elementFormDefault", "qualified");
		addElement(schema, "Configuration", "ConfigurationType");
		defineAllTypes(schema);
		return schema.toXML(true);
	}

	private static void addElement(XmlBuilder context, String elementName, String elementType) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("type", elementType);
		context.addSubElement(element);
	}

	private void defineAllTypes(XmlBuilder schema) {
		sortedFrankElements.forEach(elem -> defineXsdType(schema, elem));
	}

	private void defineXsdType(XmlBuilder schema, FrankElement frankElement) {
		XmlBuilder complexType = addComplexType(schema, xsdTypeOf(frankElement));
		List<ConfigChild> configChildren = new ArrayList<>(frankElement.getConfigChildren());
		configChildren.sort(
				(c1, c2) -> new Integer(c1.getSequenceInConfig()).compareTo(new Integer(c2.getSequenceInConfig())));
		addConfigChildren(complexType, configChildren);
		addAttributes(complexType, frankElement);
	}

	private static XmlBuilder addComplexType(XmlBuilder schema, String complexTypeName) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
		complexType.addAttribute("name", complexTypeName);
		schema.addSubElement(complexType);
		return complexType;
	}

	private String xsdTypeOf(FrankElement element) {
		return syntax2Names.get(element.getFullName()) + "Type";
	}

	private void addConfigChildren(XmlBuilder complexType, List<ConfigChild> children) {
		if(children.size() == 0) {
			return;
		}
		XmlBuilder sequence = addSequence(complexType);
		children.forEach(c -> addConfigChild(sequence, c));
	}

	private void addConfigChild(XmlBuilder context, ConfigChild child) {
		List<FrankElement> frankElementOptions = new ArrayList<>(child.getElementType().getMembers().values());
		frankElementOptions.sort((o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName()));
		if(frankElementOptions.size() == 1) {
			FrankElement frankElement = frankElementOptions.get(0);
			String syntax2Name = syntax2Names.get(frankElement.getFullName());
			String xsdType = xsdTypeOf(frankElement);
			addElement(context, syntax2Name, xsdType, getMinOccurs(child), getMaxOccurs(child));
		}
		else {
			XmlBuilder choice = addChoice(context, getMinOccurs(child), getMaxOccurs(child));
			for(FrankElement frankElement: frankElementOptions) {
				String syntax2Name = syntax2Names.get(frankElement.getFullName());
				String xsdType = xsdTypeOf(frankElement);
				addElement(choice, syntax2Name, xsdType);
			}
		}
	}

	private static void addElement(
			XmlBuilder context,
			String elementName,
			String elementType,
			String minOccurs,
			String maxOccurs) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("type", elementType);
		element.addAttribute("minOccurs", minOccurs);
		element.addAttribute("maxOccurs", maxOccurs);
		context.addSubElement(element);
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
	
	private static XmlBuilder addChoice(XmlBuilder context, String minOccurs, String maxOccurs) {
		XmlBuilder choice = new XmlBuilder("choice", "xs", XML_SCHEMA_URI);
		choice.addAttribute("minOccurs", minOccurs);
		choice.addAttribute("maxOccurs", maxOccurs);
		context.addSubElement(choice);
		return choice;
	}

	private XmlBuilder addSequence(XmlBuilder context) {
		XmlBuilder sequence = new XmlBuilder("sequence", "xs", XML_SCHEMA_URI);
		context.addSubElement(sequence);
		return sequence;
	}

	private void addAttributes(XmlBuilder complexType, FrankElement frankElement) {
		List<FrankAttribute> frankAttributes = new ArrayList<>(frankElement.getAttributes());
		frankAttributes.sort((a1, a2) -> new Integer(a1.getOrder()).compareTo(new Integer(a2.getOrder())));
		for(FrankAttribute frankAttribute: frankAttributes) {
			XmlBuilder attribute = addAttribute(complexType, frankAttribute.getName(), frankAttribute.getDefaultValue());
			if(! StringUtils.isEmpty(frankAttribute.getDescription())) {
				addDocumentation(attribute, frankAttribute.getDescription());
			}
		}
	}

	private XmlBuilder addAttribute(XmlBuilder context, String name, String defaultValue) {
		XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
		attribute.addAttribute("name", name);
		attribute.addAttribute("type", "xs:string");
		if(defaultValue != null) {
			attribute.addAttribute("default", defaultValue);
		}
		context.addSubElement(attribute);
		return attribute;
	}

	private void addDocumentation(XmlBuilder context, String description) {
		XmlBuilder annotation = new XmlBuilder("annotation", "xs", XML_SCHEMA_URI);
		context.addSubElement(annotation);
		XmlBuilder documentation = new XmlBuilder("documentation", "xs", XML_SCHEMA_URI);
		annotation.addSubElement(documentation);
		documentation.setValue(description);
	}
}
