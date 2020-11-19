package nl.nn.adapterframework.doc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;

public class FrankElement {
	private static Logger log = LogUtil.getLogger(FrankElement.class);

	private static final Comparator<ConfigChild> CONFIG_CHILD_COMPARATOR =
			Comparator.comparing(ConfigChild::getSequenceInConfig)
			.thenComparing(ConfigChild::getSyntax1Name);
	private static final Comparator<FrankAttribute> FRANK_ATTRIBUTE_COMPARATOR =
			Comparator.comparing(FrankAttribute::getOrder)
			.thenComparing(FrankAttribute::getName);

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
	private @Getter FrankElement parent;
	private @Getter List<FrankAttribute> attributes;
	private Map<String, FrankAttribute> attributeLookup;
	private @Getter List<ConfigChild> configChildren;
	private Map<ConfigChildKey, ConfigChild> configChildLookup;
	private @Getter FrankElementStatistics statistics;

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
		inputAttributes.sort(FRANK_ATTRIBUTE_COMPARATOR);
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
		children.sort(CONFIG_CHILD_COMPARATOR);
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

	FrankAttribute findAttributeMatch(ElementChild attribute) {
		return attributeLookup.get(((FrankAttribute) attribute).getName());
	}

	ConfigChild findConfigChildMatch(ElementChild configChild) {
		return configChildLookup.get(new ConfigChildKey(((ConfigChild) configChild)));
	}

	public <T extends ElementChild> List<T> getSelectedChildren(Function<FrankElement, List<T>> kind) {
		List<T> rawChildren = kind.apply(this);
		return rawChildren.stream()
				.filter(ElementChild.SELECTED)
				.collect(Collectors.toList());
	}

	private FrankElement getNextAncestor(Function<FrankElement, List<ElementChild>> childSelector) {
		FrankElement current = parent;
		while((current != null) && childSelector.apply(current).size() == 0) {
			current = current.parent;
		}
		return current;
	}

	public FrankElement getNextSelectedAttributeAncestor() {
		return getNextAncestor(el -> el.getSelectedChildren(
				FrankElement::getGenericAttributes));
	}

	private List<ElementChild> getGenericAttributes() {
		List<ElementChild> result = new ArrayList<>();
		getAttributes().forEach(result::add);
		return result;
	}

	public FrankElement getNextSelectedConfigChildAncestor() {
		return getNextAncestor(el -> el.getSelectedChildren(
				FrankElement::getGenericConfigChildren));
	}

	private List<ElementChild> getGenericConfigChildren() {
		List<ElementChild> result = new ArrayList<>();
		getConfigChildren().forEach(result::add);
		return result;
	}

	public FrankElement getNextSelectedParent() {
		return getNextAncestor(FrankElement::getGenericSelectedChildren);
	}

	private List<ElementChild> getGenericSelectedChildren() {
		List<ElementChild> result = new ArrayList<>();
		getSelectedChildren(FrankElement::getAttributes).forEach(result::add);
		getSelectedChildren(FrankElement::getConfigChildren).forEach(result::add);
		return result;
	}

	public void walkSelectedCumulativeAttributes(CumulativeChildHandler handler) {
		new AncestorChildNavigation<String>(handler) {
			@Override
			List<? extends ElementChild> getChildrenOf(FrankElement element) {
				return element.getSelectedChildren(FrankElement::getAttributes);
			}

			@Override
			FrankElement nextAncestor(FrankElement element) {
				return element.getNextSelectedAttributeAncestor(); 
			}

			@Override
			String keyOf(ElementChild child) {
				return ((FrankAttribute) child).getName();
			}
		}.run(this);
	}

	public void walkSelectedCumulativeConfigChildren(CumulativeChildHandler handler) {
		new AncestorChildNavigation<ConfigChildKey>(handler) {
			@Override
			List<? extends ElementChild> getChildrenOf(FrankElement element) {
				return element.getSelectedChildren(FrankElement::getConfigChildren);
			}

			@Override
			FrankElement nextAncestor(FrankElement element) {
				return element.getNextSelectedConfigChildAncestor(); 
			}

			@Override
			ConfigChildKey keyOf(ElementChild child) {
				return new ConfigChildKey((ConfigChild) child);
			}
		}.run(this);		
	}
}
