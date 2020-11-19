package nl.nn.adapterframework.doc.model;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.Setter;

public abstract class ElementChild {
	public abstract FrankElement getOwningElement();
	public abstract boolean isDeprecated();
	public abstract boolean isDocumented();
	private @Getter @Setter FrankElement overriddenFrom;

	public static Predicate<ElementChild> SELECTED = c ->
		(! c.isDeprecated())
		&& (c.isDocumented() || (c.getOverriddenFrom() == null));

	void calculateOverriddenFrom(BiFunction<FrankElement, ElementChild, ? extends ElementChild> lookup) {
		FrankElement match = getOwningElement();
		while(match.getParent() != null) {
			match = match.getParent();
			ElementChild matchingChild = lookup.apply(match, this);
			if(matchingChild != null) {
				FrankElement matchOverriddenFrom = matchingChild.overriddenFrom;
				if(matchOverriddenFrom != null) {
					overriddenFrom = matchOverriddenFrom;
				} else {
					overriddenFrom = match;
				}
				return;
			}
		}
	}
}
