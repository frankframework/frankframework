package nl.nn.adapterframework.frankdoc.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

public class GroupFromInterfaceRejector extends AbstractInterfaceRejector {
	public GroupFromInterfaceRejector(Set<String> rejectedInterfaces) {
		super(rejectedInterfaces);
	}

	@Override
	Set<String> getAllItems(FrankClass clazz) {
		return new HashSet<>(Arrays.asList(clazz.getName()));
	}
}
