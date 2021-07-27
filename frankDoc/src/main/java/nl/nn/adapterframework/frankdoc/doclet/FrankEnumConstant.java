package nl.nn.adapterframework.frankdoc.doclet;

public interface FrankEnumConstant extends FrankProgramElement {
	String getJavaDoc();
	FrankAnnotation getAnnotation(String name);
}
