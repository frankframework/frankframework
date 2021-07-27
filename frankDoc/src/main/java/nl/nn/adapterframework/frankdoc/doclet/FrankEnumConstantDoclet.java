package nl.nn.adapterframework.frankdoc.doclet;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.FieldDoc;

import lombok.Getter;

class FrankEnumConstantDoclet implements FrankEnumConstant {
	private @Getter String name;
	private boolean isPublic;
	private @Getter String javaDoc;
	private @Getter FrankAnnotation annotation;

	FrankEnumConstantDoclet(FieldDoc fieldDoc) {
		this.name = fieldDoc.name();
		this.isPublic = fieldDoc.isPublic();
		this.javaDoc = fieldDoc.commentText();
		AnnotationDesc[] javaDocAnnotations = fieldDoc.annotations();
		if(javaDocAnnotations.length >= 1) {
			annotation = new FrankAnnotationDoclet(javaDocAnnotations[0]);
		}
	}
	
	@Override
	public boolean isPublic() {
		return this.isPublic;
	}
}
