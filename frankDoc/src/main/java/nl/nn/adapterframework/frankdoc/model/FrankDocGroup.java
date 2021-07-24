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

package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;

public class FrankDocGroup implements Comparable<FrankDocGroup> {
	private static Logger log = LogUtil.getLogger(FrankDocGroup.class);

	private static final Comparator<FrankDocGroup> COMPARATOR =
			Comparator.comparingInt(FrankDocGroup::getOrder).thenComparing(FrankDocGroup::getName);
	public static String GROUP_NAME_OTHER = "Other";

	private final @Getter String name;
	private @Getter int order = Integer.MAX_VALUE;
	private @Getter @Setter(AccessLevel.PACKAGE) List<ElementType> elementTypes = new ArrayList<>();

	FrankDocGroup(String name) {
		this.name = name;
	}

	void setOrder(int newOrder) {
		if(newOrder == Integer.MAX_VALUE) {
			return;
		}
		if(order == Integer.MAX_VALUE) {
			order = newOrder;
		} else if(newOrder != order) {
			int selectedOrder = Math.min(order, newOrder);
			// Adding the class name that causes the conflict requires us to
			// make the class name available. Adding this feature is not worth
			// the extra complexity of the code.
			log.warn("Conflicting order values for group {}. Old {}, new {}, selected {}", name, order, newOrder, selectedOrder);
			order = selectedOrder;
		}
	}

	@Override
	public int compareTo(FrankDocGroup other) {
		return COMPARATOR.compare(this, other);
	}
}
