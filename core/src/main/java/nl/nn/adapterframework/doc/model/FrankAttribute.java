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

import java.util.Comparator;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class FrankAttribute extends ElementChild implements Comparable<FrankAttribute> {
	private static Logger log = LogUtil.getLogger(FrankAttribute.class);

	private @Getter String name;
	
	/**
	 * Different FrankAttributes of the same FrankElement are allowed to have the same order.
	 */
	private @Getter @Setter int order;
	
	private @Getter @Setter FrankElement describingElement;
	private @Getter String description;
	private @Getter String defaultValue;

	public FrankAttribute(String name, FrankElement attributeOwner) {
		super(attributeOwner);
		this.name = name;
		this.describingElement = attributeOwner;
	}

	@Override
	public String getKey() {
		return name;
	}

	void parseIbisDocAnnotation(IbisDoc ibisDoc) {
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

	@Override
	public int compareTo(FrankAttribute other) {
		return FRANK_ATTRIBUTE_COMPARATOR.compare(this, (FrankAttribute) other);
	}

	private static final Comparator<FrankAttribute> FRANK_ATTRIBUTE_COMPARATOR =
			Comparator.comparing(FrankAttribute::getOrder)
			.thenComparing(FrankAttribute::getName);
}
