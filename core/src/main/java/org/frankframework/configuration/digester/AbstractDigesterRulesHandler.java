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
package org.frankframework.configuration.digester;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.frankframework.util.LogUtil;

/**
 * @author Niels Meijer
 */
public abstract class AbstractDigesterRulesHandler extends DefaultHandler {
	protected final Logger log = LogUtil.getLogger(this);

	/**
	 * Parse all digester rules as {@link DigesterRule}
	 */
	@Override
	public final void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if("rule".equals(qName)) {
			DigesterRule rule = new DigesterRule();
			for (int i = 0; i < attributes.getLength(); ++i) {
				String method = attributes.getQName(i);
				String value = attributes.getValue(i);
				try {
					BeanUtils.setProperty(rule, method, value);
				} catch (IllegalAccessException | InvocationTargetException e) {
					log.warn("unable to set method [{}] with value [{}]", method, value);
				}
			}

			handle(rule);
		}
	}

	protected abstract void handle(DigesterRule rule) throws SAXException;
}
