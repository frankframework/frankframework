/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

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
