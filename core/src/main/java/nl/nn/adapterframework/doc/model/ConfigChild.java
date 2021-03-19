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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigChild extends ElementChild implements Comparable<ConfigChild> {
	private static Logger log = LogUtil.getLogger(ConfigChild.class);

	private static final Comparator<ConfigChild> CONFIG_CHILD_COMPARATOR =
			Comparator.comparingInt(ConfigChild::getOrder)
			.thenComparing(c -> c.getElementRole().getRoleName())
			.thenComparing(c -> c.getElementRole().getElementType().getFullName());

	private static final Comparator<ConfigChild> INVERSE_ALLOW_MULTIPLE =
			Comparator.comparing(c -> ! c.isAllowMultiple());

	private static final Comparator<ConfigChild> REMOVE_DUPLICATES_COMPARATOR =
			INVERSE_ALLOW_MULTIPLE.thenComparing(c -> ! c.isMandatory());

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

		public Key(ConfigChild configChild) {
			roleName = configChild.getRoleName();
			elementType = configChild.getElementType();
		}

		@Override
		public String toString() {
			return "(roleName=" + roleName + ", elementType=" + elementType + ")";
		}
	}

	private @Getter @Setter boolean mandatory;
	private @Getter @Setter boolean allowMultiple;
	private @Getter @Setter ElementRole elementRole;
	private String methodName;
	private boolean isOverrideMeaningfulLogged = false;

	ConfigChild(FrankElement owningElement, SortNode sortNode) {
		super(owningElement);
		setDocumented(sortNode.isDocumented());
		setDeprecated(sortNode.isDeprecated());
		this.methodName = sortNode.name;
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

	/**
	 * Removes duplicate config children. As input this method expects a list of config children
	 * found from the declared methods of a Java class. The isMandatory() and isAllowMultiple()
	 * properties of a config child follow from the name of the Java method. Therefore, the
	 * input config children are unique by the combination of role name, ElementType,
	 * isAllowMultiple() and isMandatory().
	 * <p>
	 * Now consider Java class {@link nl.nn.adapterframework.senders.SenderSeries}. It both has methods
	 * setSender() and registerSender(), which would cause a duplicate config child. If both would be
	 * included, the XSDs would define multiple times that a SenderSeries can have a sender as child.
	 * This method makes the config children unique by role name and element type, which means
	 * unique by {@link nl.nn.adapterframework.doc.model.ElementRole}.
	 */
	static List<ConfigChild> removeDuplicates(List<ConfigChild> orig) {
		Map<Key, List<ConfigChild>> byKey = orig.stream().collect(Collectors.groupingBy(Key::new));
		List<ConfigChild> result = new ArrayList<>();
		for(Key key: byKey.keySet()) {
			List<ConfigChild> bucket = new ArrayList<>(byKey.get(key));
			Collections.sort(bucket, REMOVE_DUPLICATES_COMPARATOR);
			ConfigChild selected = bucket.get(0);
			result.add(selected);
			if(log.isTraceEnabled() && (bucket.size() >= 2)) {
				for(ConfigChild omitted: bucket.subList(1, bucket.size())) {
					log.trace("Omitting config child {} because it is a duplicate of {}", omitted.toString(), selected.toString());
				}
			}
		}
		return result;
	}

	@Override
	boolean checkOverrideMeaningful(ElementChild overriddenFrom) {
		ConfigChild match = (ConfigChild) overriddenFrom;
		boolean result = (allowMultiple != match.allowMultiple) || (mandatory != match.mandatory);
		if(log.isTraceEnabled() && (! isOverrideMeaningfulLogged) && result) {
			isOverrideMeaningfulLogged = true;
			log.trace("Config {} overrides {} and changes isAllowMultiple() or isMandatory()", toString(), overriddenFrom.toString());
		}
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s.%s(%s)", getOwningElement().getSimpleName(), methodName, getElementType().getSimpleName());
	}
}
