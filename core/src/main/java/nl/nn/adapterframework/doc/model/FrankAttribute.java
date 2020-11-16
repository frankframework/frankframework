package nl.nn.adapterframework.doc.model;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class FrankAttribute {
	private static Logger log = LogUtil.getLogger(FrankAttribute.class);

	private @Getter String name;
	
	/**
	 * Different FrankAttributes of the same FrankElement are allowed to have the same order.
	 */
	private @Getter @Setter int order;
	
	private @Getter FrankElement owningElement;
	private @Getter @Setter FrankElement describingElement;
	private boolean hasIbisDoc;
	private @Setter boolean hasIbisDocRef;
	private @Getter String description;
	private @Getter String defaultValue;
	private @Getter @Setter boolean isDeprecated;
	private @Getter FrankElement overriddenFrom;

	public FrankAttribute(String name, FrankElement attributeOwner) {
		this.name = name;
		this.owningElement = attributeOwner;
		this.describingElement = attributeOwner;
	}

	void parseIbisDocAnnotation(IbisDoc ibisDoc) {
		hasIbisDoc = true;
		String[] ibisDocValues = ibisDoc.value();
		boolean isIbisDocHasOrder = false;
		order = Integer.MAX_VALUE;
		try {
			order = Integer.parseInt(ibisDocValues[0]);
			isIbisDocHasOrder = true;
		} catch (NumberFormatException e) {
			log.warn(String.format("Could not parse order in @IbisDoc annotation: [%s]", ibisDocValues[0]));
		}
		if (isIbisDocHasOrder) {
			description = ibisDocValues[1];
			if (ibisDocValues.length > 2) {
				defaultValue = ibisDocValues[2]; 
			}
		} else {
			description = ibisDocValues[0];
			if (ibisDocValues.length > 1) {
				defaultValue = ibisDocValues[1];
			}
		}
	}

	public boolean isDocumented() {
		return hasIbisDoc || hasIbisDocRef;
	}

	void calculateOverriddenFrom() {
		FrankElement match = owningElement;
		while(match.getParent() != null) {
			match = match.getParent();
			FrankAttribute matchingAttribute = match.find(name);
			if(matchingAttribute != null) {
				FrankElement matchOverriddenFrom = matchingAttribute.overriddenFrom;
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
