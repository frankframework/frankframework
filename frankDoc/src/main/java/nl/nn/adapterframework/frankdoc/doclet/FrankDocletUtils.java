package nl.nn.adapterframework.frankdoc.doclet;

import java.util.HashMap;
import java.util.Map;

import com.sun.javadoc.AnnotationDesc;

final class FrankDocletUtils {
	private FrankDocletUtils() {
	}

	static Map<String, FrankAnnotation> getFrankAnnotationsByName(AnnotationDesc[] annotationDescs) {
		Map<String, FrankAnnotation> annotationsByName = new HashMap<>();
		for(AnnotationDesc annotationDesc: annotationDescs) {
			FrankAnnotation frankAnnotation = new FrankAnnotationDoclet(annotationDesc);
			annotationsByName.put(frankAnnotation.getName(), frankAnnotation);
		}
		return annotationsByName;
	}
}
