/* 
Copyright 2021 WeAreFrank! 

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
package nl.nn.adapterframework.frankdoc;

import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addChoice;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addSequence;

import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.AttributeUse;
import nl.nn.adapterframework.frankdoc.model.ConfigChild;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.FrankAttribute;
import nl.nn.adapterframework.frankdoc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

public enum XsdVersion {
	STRICT(ElementChild.IN_XSD, ElementChild.REJECT_DEPRECATED, f -> ! f.isDeprecated(), new DelegateStrict()),
	COMPATIBILITY(ElementChild.IN_COMPATIBILITY_XSD, ElementChild.EXCLUDED, f -> true, new DelegateCompatibility());

	private static Logger log = LogUtil.getLogger(XsdVersion.class);

	private final @Getter Predicate<ElementChild> childSelector;
	private final @Getter Predicate<ElementChild> childRejector;
	private final @Getter Predicate<FrankElement> elementFilter;
	private final Delegate delegate;

	private XsdVersion(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Predicate<FrankElement> elementFilter, Delegate delegate) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
		this.elementFilter = elementFilter;
		this.delegate = delegate;
	}

	void checkForMissingDescription(FrankAttribute attribute) {
		delegate.checkForMissingDescription(attribute);
	}

	void checkForMissingDescription(ConfigChild configChild) {
		delegate.checkForMissingDescription(configChild);
	}

	AttributeUse getRoleNameAttributeUse() {
		return delegate.getRoleNameAttributeUse();
	}

	AttributeUse getClassNameAttributeUse(FrankElement frankElement) {
		return delegate.getClassNameAttributeUse(frankElement);
	}

	XmlBuilder configChildBuilderWithinSequence(XmlBuilder context) {
		return delegate.configChildBuilderWithinSequence(context);
	}

	XmlBuilder configChildBuilder(XmlBuilder context) {
		return delegate.configChildBuilder(context);
	}

	private static abstract class Delegate {
		abstract void checkForMissingDescription(FrankAttribute attribute);
		abstract void checkForMissingDescription(ConfigChild configChild);
		abstract AttributeUse getRoleNameAttributeUse();
		abstract AttributeUse getClassNameAttributeUse(FrankElement frankElement);
		abstract XmlBuilder configChildBuilderWithinSequence(XmlBuilder context);
		abstract XmlBuilder configChildBuilder(XmlBuilder context);
	}

	private static class DelegateStrict extends Delegate {
		@Override
		void checkForMissingDescription(FrankAttribute attribute) {
			if(attribute.getDescription() != null) {
				return;
			}
			log.warn("Attribute [{}] lacks description", attribute.toString());
		}

		@Override
		void checkForMissingDescription(ConfigChild configChild) {
			if(configChild.getDescription() != null) {
				return;
			}
			log.warn("Config child [{}] lacks description", configChild.toString());
		}

		AttributeUse getRoleNameAttributeUse() {
			return AttributeUse.PROHIBITED;
		}

		@Override
		AttributeUse getClassNameAttributeUse(FrankElement frankElement) {
			return AttributeUse.PROHIBITED;
		}

		@Override
		XmlBuilder configChildBuilderWithinSequence(XmlBuilder context) {
			return context;
		}

		@Override
		XmlBuilder configChildBuilder(XmlBuilder context) {
			return context;
		}
	}

	private static class DelegateCompatibility extends Delegate {
		@Override
		void checkForMissingDescription(FrankAttribute attribute) {
		}

		@Override
		void checkForMissingDescription(ConfigChild configChild) {
		}

		/**
		 * Fix of https://github.com/ibissource/iaf/issues/1760. We need
		 * to omit the "use" attribute of the "elementRole" attribute in
		 * the compatibility XSD.
		 */
		@Override
		AttributeUse getRoleNameAttributeUse() {
			return AttributeUse.OPTIONAL;
		}

		/**
		 * Fix of https://github.com/ibissource/iaf/issues/1760. We need
		 * to omit the "use" attribute of the "className" attribute in
		 * the compatibility XSD, but only for interface-based FrankElement-s.
		 */
		@Override
		AttributeUse getClassNameAttributeUse(FrankElement frankElement) {
			if(frankElement.isInterfaceBased()) {
				return AttributeUse.OPTIONAL;	
			} else {
				return AttributeUse.PROHIBITED;
			}
		}

		XmlBuilder configChildBuilderWithinSequence(XmlBuilder context) {
			return addChoice(context, "0", "unbounded");
		}

		XmlBuilder configChildBuilder(XmlBuilder context) {
			XmlBuilder sequence = addSequence(context);
			return addChoice(sequence, "0", "unbounded");
		}
	}
}
