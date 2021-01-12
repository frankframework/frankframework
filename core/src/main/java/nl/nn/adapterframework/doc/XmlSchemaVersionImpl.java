package nl.nn.adapterframework.doc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Strategy class used by {@link DocWriterNew} to include the the right elements
 * according to the chosen {@link XmlSchemaVersion}. Each strategy that can be chosen
 * appears as a subclass. These subclasses are all in the same file because they can
 * be easily compared that way.
 * @author martijn
 *
 */
abstract class XmlSchemaVersionImpl {
	private static Logger log = LogUtil.getLogger(XmlSchemaVersionImpl.class);

	private static final String STRICT = "strict.xsd";
	private static final String COMPATIBILITY = "compatibility.xsd";
	private static final Predicate<FrankElement> FIXED_FRANK_ELEMENT_FILTER = f -> ! f.isDeprecated();
	private static final Predicate<FrankElement> COMPATIBILITY_FRANK_ELEMENT_FILTER = f -> true;

	static XmlSchemaVersionImpl getInstanceStrict() {
		return new Strict();
	}

	static XmlSchemaVersionImpl getInstanceCompatibility() {
		return new Compatibility();
	}

	abstract String getFileName();
	abstract Predicate<ElementChild> childSelector();
	abstract Predicate<ElementChild> childRejector();
	abstract Predicate<FrankElement> frankElementFilter();
	abstract String genericOptionElementName(ElementRole role);

	List<FrankElement> getOptionsOfRole(ElementRole role) {
		return getOptionsOfRole(role, frankElementFilter());
	}

	List<FrankElement> getOptionsOfRole(ElementRole role, Predicate<FrankElement> frankElementFilter) {
		List<FrankElement> frankElementOptions = role.getElementType().getMembers().values().stream()
				.filter(frankElementFilter)
				.filter(f -> ! f.isAbstract())
				.collect(Collectors.toList());
		frankElementOptions.sort((o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName()));
		return frankElementOptions;
	}

	final String genericOptionElementNameBase(ElementRole role, List<FrankElement> membersToInclude) {
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

	// The boolean result is only used for unit testing
	boolean checkGenericOptionElementEqualsFixedCompatibility(ElementRole role) {
		String elementNameStrict = genericOptionElementNameBase(role, getOptionsOfRole(role, FIXED_FRANK_ELEMENT_FILTER));
		String elementNameCompatibility = genericOptionElementNameBase(role, getOptionsOfRole(role, COMPATIBILITY_FRANK_ELEMENT_FILTER));
		boolean result = elementNameCompatibility.equals(elementNameStrict);
		if(! result) {
			log.warn(String.format("XML schema file %s and %s have a different generic element option tag for role [%s]: [%s]",
					STRICT, COMPATIBILITY, role.toString(),
					Arrays.asList(elementNameStrict, elementNameCompatibility).stream().collect(Collectors.joining(", "))));
		}
		return result;
	}

	private static class Strict extends XmlSchemaVersionImpl {
		@Override
		String getFileName() {
			return STRICT;
		}

		@Override
		Predicate<ElementChild> childSelector() {
			return ElementChild.IN_XSD;
		}

		@Override
		Predicate<ElementChild> childRejector() {
			return ElementChild.DEPRECATED;
		}

		@Override
		Predicate<FrankElement> frankElementFilter() {
			return FIXED_FRANK_ELEMENT_FILTER;
		}

		@Override
		String genericOptionElementName(ElementRole role) {
			return genericOptionElementNameBase(role, getOptionsOfRole(role));
		}
	}

	private static class Compatibility extends XmlSchemaVersionImpl {
		@Override
		String getFileName() {
			return COMPATIBILITY;
		}

		@Override
		Predicate<ElementChild> childSelector() {
			return ElementChild.IN_COMPATIBILITY_XSD;
		}

		@Override
		Predicate<ElementChild> childRejector() {
			return ElementChild.NONE;
		}

		@Override
		Predicate<FrankElement> frankElementFilter() {
			return COMPATIBILITY_FRANK_ELEMENT_FILTER;
		}

		@Override
		String genericOptionElementName(ElementRole role) {
			checkGenericOptionElementEqualsFixedCompatibility(role);
			return genericOptionElementNameBase(role, getOptionsOfRole(role));
		}
	}
}
