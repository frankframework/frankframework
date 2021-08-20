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

package nl.nn.adapterframework.frankdoc.model;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.frankdoc.Utils;
import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocletConstants;
import nl.nn.adapterframework.frankdoc.model.ElementChild.AbstractKey;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.MessageStoreSender;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * Models a Java class that can be referred to in a Frank configuration.
 * <p>
 * A Java class can have a JavaDoc tag @ff.ignoreTypeMembership, with as an argument
 * the full name of a Java interface. Classes with this tag are treaded as follows by the Frank!Doc:
 * <ul>
 * <li> Attributes in the referenced Java interface are not included and they are rejected from
 * inheritance, unless the attribute is also defined from an interface or class that does not inherit
 * from the referenced Java interface.
 * <li> If the referenced Java interface has a FrankDocGroup annotation, then this annotation
 * influences the groups in the Frank!Doc website. The group in the FrankDocGroup annotation
 * is reduced by the annotated class and the derived classes of the annotated class. These
 * classes are supposed to belong to a group that comes from another implemented Java interface. 
 * </ul>
 * 
 * Example: Class {@link MessageStoreSender} extends {@link JdbcTransactionalStorage} which implements
 * {@link ITransactionalStorage}. {@link MessageStoreSender} also implements {@link ISender}.
 * Class {@link MessageStoreSender} should not produce configurable attributes by inheritance from
 * {@link ITransactionalStorage}, also not if there are attribute setter methods in {@link JdbcTransactionalStorage}.
 * But if attribute setters are duplicate in {@link MessageStoreSender} and {@link ISender}, then
 * we want those attributes.
 * <p>
 * Both {@link ITransactionalStorage} and {@link ISender} have a FrankDocGroup annotation to specify
 * the group in the Frank!Doc website. The Frank!Doc website works with a hierarchy of groups
 * that contain types that contain elements. This annotation removes only for the Frank!Doc
 * website {@link MessageStoreSender} and its derived classes from the type {@link ITransactionalStorage}.
 * <p>
 * Please note that you can re-introduce attributes lower in the class inheritance hierarchy if this
 * annotation is applied on a higher level to exclude an attribute. The reason is that omitting
 * attributes is only done on the class that is annotated with this annotation. A derived class that
 * is not annotated is not analyzed for attributes to be omitted.
 * 
 * @author martijn
 *
 */
public class FrankElement implements Comparable<FrankElement> {
	static final String JAVADOC_IGNORE_TYPE_MEMBERSHIP = "@ff.ignoreTypeMembership";

	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private static final Comparator<FrankElement> COMPARATOR =
			Comparator.comparing(FrankElement::getSimpleName).thenComparing(FrankElement::getFullName);

	private static JavadocStrategy javadocStrategy = JavadocStrategy.USE_JAVADOC;

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private final @Getter boolean isAbstract;
	private @Getter boolean isDeprecated = false;

	// True if this FrankElement corresponds to a class that implements a Java interface
	// that we model with an ElementType. This means: The Java interface only counts
	// if it appears as argument type of a config child setter.
	private @Getter @Setter(AccessLevel.PACKAGE) boolean interfaceBased = false;

	// Represents the Java superclass.
	private @Getter FrankElement parent;

	private Map<Class<? extends ElementChild>, LinkedHashMap<? extends AbstractKey, ? extends ElementChild>> allChildren;
	private @Getter List<String> xmlElementNames;
	private @Getter FrankElementStatistics statistics;
	private LinkedHashMap<String, ConfigChildSet> configChildSets;
	private @Getter @Setter String description;
	private @Getter @Setter String descriptionHeader;

	private Set<String> syntax2ExcludedFromTypes = new HashSet<>();

	FrankElement(FrankClass clazz) {
		this(clazz.getName(), clazz.getSimpleName(), clazz.isAbstract());
		isDeprecated = clazz.getAnnotation(FrankDocletConstants.DEPRECATED) != null;
		configChildSets = new LinkedHashMap<>();
		javadocStrategy.completeFrankElement(this, clazz);
		handlePossibleFrankDocIgnoreTypeMembershipAnnotation(clazz);
	}

	private void handlePossibleFrankDocIgnoreTypeMembershipAnnotation(FrankClass clazz) {
		String excludedFromType = clazz.getJavaDocTag(FrankElement.JAVADOC_IGNORE_TYPE_MEMBERSHIP);
		if(excludedFromType != null) {
			if(StringUtils.isBlank(excludedFromType)) {
				log.warn("JavaDoc tag {} should have an argument that is the full name of a Java interface", FrankElement.JAVADOC_IGNORE_TYPE_MEMBERSHIP);
			} else {
				log.trace("FrankElement [{}] has JavaDoc tag {}, excluding from type [{}]",
						() -> fullName, () -> FrankElement.JAVADOC_IGNORE_TYPE_MEMBERSHIP, () -> excludedFromType);
				syntax2ExcludedFromTypes.add(excludedFromType);
			}
		}
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
		this.xmlElementNames = new ArrayList<>();
	}

	public void setParent(FrankElement parent) {
		this.parent = parent;
		if(parent != null) {
			syntax2ExcludedFromTypes.addAll(parent.syntax2ExcludedFromTypes);
		}
		this.statistics = new FrankElementStatistics(this);
	}

	public void addXmlElementName(String elementName) {
		Misc.addToSortedListUnique(xmlElementNames, elementName);
	}

	public void setAttributes(List<FrankAttribute> inputAttributes) {
		setChildrenOfKind(inputAttributes, FrankAttribute.class);
	}

	private <C extends ElementChild> void setChildrenOfKind(List<C> inputChildren, Class<C> kind) {
		LinkedHashMap<AbstractKey, C> children = new LinkedHashMap<>();
		for(C c: inputChildren) {
			if(children.containsKey(c.getKey())) {
				log.warn("Frank element [{}] has multiple attributes / config children with key [{}]",
						() -> fullName, () -> c.getKey().toString());
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
		Class<? extends ElementChild> clazz = elementChild.getClass();
		// We do not have separate lookups for ObjectConfigChild and TextConfigChild.
		// We only have a lookup for ConfigChild.
		if(elementChild instanceof ConfigChild) {
			clazz = ConfigChild.class;
		}
		Map<? extends AbstractKey, ? extends ElementChild> lookup = allChildren.get(clazz);
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
		String postfixToRemove = elementType.getGroupName();
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

	boolean syntax2ExcludedFromType(String typeName) {
		return syntax2ExcludedFromTypes.contains(typeName);
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
