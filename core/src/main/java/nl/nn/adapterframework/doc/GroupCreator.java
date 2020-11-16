package nl.nn.adapterframework.doc;

import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.util.XmlBuilder;

class GroupCreator<K> {
	static interface Callback<K> {
		Map<K, Boolean> getChildren(FrankElement frankElement);
		FrankElement getAncestor(FrankElement frankElement);
		XmlBuilder addDeclaredGroup();
		XmlBuilder addCumulativeGroup();
		void addChildren(XmlBuilder context, Set<K> children, FrankElement itemOwner);
		void addDeclaredGroupRef(XmlBuilder context, FrankElement referee);
		void addCumulativeGroupRef(XmlBuilder context, FrankElement referee);
		void notifyGroupRefRepeated(FrankElement current);
		void notifyItemsRepeated(FrankElement itemOwner);
	}

	private XmlBuilder frankElementTypeBuilder;
	private FrankElement frankElement;
	private Callback<K> callback;

	GroupCreator(XmlBuilder frankElementTypeBuilder, FrankElement frankElement, Callback<K> callback) {
		this.frankElementTypeBuilder = frankElementTypeBuilder;
		this.frankElement = frankElement;
		this.callback = callback;
	}

	void run() {
		boolean hasNoConfigChildren = callback.getChildren(frankElement).keySet().isEmpty();
		FrankElement ancestor = callback.getAncestor(frankElement);
		if(hasNoConfigChildren) {
			if(ancestor == null) {
				return;
			}
			else {
				FrankElement superAncestor = callback.getAncestor(ancestor);
				if(superAncestor == null) {
					callback.addDeclaredGroupRef(frankElementTypeBuilder, ancestor);
				}
				else {
					callback.addCumulativeGroupRef(frankElementTypeBuilder, ancestor);
				}
			}
		}
		else {
			if(ancestor == null) {
				callback.addDeclaredGroupRef(frankElementTypeBuilder, frankElement);
			}
			else {
				callback.addCumulativeGroupRef(frankElementTypeBuilder, frankElement);
				addCumulativeChildGroup();
			}
			addDeclaredChildGroup();
		}
	}

	private void addCumulativeChildGroup() {
		final XmlBuilder cumulativeGroupBuilder = callback.addCumulativeGroup();
		new CumulativeGroupCreator<K>() {
			@Override
			FrankElement nextAncestor(FrankElement current) {
				return callback.getAncestor(current);
			}

			@Override
			Map<K, Boolean> itemsOf(FrankElement current) {
				return callback.getChildren(current);
			}

			@Override
			void addItemsOf(Set<K> items, FrankElement itemOwner) {
				callback.addChildren(cumulativeGroupBuilder, items, itemOwner);
			}

			@Override
			void addDeclaredGroup(FrankElement current) {
				callback.addDeclaredGroupRef(cumulativeGroupBuilder, current);
			}

			@Override
			void addCumulativeGroup(FrankElement current) {
				callback.addCumulativeGroupRef(cumulativeGroupBuilder, current);
			}

			@Override
			void notifyGroupRefRepeated(FrankElement current) {
				callback.notifyGroupRefRepeated(current);
			}

			@Override
			void notifyItemsRepeated(FrankElement itemOwner) {
				callback.notifyItemsRepeated(itemOwner);
			}
		}.run(frankElement);
	}

	private void addDeclaredChildGroup() {
		XmlBuilder declaredGroupBuilder = callback.addDeclaredGroup();
		callback.addChildren(declaredGroupBuilder, callback.getChildren(frankElement).keySet(), frankElement);
	}
}
