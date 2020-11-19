package nl.nn.adapterframework.doc.model;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigChild extends ElementChild {
	private static Logger log = LogUtil.getLogger(ConfigChild.class);

	private @Getter(onMethod=@__(@Override)) FrankElement owningElement;
	private @Getter @Setter ElementType elementType;
	private @Getter(onMethod=@__(@Override)) @Setter boolean documented;
	private @Getter int sequenceInConfig;
	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter(onMethod=@__(@Override)) @Setter boolean deprecated;
	private @Getter @Setter String syntax1Name;
	private @Getter(onMethod=@__(@Override)) FrankElement overriddenFrom;

	ConfigChild(FrankElement owningElement) {
		this.owningElement = owningElement;
	}

	public void setSequenceInConfigFromIbisDocAnnotation(IbisDoc ibisDoc) {
		sequenceInConfig = Integer.MAX_VALUE;
		if(ibisDoc == null) {
			log.warn(String.format("No @IbisDoc annotation for config child, parent [%s] and element type [%s]",
					owningElement.getSimpleName(), elementType.getSimpleName()));
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
						owningElement.getSimpleName(),
						elementType.getSimpleName(),
						ibisDoc.value()[0]));
			}
		}
		return result;
	}
}
