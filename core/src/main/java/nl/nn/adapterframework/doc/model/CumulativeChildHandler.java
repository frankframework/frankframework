package nl.nn.adapterframework.doc.model;

import java.util.List;

public interface CumulativeChildHandler<T extends ElementChild<?>> {
	void handleSelectedChildren(List<T> children, FrankElement owner);
	void handleChildrenOf(FrankElement frankElement);
	void handleCumulativeChildrenOf(FrankElement frankElement);
}
