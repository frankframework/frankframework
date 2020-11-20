package nl.nn.adapterframework.doc;

import java.util.List;
import java.util.function.Consumer;

import nl.nn.adapterframework.doc.model.CumulativeChildHandler;
import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.FrankElement;

class GroupCreator<T extends ElementChild<T>> {
	static interface Callback<T extends ElementChild<T>> extends CumulativeChildHandler<T> {
		List<T> getChildrenOf(FrankElement elem);
		FrankElement getAncestorOf(FrankElement elem);
		void addDeclaredGroup();
		void addCumulativeGroup();
		void addDeclaredGroupRef(FrankElement referee);
		void addCumulativeGroupRef(FrankElement referee);
	}

	private FrankElement frankElement;
	private Callback<T> callback;
	private Consumer<Callback<T>> cumulativeGroupTrigger;

	GroupCreator(
			FrankElement frankElement,
			Consumer<Callback<T>> cumulativeGroupTrigger,
			Callback<T> callback) {
		this.frankElement = frankElement;
		this.cumulativeGroupTrigger = cumulativeGroupTrigger;
		this.callback = callback;
	}

	void run() {
		boolean hasNoConfigChildren = callback.getChildrenOf(frankElement).isEmpty();
		FrankElement ancestor = callback.getAncestorOf(frankElement);
		if(hasNoConfigChildren) {
			if(ancestor == null) {
				return;
			}
			else {
				FrankElement superAncestor = callback.getAncestorOf(ancestor);
				if(superAncestor == null) {
					callback.addDeclaredGroupRef(ancestor);
				}
				else {
					callback.addCumulativeGroupRef(ancestor);
				}
			}
		}
		else {
			if(ancestor == null) {
				callback.addDeclaredGroupRef(frankElement);
			}
			else {
				callback.addCumulativeGroupRef(frankElement);
				addCumulativeChildGroup();
			}
			callback.addDeclaredGroup();
		}
	}

	private void addCumulativeChildGroup() {
		callback.addCumulativeGroup();
		cumulativeGroupTrigger.accept(callback);
	}
}
