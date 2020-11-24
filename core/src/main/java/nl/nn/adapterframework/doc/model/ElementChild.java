package nl.nn.adapterframework.doc.model;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.DocWriterNew;

/**
 * Base class of FrankAttribute and ConfigChild. This class was introduced
 * to implement the following common logic:
 * <ul>
 * <li> The decision whether to include an attribute or config child in the XML schema
 * is based on the same information.
 * <li> The structure is very similar in the XML schema for config children and
 * attributes. In both cases, we have cumulative groups that include inherited
 * items and declared groups that hold only items at the present level of the
 * inheritance hierarchy. Please see this in action at {@link DocWriterNew}
 *
 * @author martijn
 *
 * @param <T>
 */
public abstract class ElementChild<T extends ElementChild<?>> {
	private @Getter FrankElement owningElement;
	
	/**
	 * The value is inherited from ElementChild corresponding to superclass.
	 */
	private @Getter @Setter boolean deprecated;
	
	/**
	 * Only set to true if there is an IbisDoc or IbisDocRef annotation for
	 * this specific ElementChild, excluding inheritance. This property is
	 * intended to detect Java Override annotations that are only there for
	 * technical reasons, without relevance to the Frank developer.
	 * 
	 * But values inside IbisDoc or IbisDocRef annotations are inherited.
	 * That is the case to allow documentation information to be stored more
	 * centrally.
	 */
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
				overriddenFrom = match;
				return;
			}
		}
	}

	abstract T cast();
}
