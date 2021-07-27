package nl.nn.adapterframework.frankdoc.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.frankdoc.doclet.FrankAnnotation;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.FrankEnumConstant;
import nl.nn.adapterframework.util.LogUtil;

public class AttributeValue {
	private static Logger log = LogUtil.getLogger(AttributeValue.class);

	private static final String ENUM_LABEL = EnumLabel.class.getName();

	private @Getter boolean explicitLabel = false;
	private @Getter String javaTag;
	private @Getter String label;
	private @Getter String description;

	AttributeValue(FrankEnumConstant c) {
		this.javaTag = c.getName();
		this.label = this.javaTag;
		FrankAnnotation annotation = c.getAnnotation(ENUM_LABEL);
		String annotationValue = null;
		if(annotation != null) {
			try {
				annotationValue = (String) annotation.getValue();
			} catch(FrankDocException e) {
				log.warn("Could not parse annotation value of {}", ENUM_LABEL, e);
			}			
		}
		if(! StringUtils.isBlank(annotationValue)) {
			this.explicitLabel = true;
			this.label = annotationValue;
		}
		String javaDoc = c.getJavaDoc();
		if(! StringUtils.isBlank(javaDoc)) {
			this.description = javaDoc;
		}
	}
}
