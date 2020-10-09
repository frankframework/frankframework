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
	
	private @Getter @Setter FrankElement describingElement;
	private @Getter String description;
	private @Getter String defaultValue;
	private @Getter @Setter boolean isDeprecated;

	public FrankAttribute(String name) {
		this.name = name;
	}

	void parseIbisDocAnnotation(IbisDoc ibisDoc) {
		String[] ibisDocValues = ibisDoc.value();
		boolean isIbisDocHasOrder = false;
		order = Integer.MAX_VALUE;
		try {
			order = Integer.parseInt(ibisDocValues[0]);
			isIbisDocHasOrder = true;
		} catch (NumberFormatException e) {
			log.warn(String.format("Could not parse order in @IbisDoc annotation: " + ibisDocValues[0]));
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
}
