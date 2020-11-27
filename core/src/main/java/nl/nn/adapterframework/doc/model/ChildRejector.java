package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ChildRejector<K, T extends ElementChild<?>> {
	private final Function<FrankElement, List<T>> childFunction;
	private final Predicate<ElementChild<?>> selector;
	private final Predicate<ElementChild<?>> rejector;
	private final Function<T, K> keyFunction;
	private Map<String, Level> levels = new HashMap<>();

	ChildRejector(Function<FrankElement, List<T>> childFunction, Predicate<ElementChild<?>> selector, Predicate<ElementChild<?>> rejector, Function<T, K> keyFunction) {
		this.childFunction = childFunction;
		this.selector = selector;
		this.rejector = rejector;
		this.keyFunction = keyFunction;
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
			boolean hasRejectedDeclared = childFunction.apply(levelOwner).stream()
					.filter(selector)
					.map(keyFunction)
					.filter(rejectCandidates::contains)
					.collect(Collectors.counting()) >= 1;
			boolean hasDeclaredChildren = childFunction.apply(levelOwner).stream()
					.filter(selector)
					.collect(Collectors.counting()) >= 1;
			isRejectsDeclared = hasDeclaredChildren && hasRejectedDeclared;
			if(parentLevel == null) {
				isRejectsDeclaredOrInherited = isRejectsDeclared;
			} else {
				isRejectsDeclaredOrInherited = isRejectsDeclared || parentLevel.isRejectsDeclaredOrInherited;
			}
		}

		private void calculateRejectCandidates(FrankElement levelOwner, Set<K> downstreamRejectCandidates) {
			rejectCandidates = childFunction.apply(levelOwner).stream()
					.filter(rejector)
					.map(keyFunction)
					.collect(Collectors.toSet());
			rejectCandidates.addAll(downstreamRejectCandidates);
		}

		private Level addParentLevelsFor(FrankElement levelOwner) {
			Level parentLevel = null;
			FrankElement parent = getNextSelectedAncestor(levelOwner);
			if(parent != null) {
				parentLevel = addLevelsFor(parent, rejectCandidates);
			}
			return parentLevel;
		}

		boolean acceptsChildKey(K childKey) {
			return ! rejectCandidates.contains(childKey);
		}
	}

	FrankElement getNextSelectedAncestor(FrankElement elem) {
		FrankElement ancestor = elem.getParent();
		while((ancestor != null) && (numSelectedChildrenOf(ancestor) == 0)) {
			ancestor = ancestor.getParent();
		}
		return ancestor;		
	}

	private Long numSelectedChildrenOf(FrankElement element) {
		return childFunction.apply(element).stream().filter(selector).collect(Collectors.counting());
	}

	void init(FrankElement subject) {
		addLevelsFor(subject, new HashSet<>());
	}

	private Set<K> getChildKeysFor(FrankElement levelOwner) {
		return childFunction.apply(levelOwner).stream()
				.filter(selector)
				.map(keyFunction)
				.filter(k -> levels.get(levelOwner.getFullName()).acceptsChildKey(k))
				.collect(Collectors.toSet());
	}

	private List<T> childrenFor(FrankElement levelOwner, Set<K> childKeys) {
		return childFunction.apply(levelOwner).stream()
				.filter(c -> childKeys.contains(keyFunction.apply(c)))
				.collect(Collectors.toList());
	}

	List<T> getChildrenFor(FrankElement levelOwner) {
		return childrenFor(levelOwner, getChildKeysFor(levelOwner));
	}

	boolean isNoDeclaredRejected(FrankElement levelOwner) {
		return ! levels.get(levelOwner.getFullName()).isRejectsDeclared;
	}

	boolean isNoCumulativeRejected(FrankElement levelOwner) {
		return ! levels.get(levelOwner.getFullName()).isRejectsDeclaredOrInherited;
	}
}
