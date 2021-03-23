package nl.nn.adapterframework.doc.doclet;

final class TestUtil {
	private TestUtil() {
	}

	static FrankMethod getDeclaredMethodOf(FrankClass clazz, String methodName) {
		FrankMethod[] methods = clazz.getDeclaredMethods();
		for(FrankMethod m: methods) {
			if(m.getName().equals(methodName)) {
				return m;
			}
		}
		return null;
	}
}
