package nl.nn.adapterframework.frankdoc.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

class AttributesFromInterfaceRejector extends AbstractInterfaceRejector {
	AttributesFromInterfaceRejector(Set<String> rejectedInterfaces) {
		super(rejectedInterfaces);
	}

	@Override
	Set<String> getAllItems(FrankClass clazz) {
		Map<String, FrankMethod> attributesByName = FrankDocModel.getAttributeToMethodMap(clazz.getDeclaredMethods(), "set");
		return new HashSet<>(attributesByName.keySet());
	}
}
