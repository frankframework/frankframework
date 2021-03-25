package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

	void checkRoot(FrankClass clazz) {
		delegate.checkRoot(clazz);
	}

	private static abstract class Delegate {
		abstract FrankClassRepository getRepository(String packageName);
		abstract void checkRoot(FrankClass clazz);
	}

	private static class ReflectionDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			return new FrankClassRepositoryReflect();
		}

		@Override
		void checkRoot(FrankClass clazz) {
			assertNotNull(clazz);
			assertEquals(FrankDocletConstants.OBJECT, clazz.getName());
		}
	}

	private static class DocletDelegate extends Delegate {
		@Override
		FrankClassRepository getRepository(String packageName) {
			ClassDoc[] classDocs = TestUtil.getClassDocs(packageName);
			return new FrankClassRepositoryDoclet(classDocs);
		}

		@Override
		void checkRoot(FrankClass clazz) {
			assertNull(clazz);
		}
	}
}
