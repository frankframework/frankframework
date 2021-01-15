package nl.nn.adapterframework.doc;

import java.util.function.Predicate;

import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.FrankElement;

/**
 * Strategy class used by {@link DocWriterNew} to include the the right elements
 * according to the chosen {@link XmlSchemaVersion}. Each strategy that can be chosen
 * appears as a subclass. These subclasses are all in the same file because they can
 * be easily compared that way.
 * @author martijn
 *
 */
abstract class XmlSchemaVersionImpl {
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
	}
}
