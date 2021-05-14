package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.doclet.FrankAnnotation;
import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.util.LogUtil;

class FrankDocGroupFactory {
	private static Logger log = LogUtil.getLogger(FrankDocGroupFactory.class);

	private final Map<String, FrankDocGroup> allGroups = new HashMap<>();

	FrankDocGroup getGroup(FrankClass clazz) {
		FrankDocGroup result = null;
		try {
			FrankAnnotation annotation = clazz.getGroupAnnotation();
			if(annotation == null) {
				result = getGroup((String[]) null);
			} else {
				result = getGroup((String[]) annotation.getValue());
			}
		} catch(FrankDocException e) {
			log.warn("Class [{}] has invalid @IbisDoc: {}", clazz.getName(), e.getMessage());
			return getGroup(FrankDocGroup.GROUP_NAME_OTHER);
		}
		return result;
	}

	// This method can be tested without using real Java classes.
	FrankDocGroup getGroup(String[] annotationValues) throws FrankDocException {
		if(annotationValues == null) {
			return getGroup(FrankDocGroup.GROUP_NAME_OTHER);
		}
		if((annotationValues.length >= 3) || (annotationValues.length == 0)) {
			throw new FrankDocException(String.format("@IbisDoc annotation because invalid length [%d]", annotationValues.length), null);
		}
		if(annotationValues.length == 1) {
			return getGroup(annotationValues[0]);
		}
		int order = Integer.MAX_VALUE;
		try {
			order = Integer.valueOf(annotationValues[0]);
		} catch(NumberFormatException e) {
			throw new FrankDocException(String.format("@IbisDoc annotation has non-numeric order [%d]", annotationValues[0]), null);
		}
		FrankDocGroup result = getGroup(annotationValues[1]);
		result.setOrder(order);
		return result;
	}

	private FrankDocGroup getGroup(String name) {
		if(allGroups.containsKey(name)) {
			return allGroups.get(name);
		} else {
			FrankDocGroup group = new FrankDocGroup(name);
			allGroups.put(name, group);
			return group;
		}
	}

	List<FrankDocGroup> getAllGroups() {
		List<FrankDocGroup> result = new ArrayList<>(allGroups.values());
		Collections.sort(result);
		return result;
	}
}
