/*
   Copyright 2019, 2020 WeAreFrank!

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
package nl.nn.adapterframework.doc;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.doc.objects.ChildIbisBeanMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class DigesterRulesParser extends DefaultHandler {
    private String currentIbisBeanName;
    private List<ChildIbisBeanMapping> childIbisBeanMappings = new ArrayList<ChildIbisBeanMapping>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("pattern".equals(qName)) {
            String pattern = attributes.getValue("value");
            StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
            while (tokenizer.hasMoreElements()) {
                String token = tokenizer.nextToken();
                if (!"*".equals(token)) {
                    currentIbisBeanName = token;
                }
            }
        } else if ("set-next-rule".equals(qName)) {
            String methodName = attributes.getValue("methodname");
            childIbisBeanMappings.add(getChildIbisBeanMapping(methodName, currentIbisBeanName));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("pattern".equals(qName)) {
            currentIbisBeanName = null;
        }
    }

    public List<ChildIbisBeanMapping> getChildIbisBeanMappings() {
        return childIbisBeanMappings;
    }

    private static ChildIbisBeanMapping getChildIbisBeanMapping(String methodName, String childIbisBeanName) {
        ChildIbisBeanMapping result = new ChildIbisBeanMapping();
        result.setMaxOccurs(-1);
    	result.setMethodName(methodName);
        result.setChildIbisBeanName(childIbisBeanName);
        if (methodName.startsWith("set")) {
            result.setMaxOccurs(1);
        } else if (!(methodName.startsWith("add") || methodName.startsWith("register"))) {
            throw new RuntimeException("Unknow verb in method name: " + methodName);
        }
        return result;
    }
}
