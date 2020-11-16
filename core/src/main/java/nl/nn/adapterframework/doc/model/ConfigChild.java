package nl.nn.adapterframework.doc.model;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigChild {
	private static Logger log = LogUtil.getLogger(ConfigChild.class);

	private @Getter FrankElement configParent;
	private @Getter @Setter ElementType elementType;
	private @Getter @Setter boolean documented;
	private @Getter int sequenceInConfig;
	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter boolean deprecated;
	private @Getter @Setter String syntax1Name;
	private @Getter FrankElement overriddenFrom;

	ConfigChild(FrankElement configParent) {
		this.configParent = configParent;
	}

	public void setSequenceInConfigFromIbisDocAnnotation(IbisDoc ibisDoc) {
		sequenceInConfig = Integer.MAX_VALUE;
		if(ibisDoc == null) {
			log.warn(String.format("No @IbisDoc annotation for config child, parent [%s] and element type [%s]",
					configParent.getSimpleName(), elementType.getSimpleName()));
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
						configParent.getSimpleName(),
						elementType.getSimpleName(),
						ibisDoc.value()[0]));
			}
		}
		return result;
	}

	/**
	 * Calculate property overriddenFrom. Assumes that overriddenFrom has been
	 * set already for all ancestors in the FrankElement inheritance hierarchy.
	 */
	public void calculateOverriddenFrom() {
		ConfigChildKey key = new ConfigChildKey(this);
		FrankElement match = configParent;
		while(match.getParent() != null) {
			match = match.getParent();
			ConfigChild matchingChild = match.find(key);
			if(matchingChild != null) {
				if(matchingChild.overriddenFrom != null) {
					this.overriddenFrom = matchingChild.overriddenFrom;
				} else {
					this.overriddenFrom = match;
				}
				return;
			}
		}
	}
}
