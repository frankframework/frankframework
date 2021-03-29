package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.sun.javadoc.ClassDoc;

enum Environment {
	REFLECTION(new ReflectionDelegate()),
	DOCLET(new DocletDelegate());

	private final Delegate delegate;

	private Environment(final Delegate delegate) {
		this.delegate = delegate;
	}

	FrankClassRepository getRepository(String packageName) {
		return delegate.getRepository(packageName);
	}

	FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters) {
		return delegate.getRepository(allPackages, includeFilters, excludeFilters);
	}

	private static abstract class Delegate {
		abstract FrankClassRepository getRepository(String packageName);
		abstract FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters);
	}

	private static class ReflectionDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			return FrankClassRepository.getReflectInstance(packageName);
		}

		@Override
		FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters) {
			return FrankClassRepository.getReflectInstance(new HashSet<>(includeFilters), new HashSet<>(excludeFilters));
		}
	}

	private static class DocletDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			ClassDoc[] classDocs = TestUtil.getClassDocs(packageName);
			return FrankClassRepository.getDocletInstance(classDocs, new HashSet<>(Arrays.asList(packageName)), new HashSet<>());
		}

		@Override
		FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters) {
			ClassDoc[] classDocs = TestUtil.getClassDocs(allPackages.toArray(new String[] {}));
			return FrankClassRepository.getDocletInstance(classDocs, new HashSet<>(includeFilters), new HashSet<>(excludeFilters));
		}
	}
}
