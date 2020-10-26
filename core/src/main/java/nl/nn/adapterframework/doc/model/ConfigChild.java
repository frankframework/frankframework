package nl.nn.adapterframework.doc.model;

import java.text.ParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigChild {
	private static Logger log = LogUtil.getLogger(ConfigChild.class);

	private @Getter FrankElement configParent;
	private @Getter @Setter ElementType elementType;
	private @Getter int sequenceInConfig;
	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter String syntax1Name;

	ConfigChild(FrankElement configParent) {
		this.configParent = configParent;
	}

	public void setSequenceInConfigFromIbisDocAnnotation(IbisDoc ibisDoc) throws ParseException {
		sequenceInConfig = Integer.MAX_VALUE;
		if(ibisDoc == null) {
			throw new ParseException("No @IbisDoc available", 0);
		}
		Integer optionalOrder = parseIbisDocAnnotation(ibisDoc);
		if(optionalOrder != null) {
			sequenceInConfig = optionalOrder;
		} else {
			throw new ParseException(String.format("Could not get order from @IbisDoc annotation with value [%s]",
					Arrays.asList(ibisDoc.value()).stream().collect(Collectors.joining(", "))), 0);
		}
	}

	private static Integer parseIbisDocAnnotation(IbisDoc ibisDoc) {
		Integer result = null;
		if((ibisDoc != null) && (ibisDoc.value().length == 1)) {
			try {
				result = Integer.valueOf(ibisDoc.value()[0]);
			} catch(Exception e) {
				log.warn(String.format("@IbisDoc for config children has a non-integer order [%s], ignored", ibisDoc.value()[0]));
			}
		}
		return result;
	}
}
