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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.doc.DocWriterNew;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.doc.model.ElementChild.AbstractKey;
import nl.nn.adapterframework.util.LogUtil;

public class FrankElement {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private final @Getter boolean isAbstract;

	// Represents the Java superclass.
	private @Getter FrankElement parent;

	private Map<Class<? extends ElementChild>, LinkedHashMap<? extends AbstractKey, ? extends ElementChild>> allChildren;
	private @Getter List<ConfigChild> aliasSources;
	private String cachedAlias = null;
	private @Getter FrankElementStatistics statistics;

	FrankElement(Class<?> clazz) {
		this(clazz.getName(), clazz.getSimpleName(), Modifier.isAbstract(clazz.getModifiers()));
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
		this.aliasSources = new ArrayList<>();
		this.allChildren = new HashMap<>();
		this.allChildren.put(FrankAttribute.class, new LinkedHashMap<>());
		this.allChildren.put(ConfigChild.class, new LinkedHashMap<>());
	}

	public FrankElement(final String fullName, final String simpleName) {
		this(fullName, simpleName, false);
	}
	
	public void setParent(FrankElement parent) {
		this.parent = parent;
		this.statistics = new FrankElementStatistics(this);
	}

	public void setAttributes(List<FrankAttribute> inputAttributes) {
		setChildrenOfKind(inputAttributes, FrankAttribute.class);
	}

	public <C extends ElementChild> void setChildrenOfKind(List<C> inputChildren, Class<C> kind) {
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

	public List<FrankAttribute> getAttributes() {
		return allChildren.get(FrankAttribute.class).values().stream()
				.map(c -> (FrankAttribute) c).collect(Collectors.toList());
	}

	public List<FrankAttribute> getAttributes(Predicate<? super FrankAttribute> filter) {
		return getAttributes().stream().filter(filter).collect(Collectors.toList());
	}

	public void setConfigChildren(List<ConfigChild> children) {
		setChildrenOfKind(children, ConfigChild.class);
	}

	public List<ConfigChild> getConfigChildren() {
		return allChildren.get(ConfigChild.class).values().stream()
			.map(c -> (ConfigChild) c).collect(Collectors.toList());
	}

	public List<ConfigChild> getConfigChildren(Predicate<? super ConfigChild> filter) {
		return getConfigChildren().stream().filter(filter).collect(Collectors.toList());
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

	@SuppressWarnings("unchecked")
	public <T extends ElementChild> List<ElementChild> getChildren(
			Predicate<ElementChild> selector, Class<T> kind) {
		Map<? extends AbstractKey, ? extends ElementChild> lookup = allChildren.get(kind);
		return lookup.values().stream().filter(selector).map(c -> (T) c).collect(Collectors.toList());
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

	public void addAliasSource(ConfigChild aliasSource) {
		aliasSources.add(aliasSource);
	}

	/**
	 * Name to be applied in the &lt;xs:element&gt; in the XSD.
	 * A <code>FrankElement</code> can be contained in another <code>FrankElement</code>.
	 * In some cases, such a child <code>FrankElement</code> has to appear in the XSD
	 * with a different name in the <code>&lt;xs:element&gt;</code>. The alias is this
	 * different name when applicable or equals the <code>simpleName</code>. See also
	 * {@link DocWriterNew}.
	 * <p>
	 * The rule for the alias is as follows. If a config child has an {@link ElementType}
	 * that does not come from a Java interface, then the alias has to differ from the
	 * simple name. The alias then equals the syntax 1 name of the config child with the
	 * first letter capitalized. In theory, there can be different config children that
	 * apply that have different syntax 1 names, but this should not happen in practice.
	 * This method emits a warning if this ambiguity occurs.
	 *
	 * @return
	 */
	public String getAlias() {
		if(cachedAlias != null) {
			return cachedAlias;
		}
		List<String> aliasCandidates = aliasSources.stream()
				.filter(c -> ! c.isDeprecated())
				.map(c -> Utils.toUpperCamelCase(c.getSyntax1Name()))
				.collect(Collectors.toSet())
				.stream().sorted().collect(Collectors.toList());
		if(aliasCandidates.size() == 0) {
			cachedAlias = simpleName;
		} else if(aliasCandidates.size() == 1) {
			cachedAlias = aliasCandidates.iterator().next();
		} else {
			log.warn(String.format("Multiple aliases for config FrankElement %s, which are: %s",
					fullName, aliasCandidates.stream().collect(Collectors.joining(", "))));
			cachedAlias = aliasCandidates.get(0);
		}
		return cachedAlias;
	}
}
