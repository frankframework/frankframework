package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Map;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.FieldDoc;

import lombok.Getter;

class FrankEnumConstantDoclet implements FrankEnumConstant {
	private @Getter String name;
	private boolean isPublic;
	private @Getter String javaDoc;
	private Map<String, FrankAnnotation> annotationsByName;

	FrankEnumConstantDoclet(FieldDoc fieldDoc) {
		this.name = fieldDoc.name();
		this.isPublic = fieldDoc.isPublic();
		this.javaDoc = fieldDoc.commentText();
		AnnotationDesc[] javaDocAnnotations = fieldDoc.annotations();
		annotationsByName = FrankDocletUtils.getFrankAnnotationsByName(javaDocAnnotations);
	}
	
	@Override
	public boolean isPublic() {
		return this.isPublic;
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		return annotationsByName.get(name);
	}
}
