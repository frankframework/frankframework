/*
   Copyright 2019, 2022 WeAreFrank!

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
package nl.nn.adapterframework.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.xml.sax.Attributes;

/**
 * Base class for transforming SAX Attributes-lists.
 *
 * @author Gerrit van Brakel
 *
 */
public class AttributesWrapper implements Attributes {

	private final Map<String, Integer> indexByQName = new LinkedHashMap<>();
	private final Map<String, Integer> indexByUriAndLocalName = new LinkedHashMap<>();
	private final List<Attribute> attributes = new ArrayList<>();

	protected static class Attribute {
		String uri;
		String localName;
		String qName;
		String type;
		String value;
	}

	public AttributesWrapper(Attributes source) {
		this(source, (String) null);
	}

	public AttributesWrapper(Attributes source, String localNameToSkip) {
		this(source, i -> localNameToSkip == null || !localNameToSkip.equals(source.getLocalName(i)), false, null);
	}

	public AttributesWrapper(Attributes source, boolean sortAttributeOrder) {
		this(source, null, sortAttributeOrder, null);
	}

	public AttributesWrapper(Attributes source, Function<String, String> valueTransformer) {
		this(source, null, false, valueTransformer);
	}

	protected AttributesWrapper(Attributes source, Function<Integer, Boolean> filter, boolean sortAttributeOrder, Function<String, String> valueTransformer) {
		int indexPos = 0;
		for (int i = 0; i < source.getLength(); i++) {
			if (filter == null || filter.apply(i)) {
				Attribute a = new Attribute();
				a.uri = source.getURI(i);
				a.localName = source.getLocalName(i);
				a.qName = source.getQName(i);
				a.type = source.getType(i);
				a.value = valueTransformer != null ? valueTransformer.apply(source.getValue(i)) : source.getValue(i);
				indexByQName.put(a.qName, indexPos);
				indexByUriAndLocalName.put(a.uri + ":" + a.localName, indexPos);
				attributes.add(a);
				indexPos++;
			}
		}

		if (sortAttributeOrder) {
			attributes.sort((o1, o2) -> {
				String o1Name = o1.localName;
				if ("".equals(o1Name)) {
					o1Name = o1.uri;
				}

				String o2Name = o2.localName;
				if ("".equals(o2Name)) {
					o2Name = o2.uri;
				}
				return o1Name.compareTo(o2Name);
			});
		}
	}

	@Override
	public int getIndex(String qName) {
		if (!indexByQName.containsKey(qName)) {
			return -1;
		}
		return indexByQName.get(qName);
	}

	@Override
	public int getIndex(String uri, String localName) {
		String key = uri + ":" + localName;
		if (!indexByUriAndLocalName.containsKey(key)) {
			return -1;
		}
		return indexByUriAndLocalName.get(key);
	}

	@Override
	public int getLength() {
		return attributes.size();
	}

	@Override
	public String getLocalName(int i) {
		if (i < 0 || i >= attributes.size()) {
			return null;
		}
		return attributes.get(i).localName;
	}

	@Override
	public String getQName(int i) {
		if (i < 0 || i >= attributes.size()) {
			return null;
		}
		return attributes.get(i).qName;
	}

	@Override
	public String getType(int i) {
		if (i < 0 || i >= attributes.size()) {
			return null;
		}
		return attributes.get(i).type;
	}

	@Override
	public String getType(String qName) {
		int index = getIndex(qName);
		if (index == -1) {
			return null;
		}
		return attributes.get(index).type;
	}

	@Override
	public String getType(String uri, String localName) {
		int index = getIndex(uri, localName);
		if (index == -1) {
			return null;
		}
		return attributes.get(index).type;
	}

	@Override
	public String getURI(int i) {
		if (i < 0 || i >= attributes.size()) {
			return null;
		}
		return attributes.get(i).uri;
	}

	@Override
	public String getValue(int i) {
		if (i < 0 || i >= attributes.size()) {
			return null;
		}
		return attributes.get(i).value;
	}

	@Override
	public String getValue(String qName) {
		int index = getIndex(qName);
		if (index == -1) {
			return null;
		}
		return attributes.get(index).value;
	}

	@Override
	public String getValue(String uri, String localName) {
		int index = getIndex(uri, localName);
		if (index == -1) {
			return null;
		}
		return attributes.get(index).value;
	}

	public void remove(String uri, String localName) {
		int i = getIndex(uri, localName);
		if (i >= 0) {
			attributes.remove(i);
		}
	}

	protected List<Attribute> getAttributes() {
		return attributes;
	}
}
