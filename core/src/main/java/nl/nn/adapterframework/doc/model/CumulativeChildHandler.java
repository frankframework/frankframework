package nl.nn.adapterframework.doc.model;

import java.util.List;

public interface CumulativeChildHandler {
	void handleSelectedChildren(List<? extends ElementChild> children, FrankElement owner);
	void handleChildrenOf(FrankElement frankElement);
	void handleCumulativeChildrenOf(FrankElement frankElement);
}
