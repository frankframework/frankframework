package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.sun.javadoc.ClassDoc;

public enum Environment {
	REFLECTION(new ReflectionDelegate()),
	DOCLET(new DocletDelegate());

	private final Delegate delegate;

	private Environment(final Delegate delegate) {
		this.delegate = delegate;
	}

	FrankClassRepository getRepository(String packageName) {
		return delegate.getRepository(packageName);
	}

	public FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters, List<String> excludeFiltersForSuperclass) {
		return delegate.getRepository(allPackages, includeFilters, excludeFilters, excludeFiltersForSuperclass);
	}

	private static abstract class Delegate {
		abstract FrankClassRepository getRepository(String packageName);
		abstract FrankClassRepository getRepository(
				List<String> allPackages, List<String> includeFilters, List<String> excludeFilters, List<String> excludeFiltersForSuperclass);
	}

	private static class ReflectionDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			return FrankClassRepository.getReflectInstance(packageName);
		}

		@Override
		FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters, List<String> excludeFiltersForSuperclass) {
			return FrankClassRepository.getReflectInstance(new HashSet<>(includeFilters), new HashSet<>(excludeFilters), new HashSet<>(excludeFiltersForSuperclass));
		}
	}

	private static class DocletDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			ClassDoc[] classDocs = TestUtil.getClassDocs(packageName);
			return FrankClassRepository.getDocletInstance(classDocs, new HashSet<>(Arrays.asList(packageName)), new HashSet<>(), new HashSet<>());
		}

		@Override
		FrankClassRepository getRepository(List<String> allPackages, List<String> includeFilters, List<String> excludeFilters, List<String> excludeFiltersForSuperclass) {
			ClassDoc[] classDocs = TestUtil.getClassDocs(allPackages.toArray(new String[] {}));
			return FrankClassRepository.getDocletInstance(classDocs, new HashSet<>(includeFilters), new HashSet<>(excludeFilters), new HashSet<>(excludeFiltersForSuperclass));
		}
	}
}
