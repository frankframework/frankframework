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

package nl.nn.adapterframework.frankdoc.front;

import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.*;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Taglet to fix the JavaDocs of the F!F, not the Frank!Doc. JavaDoc tag ff.parameter
 * needs a taglet because it can appear multiple times. The Maven config &lt;tag&gt; for
 * the JavaDoc Maven plugin is not sufficient. This class is the required taglet.
 * 
 * This class has been created by copying from https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/taglet/ToDoTaglet.java.
 * 
 * @author martijn
 *
 */
public class TagletFfParameter implements Taglet {
	private static Logger log = LogUtil.getLogger(TagletFfParameter.class);

	public String getName() {
		return FrankElement.JAVADOC_PARAMETER;
	}

	@Override
	public boolean inPackage() {
		return false;
	}

	@Override
	public boolean inConstructor() {
		return false;
	}

	@Override
	public boolean inMethod() {
		return false;
	}

	@Override
	public boolean inOverview() {
		return false;
	}

	@Override
	public boolean inField() {
		return false;
	}

	@Override
	public boolean inType() {
		return true;
	}

	@Override
	public boolean isInlineTag() {
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void register(Map tagletMap) {
		TagletFfParameter tag = new TagletFfParameter();
		Taglet t = (Taglet) tagletMap.get(tag.getName());
		if (t != null) {
			log.warn("Tag name [{}] is already in use, removing old taglet [{}]", tag.getName(), t.getClass().getName());
			tagletMap.remove(tag.getName());
		}
		log.trace("For tag name [{}], putting taglet [{}]", tag.getName(), tag.getClass().getName());
		tagletMap.put(tag.getName(), tag);
	}

	@Override
	public String toString(Tag tag) {
		return toString(new Tag[] {tag});
	}

	@Override
	public String toString(Tag[] tags) {
		if (tags.length == 0) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		result.append("\n\n<h2>Specific parameters</h2>\n\n");
		for(int i = 0; i < tags.length; ++i) {
			TagletFfParameterItem item = new TagletFfParameterItem(tags[i].text());
			result.append(String.format("<p><b>%s: </b>%s</p>\n", item.getParameterName(), item.getDescription()));
		}
		return result.toString();
	}
}
