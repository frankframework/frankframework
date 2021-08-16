package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

abstract class AbstractInterfaceRejector {
	private final Set<String> rejectedInterfaces;

	private static class Inheriteds {
		Set<String> rejects;
		Set<String> retains;
	}

	AbstractInterfaceRejector(Set<String> rejectedInterfaces) {
		this.rejectedInterfaces = rejectedInterfaces;
	}

	abstract Set<String> getAllItems(FrankClass clazz);

	Set<String> getRetains(FrankClass clazz) {
		if(rejectedInterfaces.contains(clazz.getName())) {
			return new HashSet<>();
		} else {
			Inheriteds inheriteds = getInherits(clazz);
			inheriteds.retains.addAll(getAllItems(clazz));
			inheriteds.retains.removeAll(inheriteds.rejects);
			return inheriteds.retains;
		}
	}

	public Set<String> getRejects(FrankClass clazz) {
		if(rejectedInterfaces.contains(clazz.getName())) {
			return getAllItems(clazz);
		} else {
			Inheriteds inheriteds = getInherits(clazz);
			inheriteds.rejects.removeAll(inheriteds.retains);
			return inheriteds.rejects;
		}
	}

	private List<FrankClass> getSuperClassAndInheritedInterfaces(FrankClass clazz) {
		List<FrankClass> result = new ArrayList<>(Arrays.asList(clazz.getInterfaces()));
		if(clazz.getSuperclass() != null) {
			result.add(clazz.getSuperclass());
		}
		return result;
	}

	private Inheriteds getInherits(FrankClass clazz) {
		Inheriteds result = new Inheriteds();
		result.rejects = new HashSet<>();
		result.retains = new HashSet<>();
		for(FrankClass superClazz: getSuperClassAndInheritedInterfaces(clazz)) {
			result.rejects.addAll(getRejects(superClazz));
			result.retains.addAll(getRetains(superClazz));
		}
		return result;
	}
}
