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

	/**
	 * Converts <code>ElementChild&lt;FrankAttribute&gt;</code> to <code>FrankAttribute</code>
	 * or <code>ElementChild&lt;ConfigChild&gt;</code> to <code>ConfigChild</code>. This cast
	 * is needed to avoid compiler errors.
	 */
	abstract T cast();
}
