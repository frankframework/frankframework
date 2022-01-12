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

package nl.nn.adapterframework.frankdoc;

import java.util.List;
import java.util.function.Consumer;

import nl.nn.adapterframework.frankdoc.model.CumulativeChildHandler;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.FrankElement;

/**
 * Helper class for building attribute groups and config child groups from the model.
 * See {@link DocWriterNew} to understand what we try to achieve.
 *
 * @author martijn
 *
 */
class GroupCreator<T extends ElementChild> {
	static interface Callback<T extends ElementChild> extends CumulativeChildHandler<T> {
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

	GroupCreator(FrankElement frankElement, Consumer<Callback<T>> cumulativeGroupTrigger, Callback<T> callback) {
		this.frankElement = frankElement;
		this.cumulativeGroupTrigger = cumulativeGroupTrigger;
		this.callback = callback;
	}

	void run() {
		boolean hasNoElementChildrenOfKind = callback.getChildrenOf(frankElement).isEmpty();
		FrankElement ancestor = callback.getAncestorOf(frankElement);
		if(hasNoElementChildrenOfKind) {
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
			callback.addDeclaredGroup();
			if(ancestor == null) {
				callback.addDeclaredGroupRef(frankElement);
			}
			else {
				callback.addCumulativeGroupRef(frankElement);
				addCumulativeChildGroup();
			}
		}
	}

	private void addCumulativeChildGroup() {
		callback.addCumulativeGroup();
		cumulativeGroupTrigger.accept(callback);
	}
}
