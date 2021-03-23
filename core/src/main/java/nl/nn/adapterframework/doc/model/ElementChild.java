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

package nl.nn.adapterframework.doc.model;

import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.DocWriterNew;
import nl.nn.adapterframework.doc.doclet.FrankDocException;
import nl.nn.adapterframework.doc.doclet.FrankAnnotation;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Base class of FrankAttribute and ConfigChild. This class was introduced
 * to implement the following common logic:
 * <ul>
 * <li> The decision whether to include an attribute or config child in the XML schema
 * is based on the same information.
 * <li> The structure is very similar in the XML schema for config children and
 * attributes. In both cases, we have cumulative groups that include inherited
 * items and declared groups that hold only items at the present level of the
 * inheritance hierarchy. Please see this in action at {@link DocWriterNew}.
 * </ul>
 *
 * @author martijn
 */
public abstract class ElementChild {
	private static Logger log = LogUtil.getLogger(ElementChild.class);

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

	/**
	 * This property is used to omit "technical overrides" from the XSDs. Sometimes
	 * the Java code of the F!F overrides a method without a change of meaning of
	 * the corresponding attribute or config child. Such technical overrides should
	 * not be used to add attributes or config children to the XSDs.
	 * <p>
	 * The property is a bit different for attributes and config children, but we
	 * define it here because it is used the same way for both attributes and
	 * config children.
	 */
	private @Getter @Setter boolean technicalOverride = false;

	/**
	 * Different {@link ElementChild} of the same FrankElement are allowed to have the same order.
	 */
	private @Getter @Setter int order = Integer.MAX_VALUE;
	private @Getter String description;
	private @Getter String defaultValue;

	public static Predicate<ElementChild> IN_XSD = c ->
		(! c.isDeprecated())
		&& (c.isDocumented() || (! c.isTechnicalOverride()));

	public static Predicate<ElementChild> IN_COMPATIBILITY_XSD = c ->
		c.isDocumented() || (! c.isTechnicalOverride());

	public static Predicate<ElementChild> DEPRECATED = c -> c.isDeprecated();
	public static Predicate<ElementChild> ALL = c -> true;
	public static Predicate<ElementChild> NONE = c -> false;

	/**
	 * Base class for keys used to look up {@link FrankAttribute} objects or
	 * {@link ConfigChild} objects from a map.
	 */
	static abstract class AbstractKey {
	}

	ElementChild(final FrankElement owningElement) {
		this.owningElement = owningElement;
	}

	void calculateOverriddenFrom() {
		FrankElement match = getOwningElement();
		while(match.getParent() != null) {
			match = match.getParent();
			ElementChild matchingChild = match.findElementChildMatch(this);
			if(matchingChild != null) {
				if(matchingChild.isDeprecated()) {
					log.warn("Element child overrides deprecated ElementChild: descendant [{}], super [{}]", () -> toString(), () -> matchingChild.toString());
				}
				overriddenFrom = match;
				if(! overrideIsMeaningful(matchingChild)) {
					technicalOverride = true;
				}
				log.trace("{} [{}] of FrankElement [{}] has overriddenFrom = [{}]",
						() -> getClass().getSimpleName(), () -> toString(), () -> owningElement.getFullName(), () -> overriddenFrom.getFullName());
				return;
			}
		}
	}

	abstract boolean overrideIsMeaningful(ElementChild overriddenFrom);

	boolean parseIbisDocAnnotation(FrankAnnotation ibisDoc) {
		String[] ibisDocValues = null;
		try {
			ibisDocValues = (String[]) ibisDoc.getValue();
		} catch(FrankDocException e) {
			log.warn("Could not parse FrankAnnotation of @IbisDoc", e);
		}
		boolean isIbisDocHasOrder = false;
		description = "";
		try {
			order = Integer.parseInt(ibisDocValues[0]);
			isIbisDocHasOrder = true;
		} catch (NumberFormatException e) {
			isIbisDocHasOrder = false;
		}
		if (isIbisDocHasOrder) {
			if(ibisDocValues.length > 1) {
				description = ibisDocValues[1];
			}
			if (ibisDocValues.length > 2) {
				defaultValue = ibisDocValues[2]; 
			}
		} else {
			description = ibisDocValues[0];	
			if (ibisDocValues.length > 1) {
				defaultValue = ibisDocValues[1];
			}
		}
		return isIbisDocHasOrder;
	}

	@Override
	public String toString() {
		return String.format("(Key %s, owner %s)", getKey().toString(), owningElement.getFullName());
	}

	/**
	 * Get key that is used to match overrides. If {@link FrankElement} <code>A</code>
	 * is a descendant of {@link FrankElement} <code>B</code> and if their
	 * respective attributes <code>a</code> and <code>b</code> have an equal key,
	 * then attribute <code>a</code> is assumed to override attribute <code>b</b>.
	 * This function has the same purpose for config children.
	 */
	abstract AbstractKey getKey();
}
