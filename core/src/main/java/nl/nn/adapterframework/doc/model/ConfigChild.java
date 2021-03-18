/* 
Copyright 2020, 2021 WeAreFrank! 

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
import java.util.Comparator;

import org.springframework.core.annotation.AnnotationUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;

public class ConfigChild extends ElementChild implements Comparable<ConfigChild> {
	private static final Comparator<ConfigChild> CONFIG_CHILD_COMPARATOR =
			Comparator.comparingInt(ConfigChild::getOrder)
			.thenComparing(c -> c.getElementRole().getRoleName())
			.thenComparing(c -> c.getElementRole().getElementType().getFullName());

	static final class SortNode implements Comparable<SortNode> {
		private static final Comparator<SortNode> SORT_NODE_COMPARATOR =
				Comparator.comparing(SortNode::getName).thenComparing(sn -> sn.getElementTypeClass().getName());

		private @Getter String name;
		private @Getter boolean documented;
		private @Getter boolean deprecated;
		private @Getter Class<?> elementTypeClass;
		private @Getter IbisDoc ibisDoc;

		SortNode(Method method) {
			this.name = method.getName();
			this.documented = (method.getAnnotation(IbisDoc.class) != null);
			this.deprecated = isDeprecated(method);
			this.elementTypeClass = method.getParameterTypes()[0];
			this.ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);
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

	@EqualsAndHashCode(callSuper = false)
	static final class Key extends AbstractKey {
		private final @Getter String roleName;
		private final @Getter ElementType elementType;
		private final @Getter boolean mandatory;
		private final @Getter boolean allowMultiple;

		public Key(ConfigChild configChild) {
			roleName = configChild.getRoleName();
			elementType = configChild.getElementType();
			mandatory = configChild.isMandatory();
			allowMultiple = configChild.isAllowMultiple();
		}

		@Override
		public String toString() {
			return "(roleName=" + roleName + ", elementType=" + elementType + ", mandatory=" + mandatory
					+ ", allowMultiple=" + allowMultiple + ")";
		}
	}

	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter ElementRole elementRole;

	ConfigChild(FrankElement owningElement, SortNode sortNode) {
		super(owningElement);
		setDocumented(sortNode.isDocumented());
		setDeprecated(sortNode.isDeprecated());
	}

	@Override
	Key getKey() {
		return new Key(this);
	}

	public String getRoleName() {
		return elementRole.getRoleName();
	}

	public ElementType getElementType() {
		return elementRole.getElementType();
	}

	@Override
	public int compareTo(ConfigChild other) {
		return CONFIG_CHILD_COMPARATOR.compare(this, other);
	}
}
