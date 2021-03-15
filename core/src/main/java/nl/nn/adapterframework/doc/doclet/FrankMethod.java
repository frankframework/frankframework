package nl.nn.adapterframework.doc.doclet;

public interface FrankMethod extends FrankProgramElement {
	/**
	 * If the return type is void, return a {@link FrankPrimitiveType} wrapping "void".
	 */
	FrankType getReturnType();
	int getParameterCount();
	FrankType[] getParameterTypes();
	FrankAnnotation getAnnotationInludingInherited(String name) throws DocletReflectiveOperationException;
}
