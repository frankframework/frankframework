package nl.nn.adapterframework.doc;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Enum to decide what version of the XML Schema file for Frank developers should be written.
 */
public enum XmlSchemaVersion {
	/**
	 * Write <code>strict.xsd</code>, the version that only allows non-deprecated elements.
	 */
	STRICT(XmlSchemaVersionImpl.getInstanceStrict()),
	
	/**
	 * Write <code>compatibility.xsd</code>, the version that also allows deprecated elements.
	 */
	COMPATIBILITY(XmlSchemaVersionImpl.getInstanceCompatibility());

	private @Getter(AccessLevel.PACKAGE) XmlSchemaVersionImpl strategy;

	XmlSchemaVersion(XmlSchemaVersionImpl strategy) {
		this.strategy = strategy;
	}
}
