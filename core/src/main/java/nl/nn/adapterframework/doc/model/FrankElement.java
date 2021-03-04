/* 
Copyright 2020, 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

package nl.nn.adapterframework.doc.model;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.doc.model.ElementChild.AbstractKey;
import nl.nn.adapterframework.util.LogUtil;

public class FrankElement implements Comparable<FrankElement> {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private static final Comparator<FrankElement> COMPARATOR =
			Comparator.comparing(FrankElement::getSimpleName).thenComparing(FrankElement::getFullName);

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private final @Getter boolean isAbstract;
	private @Getter boolean isDeprecated = false;

	// Represents the Java superclass.
	private @Getter FrankElement parent;

	private Map<Class<? extends ElementChild>, LinkedHashMap<? extends AbstractKey, ? extends ElementChild>> allChildren;
	private @Getter FrankElementStatistics statistics;
	private LinkedHashMap<String, ConfigChildSet> configChildSets;

	FrankElement(Class<?> clazz) {
		this(clazz.getName(), clazz.getSimpleName(), Modifier.isAbstract(clazz.getModifiers()));
		isDeprecated = clazz.getAnnotation(Deprecated.class) != null;
		configChildSets = new LinkedHashMap<>();
	}

	/**
	 * Constructor for testing purposes. We want to test attribute construction in isolation,
	 * in which case we do not have a parent.
	 * TODO: Reorganize files such that this test constructor need not be public.
	 */
	public FrankElement(final String fullName, final String simpleName, boolean isAbstract) {
		this.fullName = fullName;
		this.simpleName = simpleName;
		this.isAbstract = isAbstract;
		this.allChildren = new HashMap<>();
		this.allChildren.put(FrankAttribute.class, new LinkedHashMap<>());
		this.allChildren.put(ConfigChild.class, new LinkedHashMap<>());
	}

	public void setParent(FrankElement parent) {
		this.parent = parent;
		this.statistics = new FrankElementStatistics(this);
	}

	public void setAttributes(List<FrankAttribute> inputAttributes) {
		setChildrenOfKind(inputAttributes, FrankAttribute.class);
	}

	private <C extends ElementChild> void setChildrenOfKind(List<C> inputChildren, Class<C> kind) {
		LinkedHashMap<AbstractKey, C> children = new LinkedHashMap<>();
		for(C c: inputChildren) {
			if(children.containsKey(c.getKey())) {
				log.warn(String.format("Frank element [%s] has multiple attributes / config children with key [%s]",
						fullName, c.getKey().toString()));
			} else {
				children.put(c.getKey(), c);
			}
		}
		allChildren.put(kind, children);
	}

	public List<FrankAttribute> getAttributes(Predicate<ElementChild> filter) {
		return getChildrenOfKind(filter, FrankAttribute.class);
	}

	@SuppressWarnings("unchecked")
	public <T extends ElementChild> List<T> getChildrenOfKind(Predicate<ElementChild> selector, Class<T> kind) {
		Map<? extends AbstractKey, ? extends ElementChild> lookup = allChildren.get(kind);
		return lookup.values().stream().filter(selector).map(c -> (T) c).collect(Collectors.toList());
	}

	public void setConfigChildren(List<ConfigChild> children) {
		setChildrenOfKind(children, ConfigChild.class);
	}

	public List<ConfigChild> getConfigChildren(Predicate<ElementChild> filter) {
		return getChildrenOfKind(filter, ConfigChild.class);
	}

	<C extends ElementChild> ElementChild findElementChildMatch(C elementChild) {
		Map<? extends AbstractKey, ? extends ElementChild> lookup = allChildren.get(elementChild.getClass());
		return lookup.get(elementChild.getKey());
	}

	public FrankElement getNextAncestorThatHasChildren(Predicate<FrankElement> noChildren) {
		FrankElement ancestor = parent;
		while((ancestor != null) && noChildren.test(ancestor)) {
			ancestor = ancestor.getParent();
		}
		return ancestor;
	}

	public boolean hasAncestorThatHasConfigChildrenOrAttributes(Predicate<ElementChild> selector) {
		FrankElement ancestorAttributes = getNextAncestorThatHasAttributes(selector);
		FrankElement ancestorConfigChildren = getNextAncestorThatHasConfigChildren(selector);
		return (ancestorAttributes != null) || (ancestorConfigChildren != null);
	}

	public FrankElement getNextAncestorThatHasConfigChildren(Predicate<ElementChild> selector) {
		FrankElement ancestorConfigChildren = getNextAncestorThatHasChildren(el -> el.getChildrenOfKind(selector, ConfigChild.class).isEmpty());
		return ancestorConfigChildren;
	}

	public FrankElement getNextAncestorThatHasAttributes(Predicate<ElementChild> selector) {
		FrankElement ancestorAttributes = getNextAncestorThatHasChildren(el -> el.getChildrenOfKind(selector, FrankAttribute.class).isEmpty());
		return ancestorAttributes;
	}

	public void walkCumulativeAttributes(
			CumulativeChildHandler<FrankAttribute> handler, Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector) {
		new AncestorChildNavigation<FrankAttribute>(
				handler, childSelector, childRejector, FrankAttribute.class).run(this);
	}

	public void walkCumulativeConfigChildren(
			CumulativeChildHandler<ConfigChild> handler, Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector) {
		new AncestorChildNavigation<ConfigChild>(
				handler, childSelector, childRejector, ConfigChild.class).run(this);		
	}

	public List<ConfigChild> getCumulativeConfigChildren(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		return getCumulativeChildren(selector, rejector, ConfigChild.class).stream()
				.map(c -> (ConfigChild) c).collect(Collectors.toList());
	}

	public List<FrankAttribute> getCumulativeAttributes(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		return getCumulativeChildren(selector, rejector, FrankAttribute.class).stream()
				.map(c -> (FrankAttribute) c).collect(Collectors.toList());
	}

	private <T extends ElementChild> List<T> getCumulativeChildren(Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Class<T> kind) {
		final List<T> result = new ArrayList<>();
		new AncestorChildNavigation<T>(new CumulativeChildHandler<T>() {
			@Override
			public void handleSelectedChildren(List<T> children, FrankElement owner) {
				result.addAll(children);
			}

			@Override
			public void handleChildrenOf(FrankElement frankElement) {
				result.addAll(frankElement.getChildrenOfKind(selector, kind));
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement frankElement) {
				result.addAll(frankElement.getCumulativeChildren(selector, rejector, kind));
			}
		}, selector, rejector, kind).run(this);
		return result;
	}

	public String getXsdElementName(ElementRole elementRole) {
		return getXsdElementName(elementRole.getElementType(), elementRole.getRoleName());
	}

	String getXsdElementName(ElementType elementType, String roleName) {
		if(! elementType.isFromJavaInterface()) {
			return Utils.toUpperCamelCase(roleName);
		}
		String postfixToRemove = elementType.getSimpleName();
		if(postfixToRemove.startsWith("I")) {
			postfixToRemove = postfixToRemove.substring(1);
		}
		String result = simpleName;
		if(result.endsWith(postfixToRemove)) {
			result = result.substring(0, result.lastIndexOf(postfixToRemove));
		}
		result = result + Utils.toUpperCamelCase(roleName);
		return result;
	}

	void addConfigChildSet(ConfigChildSet configChildSet) {
		configChildSets.put(configChildSet.getRoleName(), configChildSet);
	}

	public ConfigChildSet getConfigChildSet(String roleName) {
		return configChildSets.get(roleName);
	}

	public List<ConfigChildSet> getCumulativeConfigChildSets() {
		Map<String, ConfigChildSet> resultAsMap = new HashMap<>();
		for(String roleName: configChildSets.keySet()) {
			resultAsMap.put(roleName, configChildSets.get(roleName));
		}
		if(parent != null) {
			List<ConfigChildSet> inheritedConfigChildSets = getParent().getCumulativeConfigChildSets();
			for(ConfigChildSet inherited: inheritedConfigChildSets) {
				resultAsMap.putIfAbsent(inherited.getRoleName(), inherited);
			}
		}
		List<ConfigChildSet> result = new ArrayList<>();
		List<String> keys = new ArrayList<>(resultAsMap.keySet());
		Collections.sort(keys);
		for(String key: keys) {
			result.add(resultAsMap.get(key));
		}
		return result;
	}

	public boolean hasFilledConfigChildSets(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		if(configChildSets.isEmpty()) {
			return false;
		}
		return configChildSets.values().stream()
				.anyMatch(cs -> cs.getConfigChildren().stream().filter(selector.or(rejector)).collect(Collectors.counting()) >= 1);
	}

	public FrankElement getNextPluralConfigChildrenAncestor(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		FrankElement ancestor = parent;
		while(ancestor != null) {
			if(! ancestor.getParent().hasOrInheritsPluralConfigChildren(selector, rejector)) {
				return ancestor;
			}
			if(ancestor.hasFilledConfigChildSets(selector, rejector)) {
				return ancestor;
			}
			ancestor = ancestor.getParent();
		}
		return null;
	}

	public boolean hasOrInheritsPluralConfigChildren(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		boolean hasPluralConfigChildren = configChildSets.values().stream()
				.anyMatch(c -> c.getFilteredElementRoles(selector, rejector).size() >= 2);
		boolean inheritsPluralConfigChildren = false;
		FrankElement ancestor = getNextAncestorThatHasConfigChildren(selector);
		if(ancestor != null) {
			inheritsPluralConfigChildren = ancestor.hasOrInheritsPluralConfigChildren(selector, rejector);
		}
		return hasPluralConfigChildren || inheritsPluralConfigChildren;
	}

	@Override
	public int compareTo(FrankElement other) {
		return COMPARATOR.compare(this, other);
	}

	static String describe(Collection<FrankElement> collection) {
		return collection.stream().map(FrankElement::getFullName).collect(Collectors.joining(", "));
	}

	static Set<FrankElement> join(Set<FrankElement> s1, Set<FrankElement> s2) {
		Set<FrankElement> result = new HashSet<>();
		result.addAll(s1);
		result.addAll(s2);
		return result;
	}
}
