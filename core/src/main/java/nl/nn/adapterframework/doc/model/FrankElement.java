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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.util.LogUtil;

public class FrankElement {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	@EqualsAndHashCode
	private final class ConfigChildKey {
		private final @Getter String syntax1Name;
		private final @Getter ElementType elementType;
		private final @Getter boolean mandatory;
		private final @Getter boolean allowMultiple;

		public ConfigChildKey(ConfigChild configChild) {
			syntax1Name = configChild.getSyntax1Name();
			elementType = configChild.getElementType();
			mandatory = configChild.isMandatory();
			allowMultiple = configChild.isAllowMultiple();
		}
	}

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private final @Getter boolean isAbstract;
	private @Getter FrankElement parent;
	private @Getter List<FrankAttribute> attributes;
	private Map<String, FrankAttribute> attributeLookup;
	private @Getter List<ConfigChild> configChildren;
	private Map<ConfigChildKey, ConfigChild> configChildLookup;
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
	}

	public FrankElement(final String fullName, final String simpleName) {
		this(fullName, simpleName, false);
	}
	
	public void setParent(FrankElement parent) {
		this.parent = parent;
		this.statistics = new FrankElementStatistics(this);
	}

	/**
	 * Setter for attributes. We prevent modifying the list of attributes
	 * because we want to maintain the private field attributeLookup.
	 * @param inputAttributes
	 */
	public void setAttributes(List<FrankAttribute> inputAttributes) {
		Collections.sort(inputAttributes);
		this.attributes = Collections.unmodifiableList(inputAttributes);
		attributeLookup = new HashMap<>();
		for(FrankAttribute a: attributes) {
			if(attributeLookup.containsKey(a.getName())) {
				log.warn(String.format("Frank element [%s] has multiple attributes with name [%s]",
						fullName, a.getName()));
			} else {
				attributeLookup.put(a.getName(), a);
			}
		}
	}

	public List<FrankAttribute> getAttributes(Predicate<? super FrankAttribute> filter) {
		return attributes.stream().filter(filter).collect(Collectors.toList());
	}

	/**
	 * Setter for config children. We prevent modifying the list of config children
	 * because we want to maintain the private field configChildLookup.
	 * @param children
	 */
	public void setConfigChildren(List<ConfigChild> children) {
		Collections.sort(children);
		this.configChildren = Collections.unmodifiableList(children);
		configChildLookup = new HashMap<>();
		for(ConfigChild c: children) {
			ConfigChildKey key = new ConfigChildKey(c);
			if(configChildLookup.containsKey(key)) {
				log.warn(String.format("Different config children of Frank element [%s] have the same key", fullName));
			} else {
				configChildLookup.put(key, c);
			}
		}
	}

	public List<ConfigChild> getConfigChildren(Predicate<? super ConfigChild> filter) {
		return configChildren.stream().filter(filter).collect(Collectors.toList());
	}

	FrankAttribute findAttributeMatch(FrankAttribute attribute) {
		return attributeLookup.get(attribute.getName());
	}

	ConfigChild findConfigChildMatch(ConfigChild configChild) {
		return configChildLookup.get(new ConfigChildKey(configChild));
	}

	public FrankElement getNextConfigChildAncestor(Predicate<? super ConfigChild> childFilter) {
		return nextAncestor(this, el -> el.getConfigChildren(childFilter));
	}

	static <U extends ElementChild<U>> FrankElement nextAncestor(FrankElement elem, Function<FrankElement, List<U>> fun) {
		FrankElement ancestor = elem.getParent();
		while((ancestor != null) && (fun.apply(ancestor).size() == 0)) {
			ancestor = ancestor.getParent();
		}
		return ancestor;
	}

	public FrankElement getNextAncestor(Predicate<ElementChild<?>> selector) {
		FrankElement current = parent;
		while((current != null) && (current.getGenericChildren(selector).size() == 0)) {
			current = current.parent;
		}
		return current;
	}

	public FrankElement getNextAttributeAncestor(Predicate<? super FrankAttribute> childFilter) {
		return nextAncestor(this, el -> el.getAttributes(childFilter));
	}

	private List<ElementChild<?>> getGenericChildren(Predicate<ElementChild<?>> selector) {
		List<ElementChild<?>> result = new ArrayList<>();
		result.addAll(getAttributes(selector));
		result.addAll(getConfigChildren(selector));
		return result;
	}

	public void walkCumulativeAttributes(
			CumulativeChildHandler<FrankAttribute> handler,
			Predicate<ElementChild<?>> childSelector,
			Predicate<ElementChild<?>> childRejector) {
		new AncestorChildNavigation<String, FrankAttribute>(
				handler, el -> el.getAttributes(), childSelector, childRejector) {
			@Override
			String keyOf(FrankAttribute child) {
				return child.getName();
			}
		}.run(this);
	}

	public void walkCumulativeConfigChildren(
			CumulativeChildHandler<ConfigChild> handler,
			Predicate<ElementChild<?>> childSelector,
			Predicate<ElementChild<?>> childRejector) {
		new AncestorChildNavigation<ConfigChildKey, ConfigChild>(
				handler, el -> el.getConfigChildren(childSelector), childSelector, childRejector) {
			@Override
			ConfigChildKey keyOf(ConfigChild child) {
				return new ConfigChildKey(child);
			}
		}.run(this);		
	}

	public void addAliasSource(ConfigChild aliasSource) {
		aliasSources.add(aliasSource);
	}

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
