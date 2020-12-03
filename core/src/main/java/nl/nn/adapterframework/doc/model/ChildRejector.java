package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ChildRejector<T extends ElementChild> {
	private final Predicate<ElementChild> selector;
	private final Predicate<ElementChild> rejector;
	private final Class<T> kind;

	private Map<String, Level> levels = new HashMap<>();

	ChildRejector(Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Class<T> kind) {
		this.selector = selector;
		this.rejector = rejector;
		this.kind = kind;
	}

	private Level addLevelsFor(FrankElement owner, Set<String> rejectCandidates) {
		Level result = new Level(owner, rejectCandidates);
		levels.put(owner.getFullName(), result);
		return result;
	}

	private class Level {
		Set<String> rejectCandidates = new HashSet<>();
		boolean isRejectsDeclared = false;
		boolean isRejectsDeclaredOrInherited = false;

		Level(FrankElement levelOwner, Set<String> downstreamRejectCandidates) {
			calculateRejectCandidates(levelOwner, downstreamRejectCandidates);
			Level parentLevel = addParentLevelsFor(levelOwner);
			isRejectsDeclared = levelOwner.getChildren(selector, kind).stream()
					.map(ElementChild::getKey)
					.filter(rejectCandidates::contains)
					.collect(Collectors.counting()) >= 1;
			if(parentLevel == null) {
				isRejectsDeclaredOrInherited = isRejectsDeclared;
			} else {
				isRejectsDeclaredOrInherited = isRejectsDeclared || parentLevel.isRejectsDeclaredOrInherited;
			}
		}

		private void calculateRejectCandidates(FrankElement levelOwner, Set<String> downstreamRejectCandidates) {
			rejectCandidates = levelOwner.getChildren(rejector, kind).stream()
					.map(ElementChild::getKey)
					.collect(Collectors.toSet());
			rejectCandidates.addAll(downstreamRejectCandidates);
		}

		private Level addParentLevelsFor(FrankElement levelOwner) {
			Level parentLevel = null;
			FrankElement parent = levelOwner.getNextAncestor(selector, kind);
			if(parent != null) {
				parentLevel = addLevelsFor(parent, rejectCandidates);
			}
			return parentLevel;
		}

		boolean acceptsChildKey(String childKey) {
			return ! rejectCandidates.contains(childKey);
		}
	}

	void init(FrankElement subject) {
		addLevelsFor(subject, new HashSet<>());
	}

	List<ElementChild> getChildrenFor(FrankElement levelOwner) {
		return childrenFor(levelOwner, getChildKeysFor(levelOwner));
	}

	private List<ElementChild> childrenFor(FrankElement levelOwner, Set<String> childKeys) {
		return levelOwner.getChildren(ALL, kind).stream()
				.filter(c -> childKeys.contains(c.getKey()))
				.collect(Collectors.toList());
	}

	private Set<String> getChildKeysFor(FrankElement levelOwner) {
		return levelOwner.getChildren(selector, kind).stream()
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
