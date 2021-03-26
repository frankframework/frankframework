package nl.nn.adapterframework.frankdoc.doclet;

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

	private static abstract class Delegate {
		abstract FrankClassRepository getRepository(String packageName);
	}

	private static class ReflectionDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			return new FrankClassRepositoryReflect();
		}
	}

	private static class DocletDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			ClassDoc[] classDocs = TestUtil.getClassDocs(packageName);
			return new FrankClassRepositoryDoclet(classDocs);
		}
	}
}
