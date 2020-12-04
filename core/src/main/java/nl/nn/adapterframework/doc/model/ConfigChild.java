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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigChild extends ElementChild<ConfigChild.Key> implements Comparable<ConfigChild> {
	private static Logger log = LogUtil.getLogger(ConfigChild.class);

	@EqualsAndHashCode
	static final class Key {
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
	public int compareTo(ConfigChild other) {
		return CONFIG_CHILD_COMPARATOR.compare(this, (ConfigChild) other);
	}

	private static final Comparator<ConfigChild> CONFIG_CHILD_COMPARATOR =
			Comparator.comparing(ConfigChild::getSequenceInConfig)
			.thenComparing(ConfigChild::getSyntax1Name);
}
