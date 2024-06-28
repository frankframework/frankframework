/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3.handlers;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecord;

import org.frankframework.util.LogUtil;

/**
 * Handler that serves as a base for other SAP XML element handlers.
 *
 * @author  Jaco de Groot
 * @since   5.0
 */
public abstract class Handler extends DefaultHandler {
	protected Logger log = LogUtil.getLogger(this);

	protected Handler childHandler;
	protected boolean parsedStringField = false;
	protected StringBuilder stringFieldValue = new StringBuilder();
	protected int unknownElementDepth = 0;
	protected boolean done = false;

	protected abstract void startElement(String localName);
	protected abstract void endElement(String localName);


	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (childHandler != null) {
			childHandler.startElement(namespaceURI, localName, qName, atts);
		} else {
			if (unknownElementDepth > 0) {
				unknownElementDepth++;
			} else {
				startElement(localName);
			}
		}
	}


	protected void startStringField(String localName, JCoRecord structure) {
		parsedStringField = true;
		stringFieldValue.setLength(0);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (childHandler != null) {
			childHandler.characters(ch, start, length);
		} else {
			if (parsedStringField) {
				stringFieldValue.append(ch,start,length);
			}
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (childHandler != null) {
			childHandler.endElement(namespaceURI, localName, qName);
			if (childHandler.done()) {
				childHandler = null;
			}
		} else {
			if (unknownElementDepth > 0) {
				unknownElementDepth--;
			} else {
				endElement(localName);
			}
		}
	}


	protected void endStringField(String localName, JCoRecord record) {
		if (record.getMetaData().hasField(localName)) {
			if(log.isTraceEnabled()) log.trace("setting field [{}] to value [{}]", localName, stringFieldValue);
			record.setValue(localName,stringFieldValue.toString());
		} else {
			log.warn("unknown field [{}] for value [{}]", localName, stringFieldValue);
		}
		parsedStringField = false;
	}

	protected void finished(String localName) {
		if(log.isTraceEnabled()) log.trace("finished parsing '{}'", localName);
		done = true;
	}

	protected boolean done() {
		return done;
	}

	public static Handler getHandler(List<JCoParameterList> parameterLists, Logger log) {
		log.debug("new RootHandler");
		return new RootHandler(parameterLists);
	}

	protected Handler getHandler(JCoParameterList jcoParameterList) {
		if(log.isDebugEnabled()) log.debug("new ParameterListHandler for '{}'", jcoParameterList.getMetaData().getName());
		return new ParameterListHandler(jcoParameterList);
	}

	protected Handler getHandler(JCoRecord jcoRecord, String fieldName) {
		return getHandler(jcoRecord, fieldName, false);
	}

	protected Handler getHandler(JCoRecord jcoRecord, String fieldName, boolean warnWhenNoHandler) {
		int jcoMetaDataType = jcoRecord.getMetaData().getType(fieldName);
		if (jcoMetaDataType == JCoMetaData.TYPE_TABLE) {
			if(log.isTraceEnabled()) log.trace("new TableHandler for '{}'", fieldName);
			return new TableHandler(jcoRecord.getTable(fieldName));
		} else if (jcoMetaDataType == JCoMetaData.TYPE_STRUCTURE) {
			if(log.isTraceEnabled()) log.trace("new StructureHandler for '{}'", fieldName);
			return new StructureHandler(jcoRecord.getStructure(fieldName));
		} else {
			if (warnWhenNoHandler) {
				String type = jcoRecord.getMetaData().getTypeAsString(fieldName);
				log.warn("no handler for type '{}'", type);
			}
			return null;
		}
	}

}
