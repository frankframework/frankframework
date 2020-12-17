/* 
Copyright 2020 WeAreFrank! 

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

package nl.nn.adapterframework.doc.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;

public class ConfigChild extends ElementChild {
	private static final Comparator<SortNode> SORT_NODE_COMPARATOR =
			Comparator.comparing(SortNode::getSequenceInConfig)
			.thenComparing(SortNode::getName);

	static final class SortNode implements Comparable<SortNode> {
		private @Getter int sequenceInConfig = Integer.MAX_VALUE;
		private @Getter String name;
		private @Getter boolean documented;
		private @Getter boolean deprecated;
		private @Getter Method method;

		SortNode(Method method) {
			this.name = method.getName();
			this.method = method;
			this.documented = (method.getAnnotation(IbisDoc.class) != null);
			this.deprecated = isDeprecated(method);
		}

		void parseIbisDocAnnotation() throws IbisDocAnnotationException {
			IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
			if(ibisDoc == null) {
				throw new IbisDocAnnotationException(String.format(
						"No @IbisDoc annotation on method [%s]", name));
			}
			Integer optionalOrder = parseIbisDocAnnotation(ibisDoc);
			if(optionalOrder != null) {
				sequenceInConfig = optionalOrder;
			}
		}

		private Integer parseIbisDocAnnotation(IbisDoc ibisDoc) throws IbisDocAnnotationException {
			Integer result = null;
			if(ibisDoc.value().length >= 1) {
				try {
					result = Integer.valueOf(ibisDoc.value()[0]);
				} catch(Exception e) {
					throw new IbisDocAnnotationException(String.format(
							"@IbisDoc annotation on method [%s] has no valid order", name));
				}
			}
			return result;
		}

		private static boolean isDeprecated(Method m) {
			Deprecated deprecated = m.getAnnotation(Deprecated.class);
			return (deprecated != null);
		}

		@Override
		public int compareTo(SortNode other) {
			return SORT_NODE_COMPARATOR.compare(this, other);
		}
	}

	@SuppressWarnings("serial")
	public static class IbisDocAnnotationException extends Exception {		
		IbisDocAnnotationException(String message) {
			super(message);
		}

		IbisDocAnnotationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	@EqualsAndHashCode(callSuper = false)
	static final class Key extends AbstractKey {
		private final @Getter String syntax1Name;
		private final @Getter ElementType elementType;
		private final @Getter boolean mandatory;
		private final @Getter boolean allowMultiple;

		public Key(ConfigChild configChild) {
			syntax1Name = configChild.getSyntax1Name();
			elementType = configChild.getElementType();
			mandatory = configChild.isMandatory();
			allowMultiple = configChild.isAllowMultiple();
		}

		@Override
		public String toString() {
			List<String> components = new ArrayList<>();
			components.add(syntax1Name);
			components.add(elementType.getSimpleName());
			components.add("mandatory " + Boolean.toString(mandatory));
			components.add("multiple " + Boolean.toString(allowMultiple));
			return components.stream().collect(Collectors.joining(", "));
		}
	}

	private @Getter @Setter int sequenceInConfig;
	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter ElementRole elementRole;

	ConfigChild(FrankElement owningElement, SortNode sortNode) {
		super(owningElement);
		setDocumented(sortNode.isDocumented());
		setSequenceInConfig(sortNode.getSequenceInConfig());
		setDeprecated(sortNode.isDeprecated());
	}

	@Override
	Key getKey() {
		return new Key(this);
	}

	public String getSyntax1NamePlural() {
		if(getSyntax1Name().endsWith("s")) {
			return getSyntax1Name();
		} else {
			return getSyntax1Name() + "s";
		}
	}

	public String getSyntax1Name() {
		return elementRole.getSyntax1Name();
	}

	public ElementType getElementType() {
		return elementRole.getElementType();
	}

	void registerSyntax1NameWithElementType(final String syntax1Name) {
		if(IN_XSD.test(this)) {
			elementRole.getElementType().addConfigChildSyntax1Name(syntax1Name);
		}
	}

}
