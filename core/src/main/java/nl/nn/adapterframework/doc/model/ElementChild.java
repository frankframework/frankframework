package nl.nn.adapterframework.doc.model;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.Setter;

public abstract class ElementChild<T extends ElementChild<?>> {
	private @Getter FrankElement owningElement;
	private @Getter @Setter boolean deprecated;
	private @Getter @Setter boolean documented;
	private @Getter FrankElement overriddenFrom;

	public static Predicate<ElementChild<?>> SELECTED = c ->
		(! c.isDeprecated())
		&& (c.isDocumented() || (c.getOverriddenFrom() == null));

	ElementChild(final FrankElement owningElement) {
		this.owningElement = owningElement;
	}

	public static Predicate<ElementChild<?>> ALL = c -> true;

	void calculateOverriddenFrom(BiFunction<FrankElement, T, T> lookup) {
		FrankElement match = getOwningElement();
		while(match.getParent() != null) {
			match = match.getParent();
			T matchingChild = lookup.apply(match, cast());
			if(matchingChild != null) {
				FrankElement matchOverriddenFrom = matchingChild.getOverriddenFrom();
				if(matchOverriddenFrom != null) {
					overriddenFrom = matchOverriddenFrom;
				} else {
					overriddenFrom = match;
				}
				return;
			}
		}
	}

	abstract T cast();
}
