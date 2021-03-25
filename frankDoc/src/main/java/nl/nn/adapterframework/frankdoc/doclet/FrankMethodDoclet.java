package nl.nn.adapterframework.frankdoc.doclet;

import java.util.ArrayList;
import java.util.List;

import com.sun.javadoc.MethodDoc;

class FrankMethodDoclet implements FrankMethod {
	private final MethodDoc method;
	private final FrankClassDoclet declaringClass;

	FrankMethodDoclet(MethodDoc method, FrankClassDoclet declaringClass) {
		this.method = method;
		this.declaringClass = declaringClass;
	}

	@Override
	public String getName() {
		return method.name();
	}

	@Override
	public boolean isPublic() {
		return method.isPublic();
	}

	@Override
	public FrankAnnotation[] getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FrankClass getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public FrankType getReturnType() {
		// TODO: Implement
		return null;
	}

	@Override
	public int getParameterCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public FrankType[] getParameterTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FrankAnnotation getAnnotationInludingInherited(String name) throws FrankDocException {
		// TODO Auto-generated method stub
		return null;
	}

	List<FrankMethod> removeOverriddenMethod(List<FrankMethod> from) {
		MethodDoc toRemove = method.overriddenMethod();
		List<FrankMethod> result = new ArrayList<>();
		for(FrankMethod candidate: from) {
			if(! ((FrankMethodDoclet) candidate).method.equals(toRemove)) {
				result.add(candidate);
			}
		}
		return result;
	}
}
