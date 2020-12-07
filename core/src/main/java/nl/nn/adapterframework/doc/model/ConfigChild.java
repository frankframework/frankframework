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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigChild extends ElementChild {
	private static Logger log = LogUtil.getLogger(ConfigChild.class);

	@EqualsAndHashCode(callSuper = false)
	static final class Key extends AbstractKey {
		private final @Getter String syntax1Name;
		private final @Getter ElementType elementType;
		private final @Getter boolean mandatory;
		private final @Getter boolean allowMultiple;

		public Key(ConfigChild configChild) {
			syntax1Name = configChild.getSyntax1Name();
			elementType = configChild.getElementType();
			mandatory = configChild.isMandatory();
			allowMultiple = configChild.isAllowMultiple();
		}

		@Override
		public String toString() {
			List<String> components = new ArrayList<>();
			components.add(syntax1Name);
			components.add(elementType.getSimpleName());
			components.add("mandatory " + Boolean.toString(mandatory));
			components.add("multiple " + Boolean.toString(allowMultiple));
			return components.stream().collect(Collectors.joining(", "));
		}
	}

	private @Getter @Setter ElementType elementType;
	private @Getter int sequenceInConfig;
	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter String syntax1Name;

	ConfigChild(FrankElement owningElement) {
		super(owningElement);
	}

	@Override
	Key getKey() {
		return new Key(this);
	}

	public void setSequenceInConfigFromIbisDocAnnotation(IbisDoc ibisDoc) {
		sequenceInConfig = Integer.MAX_VALUE;
		if(ibisDoc == null) {
			log.warn(String.format("No @IbisDoc annotation for config child, parent [%s] and element type [%s]",
					getOwningElement().getSimpleName(), elementType.getSimpleName()));
			return;
		}
		Integer optionalOrder = parseIbisDocAnnotation(ibisDoc);
		if(optionalOrder != null) {
			sequenceInConfig = optionalOrder;
		}
	}

	private Integer parseIbisDocAnnotation(IbisDoc ibisDoc) {
		Integer result = null;
		if(ibisDoc.value().length >= 1) {
			try {
				result = Integer.valueOf(ibisDoc.value()[0]);
			} catch(Exception e) {
				log.warn(String.format("@IbisDoc for config child with parent [%s] and type [%s] has a non-integer order [%s], ignored",
						getOwningElement().getSimpleName(),
						elementType.getSimpleName(),
						ibisDoc.value()[0]));
			}
		}
		return result;
	}

	public String getSyntax1NamePlural() {
		if(syntax1Name.endsWith("s")) {
			return syntax1Name;
		} else {
			return syntax1Name + "s";
		}
	}

	@Override
	public int compareTo(ElementChild other) {
		return CONFIG_CHILD_COMPARATOR.compare(this, (ConfigChild) other);
	}

	/**
	 * Registers the syntax 1 name of this {@link ConfigChild} with the
	 * {@link ElementType}. This is done recursively because the XSD does
	 * not only use the declared config children, but also the inherited
	 * config children of a {@link FrankElement}. For each combination of
	 * a syntax 1 name and an {@link ElementChild}, we have &lt;xs:group&gt;
	 * declarations in the XSD. The recursion ensures that references to
	 * ancestor groups are valid.
	 */
	void recursivelyRegisterSyntax1NameWithElementType(final String syntax1Name) {
		if(IN_XSD.test(this)) {
			elementType.addConfigChildSyntax1Name(syntax1Name);
			FrankElement parent = getOwningElement().getNextAncestorThatHasChildren(
					f -> f.getChildrenOfKind(IN_XSD, ConfigChild.class).isEmpty());
			if(parent != null) {
				ConfigChild match = (ConfigChild) parent.findElementChildMatch(this, ConfigChild.class);
				if(match != null) {
					match.recursivelyRegisterSyntax1NameWithElementType(syntax1Name);
				}
			}
		}
	}

	private static final Comparator<ConfigChild> CONFIG_CHILD_COMPARATOR =
			Comparator.comparing(ConfigChild::getSequenceInConfig)
			.thenComparing(ConfigChild::getSyntax1Name);
}
