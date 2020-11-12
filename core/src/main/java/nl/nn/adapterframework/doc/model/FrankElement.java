package nl.nn.adapterframework.doc.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;

public class FrankElement {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private final @Getter String fullName;
	private final @Getter String simpleName;
	private @Getter @Setter FrankElement parent;
	private @Getter List<FrankAttribute> attributes;
	private Map<String, FrankAttribute> attributeLookup;
	private @Getter List<ConfigChild> configChildren;
	private Map<ConfigChildKey, ConfigChild> configChildLookup;

	FrankElement(Class<?> clazz) {
		this(clazz.getName(), clazz.getSimpleName());
	}

	/**
	 * Constructor for testing purposes. We want to test attribute construction in isolation,
	 * in which case we do not have a parent.
	 * TODO: Reorganize files such that this test constructor need not be public.
	 */
	public FrankElement(final String fullName, final String simpleName) {
		this.fullName = fullName;
		this.simpleName = simpleName;
	}

	/**
	 * Setter for attributes. We prevent modifying the list of attributes
	 * because we want to maintain the private field attributeLookup.
	 * @param inputAttributes
	 */
	public void setAttributes(List<FrankAttribute> inputAttributes) {
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

	/**
	 * Setter for config children. We prevent modifying the list of config children
	 * because we want to maintain the private field configChildLookup.
	 * @param children
	 */
	public void setConfigChildren(List<ConfigChild> children) {
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

	public FrankAttribute find(String attributeName) {
		return attributeLookup.get(attributeName);
	}

	public ConfigChild find(ConfigChildKey key) {
		return configChildLookup.get(key);
	}
}
