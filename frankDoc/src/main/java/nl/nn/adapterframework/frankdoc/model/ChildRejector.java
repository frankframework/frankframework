/* 
Copyright 2020, 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc.model;

import static nl.nn.adapterframework.frankdoc.model.ElementChild.ALL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.nn.adapterframework.frankdoc.model.ElementChild.AbstractKey;

class ChildRejector<T extends ElementChild> {
	private final Predicate<ElementChild> selector;
	private final Predicate<ElementChild> rejector;
	private final Class<T> kind;

	private Predicate<FrankElement> noChildren;

	private Map<String, Level> levels = new HashMap<>();

	ChildRejector(Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Class<T> kind) {
		this.selector = selector;
		this.rejector = rejector;
		this.kind = kind;
		this.noChildren = el -> el.getChildrenOfKind(selector, kind).isEmpty();
	}

	private Level addLevelsFor(FrankElement owner, Set<AbstractKey> rejectCandidates) {
		Level result = new Level(owner, rejectCandidates);
		levels.put(owner.getFullName(), result);
		return result;
	}

	private class Level {
		Set<AbstractKey> rejectCandidates = new HashSet<>();
		boolean isRejectsDeclared = false;
		boolean isRejectsDeclaredOrInherited = false;

		Level(FrankElement levelOwner, Set<AbstractKey> downstreamRejectCandidates) {
			calculateRejectCandidates(levelOwner, downstreamRejectCandidates);
			Level parentLevel = addParentLevelsFor(levelOwner);
			isRejectsDeclared = levelOwner.getChildrenOfKind(selector, kind).stream()
					.map(ElementChild::getKey)
					.filter(rejectCandidates::contains)
					.collect(Collectors.counting()) >= 1;
			if(parentLevel == null) {
				isRejectsDeclaredOrInherited = isRejectsDeclared;
			} else {
				isRejectsDeclaredOrInherited = isRejectsDeclared || parentLevel.isRejectsDeclaredOrInherited;
			}
		}

		private void calculateRejectCandidates(FrankElement levelOwner, Set<AbstractKey> downstreamRejectCandidates) {
			rejectCandidates = levelOwner.getChildrenOfKind(rejector, kind).stream()
					.map(ElementChild::getKey)
					.collect(Collectors.toSet());
			rejectCandidates.addAll(downstreamRejectCandidates);
		}

		private Level addParentLevelsFor(FrankElement levelOwner) {
			Level parentLevel = null;
			FrankElement parent = levelOwner.getNextAncestorThatHasChildren(noChildren);
			if(parent != null) {
				parentLevel = addLevelsFor(parent, rejectCandidates);
			}
			return parentLevel;
		}

		boolean acceptsChildKey(AbstractKey childKey) {
			return ! rejectCandidates.contains(childKey);
		}
	}

	void init(FrankElement subject) {
		addLevelsFor(subject, new HashSet<>());
	}

	List<ElementChild> getChildrenFor(FrankElement levelOwner) {
		return childrenFor(levelOwner, getChildKeysFor(levelOwner));
	}

	private List<ElementChild> childrenFor(FrankElement levelOwner, Set<AbstractKey> childKeys) {
		return levelOwner.getChildrenOfKind(ALL, kind).stream()
				.filter(c -> childKeys.contains(c.getKey()))
				.collect(Collectors.toList());
	}

	private Set<AbstractKey> getChildKeysFor(FrankElement levelOwner) {
		return levelOwner.getChildrenOfKind(selector, kind).stream()
				.map(ElementChild::getKey)
				.filter(k -> levels.get(levelOwner.getFullName()).acceptsChildKey(k))
				.collect(Collectors.toSet());
	}

	boolean isNoDeclaredRejected(FrankElement levelOwner) {
		return ! levels.get(levelOwner.getFullName()).isRejectsDeclared;
	}

	boolean isNoCumulativeRejected(FrankElement levelOwner) {
		return ! levels.get(levelOwner.getFullName()).isRejectsDeclaredOrInherited;
	}
}
