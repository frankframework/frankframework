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
import java.util.Comparator;
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
	
	private Map<String,Integer> indexByQName=new LinkedHashMap<String,Integer>();
	private Map<String,Integer> indexByUriAndLocalName=new LinkedHashMap<String,Integer>();
	private List<Attribute> attributes = new ArrayList<Attribute>();
	
	private class Attribute {
		public String uri;
		public String localName;
		public String qName;
		public String type;
		public String value;
	}

	public AttributesWrapper(Attributes source, String localNameToSkip) {
		this(source, i->localNameToSkip==null || !localNameToSkip.equals(source.getLocalName(i)), false);
	}

	public AttributesWrapper(Attributes source, boolean sortAttributeOrder) {
		this(source, null, sortAttributeOrder);
	}

	protected AttributesWrapper(Attributes source, Function<Integer,Boolean> filter, boolean sortAttributeOrder) {
		for(int i=0;i<source.getLength();i++) {
			if (filter!=null && filter.apply(i)) {
				Attribute a = new Attribute();
				a.uri=source.getURI(i);
				a.localName=source.getLocalName(i);
				a.qName=source.getQName(i);
				a.type=source.getType(i);
				a.value=source.getValue(i);
				indexByQName.put(a.qName, i);
				indexByUriAndLocalName.put(a.uri+":"+a.localName, i);
				attributes.add(a);
			}
		}

		if(sortAttributeOrder) {
			attributes.sort(new Comparator<Attribute>() {
				@Override
				public int compare(Attribute o1, Attribute o2) {
					String o1Name = o1.localName;
					if ("".equals(o1Name)) {
						o1Name = o1.uri;
					}
	
					String o2Name = o2.localName;
					if ("".equals(o2Name)) {
						o2Name = o2.uri;
					}
					return o1Name.compareTo(o2Name);
				}
			});
		}
	}

	public AttributesWrapper(Attributes source) {
		this(source,null);
	}

	@Override
	public int getIndex(String qName) {
		return indexByQName.get(qName);
	}

	@Override
	public int getIndex(String uri, String localName) {
		return indexByUriAndLocalName.get(uri+":"+localName);
	}

	@Override
	public int getLength() {
		return attributes.size();
	}

	@Override
	public String getLocalName(int i) {
		return attributes.get(i).localName;
	}

	@Override
	public String getQName(int i) {
		return attributes.get(i).qName;
	}

	@Override
	public String getType(int i) {
		return attributes.get(i).type;
	}

	@Override
	public String getType(String qName) {
		return attributes.get(getIndex(qName)).type;
	}

	@Override
	public String getType(String uri, String localName) {
		return attributes.get(getIndex(uri,localName)).type;
	}

	@Override
	public String getURI(int i) {
		return attributes.get(i).uri;
	}

	@Override
	public String getValue(int i) {
		return attributes.get(i).value;
	}

	@Override
	public String getValue(String qName) {
		return attributes.get(getIndex(qName)).value;
	}

	@Override
	public String getValue(String uri, String localName) {
		return attributes.get(getIndex(uri,localName)).value;
	}

	public void remove(String uri, String localName) {
		int i=getIndex(uri,localName);
		if (i>=0) {
			attributes.remove(i);
		}
	}
}
