package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.FrankDocIgnoreTypeMembership;
import nl.nn.adapterframework.doc.NoFrankAttribute;
import nl.nn.adapterframework.frankdoc.doclet.FrankAnnotation;
import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.util.LogUtil;

class AttributeNotRealSetter {
	private static Logger log = LogUtil.getLogger(AttributeNotRealSetter.class);

	private Set<String> namesNonAttributesDueToIgnoredInterface = new HashSet<>();

	AttributeNotRealSetter(FrankClass clazz) {
		FrankAnnotation attributeIgnoreAnnotation = clazz.getAnnotation(FrankDocIgnoreTypeMembership.class.getName());
		if(attributeIgnoreAnnotation != null) {
			try {
				String ignoredInterface = (String) attributeIgnoreAnnotation.getValue();
				log.trace("Have annotation {} that refers to interface [{}]", () -> FrankDocIgnoreTypeMembership.class.getName(), () -> ignoredInterface);
				AttributesFromInterfaceRejector rejector = new AttributesFromInterfaceRejector(new HashSet<>(Arrays.asList(ignoredInterface)));
				namesNonAttributesDueToIgnoredInterface = rejector.getRejects(clazz);
			} catch(FrankDocException e) {
				log.warn("Could not parse annotation {}", FrankDocIgnoreTypeMembership.class.getName(), e);
				return;
			}
			if(log.isTraceEnabled()) {
				String namesNonAttributesStr = namesNonAttributesDueToIgnoredInterface.stream().collect(Collectors.joining(", "));
				log.trace("The following will be excluded as attributes: {}", namesNonAttributesStr);
			}
		}
	}

	void updateAttribute(FrankAttribute attribute, FrankMethod method) {
		if(method.getAnnotation(NoFrankAttribute.class.getName()) != null) {
			log.trace("Attribute [{}] has annotation {}, marking as not real", () -> attribute.getName(), () -> NoFrankAttribute.class.getName());
			attribute.setNotReal(true);
		}
		if(namesNonAttributesDueToIgnoredInterface.contains(attribute.getName())) {
			log.trace("Attribute [{}] is non-real because it belongs to an excluded interface", () -> attribute.getName());
			attribute.setNotReal(true);
		}
		namesNonAttributesDueToIgnoredInterface.remove(attribute.getName());
	}

	List<FrankAttribute> getFakeNonRealAttributesForRemainingNames(FrankElement attributeOwner) {
		List<FrankAttribute> result = new ArrayList<>();
		for(String name: namesNonAttributesDueToIgnoredInterface) {
			FrankAttribute a = new FrankAttribute(name, attributeOwner);
			a.setNotReal(true);
			result.add(a);
			log.trace("Created fake not-real attribute [{}]", () -> a.getName());
		}
		return result;
	}
}
