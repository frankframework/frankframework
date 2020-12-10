/* 
Copyright 2020 WeAreFrank! 

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.doc.model.ElementChild.AbstractKey;
import nl.nn.adapterframework.util.LogUtil;

public class FrankElement {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private final @Getter boolean isAbstract;
	private @Getter boolean isDeprecated = false;

	// Represents the Java superclass.
	private @Getter FrankElement parent;

	private ElementType creatingElementType;

	/**
	 * If this property is true and if there is no relevant ancestor, then
	 * this {@link FrankElement} can be created without a type definition in
	 * the XSD, allowing for simpler content there. In this case there is
	 * no need to define child groups or attribute groups.
	 */
	public boolean isInElementTypeFromJavaInterface() {
		return creatingElementType == null ? false : creatingElementType.isFromJavaInterface();
	}

	/**
	 * Sets the creating element type if it was null, or otherwise
	 * verify that both the existing and the new element type model
	 * a Java interface or none of the two. See also the constructors.
	 */
	void registerCreatingElementTypeOrCheckConflict(ElementType otherElementType) {
		if(otherElementType == null) {
			// Can occur if a FrankElement is first created with a non-null
			// creatingElementType and appears later as the describing element
			// of some attribute. In that case, we should not cancel the
			// creatingElementType.
			return;
		}
		if(creatingElementType == null) {
			// Can occur if a FrankElement first appears as the describing element
			// of some attribute, and is later assigned to a non-null element type.
			creatingElementType = otherElementType;
			return;
		}
		if(isInElementTypeFromJavaInterface() != otherElementType.isFromJavaInterface()) {
			log.warn(String.format(
					"Conflict about isInElementType for FrankElement [%s], conflicting types are [%s] and [%s]",
					simpleName,
					this.creatingElementType.getFullName(),
					otherElementType.getFullName()));
		}
	}

	private Map<Class<? extends ElementChild>, LinkedHashMap<? extends AbstractKey, ? extends ElementChild>> allChildren;
	private @Getter FrankElementStatistics statistics;

	/**
	 * @param clazz The Java class being modeled by this {@link FrankElement}.
	 * @param creatingElementType An ElementType to which this object belongs,
	 * or null. The value null should be passed in the following cases:
	 * <ul>
	 * <li> This object is the first {@link FrankElement} being created (the root).
	 * <li> This object is first created as the describing element of an attribute.
	 * </ul>
	 * In theory, there can be multiple {@link ElementType} objects to which this
	 * object belongs. This is not a problem, because it is only relevant whether
	 * the {@link ElementType} comes from a Java interface or not.
	 */
	FrankElement(Class<?> clazz, ElementType creatingElementType) {
		this(clazz.getName(), clazz.getSimpleName(), Modifier.isAbstract(clazz.getModifiers()), creatingElementType);
		isDeprecated = clazz.getAnnotation(Deprecated.class) != null;
	}

	/**
	 * Constructor for testing purposes. We want to test attribute construction in isolation,
	 * in which case we do not have a parent.
	 * TODO: Reorganize files such that this test constructor need not be public.
	 */
	private FrankElement(final String fullName, final String simpleName, boolean isAbstract, ElementType creatingElementType) {
		this.fullName = fullName;
		this.simpleName = simpleName;
		this.isAbstract = isAbstract;
		this.creatingElementType = creatingElementType;
		this.allChildren = new HashMap<>();
		this.allChildren.put(FrankAttribute.class, new LinkedHashMap<>());
		this.allChildren.put(ConfigChild.class, new LinkedHashMap<>());
	}

	public FrankElement(final String fullName, final String simpleName) {
		this(fullName, simpleName, false, null);
	}
	
	public void setParent(FrankElement parent) {
		this.parent = parent;
		this.statistics = new FrankElementStatistics(this);
	}

	public void setAttributes(List<FrankAttribute> inputAttributes) {
		setChildrenOfKind(inputAttributes, FrankAttribute.class);
	}

	private <C extends ElementChild> void setChildrenOfKind(List<C> inputChildren, Class<C> kind) {
		Collections.sort(inputChildren);
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
	public <T extends ElementChild> List<T> getChildrenOfKind(
			Predicate<ElementChild> selector, Class<T> kind) {
		Map<? extends AbstractKey, ? extends ElementChild> lookup = allChildren.get(kind);
		return lookup.values().stream().filter(selector).map(c -> (T) c).collect(Collectors.toList());
	}

	public void setConfigChildren(List<ConfigChild> children) {
		setChildrenOfKind(children, ConfigChild.class);
	}

	public List<ConfigChild> getConfigChildren(Predicate<ElementChild> filter) {
		return getChildrenOfKind(filter, ConfigChild.class);
	}

	ElementChild findElementChildMatch(ElementChild elementChild, Class<? extends ElementChild> kind) {
		Map<? extends AbstractKey, ? extends ElementChild> lookup = allChildren.get(kind);
		return lookup.get(elementChild.getKey());
	}

	public FrankElement getNextAncestorThatHasChildren(Predicate<FrankElement> noChildren) {
		FrankElement ancestor = parent;
		while((ancestor != null) && noChildren.test(ancestor)) {
			ancestor = ancestor.getParent();
		}
		return ancestor;
	}

	public void walkCumulativeAttributes(
			CumulativeChildHandler<FrankAttribute> handler,
			Predicate<ElementChild> childSelector,
			Predicate<ElementChild> childRejector) {
		new AncestorChildNavigation<FrankAttribute>(
				handler, childSelector, childRejector, FrankAttribute.class).run(this);
	}

	public void walkCumulativeConfigChildren(
			CumulativeChildHandler<ConfigChild> handler,
			Predicate<ElementChild> childSelector,
			Predicate<ElementChild> childRejector) {
		new AncestorChildNavigation<ConfigChild>(
				handler, childSelector, childRejector, ConfigChild.class).run(this);		
	}

	public String getXsdElementName(final ElementType elementType, final String groupSyntax1Name) {
		if(! elementType.isFromJavaInterface()) {
			return Utils.toUpperCamelCase(groupSyntax1Name);
		}
		String postfixToRemove = elementType.getSimpleName();
		if(postfixToRemove.startsWith("I")) {
			postfixToRemove = postfixToRemove.substring(1);
		}
		String result = simpleName;
		if(result.endsWith(postfixToRemove)) {
			result = result.substring(0, result.lastIndexOf(postfixToRemove));
		}
		result = result + Utils.toUpperCamelCase(groupSyntax1Name);
		return result;
	}
}
