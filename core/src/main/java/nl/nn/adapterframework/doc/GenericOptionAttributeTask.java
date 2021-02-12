package nl.nn.adapterframework.doc;

import java.util.Set;

import lombok.Getter;
import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.util.XmlBuilder;

class GenericOptionAttributeTask {
	private final @Getter Set<ElementRole.Key> rolesKey;
	private final @Getter XmlBuilder builder;

	GenericOptionAttributeTask(Set<ElementRole.Key> rolesKey, XmlBuilder builder) {
		this.rolesKey = rolesKey;
		this.builder = builder;
	}
}
