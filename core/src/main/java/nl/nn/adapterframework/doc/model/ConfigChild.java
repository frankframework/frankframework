package nl.nn.adapterframework.doc.model;

import lombok.Getter;
import lombok.Setter;

public class ConfigChild {
	private @Getter FrankElement configParent;
	private @Getter @Setter ElementType elementType;
	private @Getter @Setter int sequenceInConfig;
	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter String syntax1Name;

	ConfigChild(FrankElement configParent) {
		this.configParent = configParent;
	}
}
