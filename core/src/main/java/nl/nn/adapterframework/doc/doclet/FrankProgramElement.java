package nl.nn.adapterframework.doc.doclet;

public interface FrankProgramElement {
	String getName();
	boolean isPublic();
	FrankAnnotation[] getAnnotations();
	FrankAnnotation getAnnotation(String name);
}
