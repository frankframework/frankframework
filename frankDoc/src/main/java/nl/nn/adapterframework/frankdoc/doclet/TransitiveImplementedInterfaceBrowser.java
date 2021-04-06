package nl.nn.adapterframework.frankdoc.doclet;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

class TransitiveImplementedInterfaceBrowser<T> {
	final Deque<FrankClass> interfazes = new ArrayDeque<>();
	final Set<String> interfaceNamesDone = new HashSet<>();

	TransitiveImplementedInterfaceBrowser(FrankClass clazz) throws FrankDocException {
		uniquelyEnqueueSuperInterfaces(clazz);
	}

	private void uniquelyEnqueueSuperInterfaces(FrankClass clazz) throws FrankDocException {
		Arrays.asList(clazz.getInterfaces()).forEach(this::enqueueUniquely);
	}

	private void enqueueUniquely(FrankClass clazz) {
		if(! interfaceNamesDone.contains(clazz.getName())) {
			interfaceNamesDone.add(clazz.getName());
			interfazes.addLast(clazz);
		}
	}

	T search(Function<FrankClass, T> testFunction) throws FrankDocException {
		while(! interfazes.isEmpty()) {
			FrankClass current = interfazes.removeFirst();
			T result = testFunction.apply(current);
			if(result != null) {
				return result;
			}
			uniquelyEnqueueSuperInterfaces(current);
		}
		return null;
	}
}
