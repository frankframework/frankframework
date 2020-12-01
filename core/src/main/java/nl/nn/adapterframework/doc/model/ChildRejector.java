package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ChildRejector<K, T extends ElementChild> {
	private final Predicate<ElementChild> selector;
	private final Predicate<ElementChild> rejector;
	private final Function<ElementChild, K> keyFunction;
	private final Class<T> kind;

	private Map<String, Level> levels = new HashMap<>();

	ChildRejector(Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Function<ElementChild, K> keyFunction, Class<T> kind) {
		this.selector = selector;
		this.rejector = rejector;
		this.keyFunction = keyFunction;
		this.kind = kind;
	}

	private Level addLevelsFor(FrankElement owner, Set<K> rejectCandidates) {
		Level result = new Level(owner, rejectCandidates);
		levels.put(owner.getFullName(), result);
		return result;
	}

	private class Level {
		Set<K> rejectCandidates = new HashSet<>();
		boolean isRejectsDeclared = false;
		boolean isRejectsDeclaredOrInherited = false;

		Level(FrankElement levelOwner, Set<K> downstreamRejectCandidates) {
			calculateRejectCandidates(levelOwner, downstreamRejectCandidates);
			Level parentLevel = addParentLevelsFor(levelOwner);
			isRejectsDeclared = levelOwner.getChildren(selector, kind).stream()
					.map(keyFunction)
					.filter(rejectCandidates::contains)
					.collect(Collectors.counting()) >= 1;
			if(parentLevel == null) {
				isRejectsDeclaredOrInherited = isRejectsDeclared;
			} else {
				isRejectsDeclaredOrInherited = isRejectsDeclared || parentLevel.isRejectsDeclaredOrInherited;
			}
		}

		private void calculateRejectCandidates(FrankElement levelOwner, Set<K> downstreamRejectCandidates) {
			rejectCandidates = levelOwner.getChildren(rejector, kind).stream()
					.map(keyFunction)
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

		boolean acceptsChildKey(K childKey) {
			return ! rejectCandidates.contains(childKey);
		}
	}

	void init(FrankElement subject) {
		addLevelsFor(subject, new HashSet<>());
	}

	List<ElementChild> getChildrenFor(FrankElement levelOwner) {
		return childrenFor(levelOwner, getChildKeysFor(levelOwner));
	}

	private List<ElementChild> childrenFor(FrankElement levelOwner, Set<K> childKeys) {
		return levelOwner.getChildren(ALL, kind).stream()
				.filter(c -> childKeys.contains(keyFunction.apply(c)))
				.collect(Collectors.toList());
	}

	private Set<K> getChildKeysFor(FrankElement levelOwner) {
		return levelOwner.getChildren(selector, kind).stream()
				.map(keyFunction)
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
