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

package nl.nn.adapterframework.frankdoc.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;

public class AttributeEnum {
	private @Getter String fullName;
	private String simpleName;
	private final @Getter List<AttributeEnumValue> values;
	private final Set<String> valueSet;
	private int seq;

	AttributeEnum(String fullName, String simpleName, List<AttributeEnumValue> values, int seq) {
		this.fullName = fullName;
		this.values = values;
		this.valueSet = values.stream().map(v -> v.getLabel()).collect(Collectors.toSet());
		this.simpleName = simpleName;
		this.seq = seq;
	}

	public String getUniqueName(String groupWord) {
		if(seq == 1) {
			return String.format("%s%s", simpleName, groupWord);
		} else {
			return String.format("%s%s_%d", simpleName, groupWord, seq);
		}
	}

	void typeCheck(String value) throws FrankDocException {
		if(! valueSet.contains(value)) {
			throw new FrankDocException(String.format("Value [%s] is not allowed, expected one of [%s]", value,
					valueSet.stream().collect(Collectors.joining(", "))), null);
		}
	}
}
