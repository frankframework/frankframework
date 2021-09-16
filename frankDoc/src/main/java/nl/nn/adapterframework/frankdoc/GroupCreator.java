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

import java.util.function.Consumer;
import java.util.function.Predicate;

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
		void addDeclaredGroup();
		void addCumulativeGroup();
		void addDeclaredGroupRef(FrankElement referee);
		void addCumulativeGroupRef(FrankElement referee);
	}

	private FrankElement frankElement;
	private Predicate<FrankElement> hasRelevantChildren;
	private Callback<T> callback;
	private Consumer<Callback<T>> cumulativeGroupTrigger;

	GroupCreator(FrankElement frankElement, Predicate<FrankElement> hasRelevantChildren, Consumer<Callback<T>> cumulativeGroupTrigger, Callback<T> callback) {
		this.frankElement = frankElement;
		this.hasRelevantChildren = hasRelevantChildren;
		this.cumulativeGroupTrigger = cumulativeGroupTrigger;
		this.callback = callback;
	}

	void run() {
		boolean hasNoRelevantChildren = ! hasRelevantChildren.test(frankElement);
		FrankElement ancestor = nextAncestor(frankElement);
		if(hasNoRelevantChildren) {
			if(ancestor == null) {
				return;
			}
			else {
				FrankElement superAncestor = nextAncestor(ancestor);
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

	private FrankElement nextAncestor(FrankElement e) {
		return e.getNextAncestorThatHasChildren(hasRelevantChildren.negate());
	}

	private void addCumulativeChildGroup() {
		callback.addCumulativeGroup();
		cumulativeGroupTrigger.accept(callback);
	}
}
