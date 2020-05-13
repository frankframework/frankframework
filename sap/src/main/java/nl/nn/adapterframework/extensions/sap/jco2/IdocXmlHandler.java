/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap.jco2;

import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.mw.idoc.IDoc;
import com.sap.mw.idoc.jco.JCoIDoc;

/**
 * DefaultHandler extension to parse SAP Idocs in XML format into JCoIDoc format.
 * 
 * @author  Gerrit van Brakel
 * @since   4.8  
 */
public class IdocXmlHandler extends DefaultHandler {
	protected Logger log = LogUtil.getLogger(this.getClass());
	
	private SapSystem sapSystem;
	private IDoc.Document doc=null;
	private List<IDoc.Segment> segmentStack=new ArrayList<IDoc.Segment>();
	private String currentField;
	private StringBuffer currentFieldValue=new StringBuffer();
	private boolean parsingEdiDcHeader=false;
	private Locator locator;
	
	public IdocXmlHandler(SapSystem sapSystem) {
		super();
		this.sapSystem=sapSystem;
	}
	
	public IDoc.Document getIdoc() {
		return doc;
	}


	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		//log.debug("startElement("+localName+")");
		if (doc==null) {
			log.debug("creating Idoc ["+localName+"]");
			doc = JCoIDoc.createDocument(sapSystem.getIDocRepository(), localName);
			IDoc.Segment segment = doc.getRootSegment();
			segmentStack.add(segment);
		} else {
			if (attributes.getIndex("SEGMENT")>=0) {
				if (localName.startsWith("EDI_DC")) {
					parsingEdiDcHeader=true;
				} else {
					log.debug("creating segment ["+localName+"]");
					IDoc.Segment parentSegment = (IDoc.Segment)segmentStack.get(segmentStack.size()-1);
					IDoc.Segment segment = parentSegment.addChild(localName);
					segmentStack.add(segment);
				}
			} else {
				currentField=localName;
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		//log.debug("endElement("+localName+")");
		if (currentField!=null) {
			String value=currentFieldValue.toString().trim();
			if (StringUtils.isNotEmpty(value)) {
				if (parsingEdiDcHeader) {
					if (log.isDebugEnabled()) log.debug("parsed header field ["+currentField+"] value ["+value+"]");
					if (currentField.equals("ARCKEY")) 		{ doc.setArchiveKey(value); }
					else if (currentField.equals("MANDT"))  { doc.setClient(value); }
					else if (currentField.equals("CREDAT")) { doc.setCreationDate(value); }
					else if (currentField.equals("CRETIM")) { doc.setCreationTime(value); }
					else if (currentField.equals("DIRECT")) { doc.setDirection(value); }
					else if (currentField.equals("REFMES")) { doc.setEDIMessage(value); }
					else if (currentField.equals("REFGRP")) { doc.setEDIMessageGroup(value); }
					else if (currentField.equals("STDMES")) { doc.setEDIMessageType(value); }
					else if (currentField.equals("STD"))    { doc.setEDIStandardFlag(value); }
					else if (currentField.equals("STDVRS")) { doc.setEDIStandardVersion(value); }
					else if (currentField.equals("REFINT")) { doc.setEDITransmissionFile(value); }
					else if (currentField.equals("EXPRSS")) { doc.setExpressFlag(value); }
					else if (currentField.equals("DOCTYP")) { doc.setIDocCompoundType(value); }
					else if (currentField.equals("DOCNUM")) { doc.setIDocNumber(value); }
					else if (currentField.equals("DOCREL")) { doc.setIDocSAPRelease(value); }
					else if (currentField.equals("IDOCTYP")){ doc.setIDocType(value); }
					else if (currentField.equals("CIMTYP")) { doc.setIDocTypeExtension(value); }
					else if (currentField.equals("MESCOD")) { doc.setMessageCode(value); }
					else if (currentField.equals("MESFCT")) { doc.setMessageFunction(value); }
					else if (currentField.equals("MESTYP")) { doc.setMessageType(value); }
					else if (currentField.equals("OUTMOD")) { doc.setOutputMode(value); }
					else if (currentField.equals("RCVSAD")) { doc.setRecipientAddress(value); }
					else if (currentField.equals("RCVLAD")) { doc.setRecipientLogicalAddress(value); }
					else if (currentField.equals("RCVPFC")) { doc.setRecipientPartnerFunction(value); }
					else if (currentField.equals("RCVPRN")) { doc.setRecipientPartnerNumber(value); }
					else if (currentField.equals("RCVPRT")) { doc.setRecipientPartnerType(value); }
					else if (currentField.equals("RCVPOR")) { doc.setRecipientPort(value); }
					else if (currentField.equals("SNDSAD")) { doc.setSenderAddress(value); }
					else if (currentField.equals("SNDLAD")) { doc.setSenderLogicalAddress(value); }
					else if (currentField.equals("SNDPFC")) { doc.setSenderPartnerFunction(value); }
					else if (currentField.equals("SNDPRN")) { doc.setSenderPartnerNumber(value); }
					else if (currentField.equals("SNDPRT")) { doc.setSenderPartnerType(value); }
					else if (currentField.equals("SNDPOR")) { doc.setSenderPort(value); }
					else if (currentField.equals("SERIAL")) { doc.setSerialization(value); }
					else if (currentField.equals("STATUS")) { doc.setStatus(value); }
					else if (currentField.equals("TEST"))   { doc.setTestFlag(value); }
					else {
						log.warn("header field ["+currentField+"] value ["+value+"] discarded");
					}
				} else {
					IDoc.Segment segment = (IDoc.Segment)segmentStack.get(segmentStack.size()-1);
					if (log.isDebugEnabled()) log.debug("setting field ["+currentField+"] to ["+value+"]");  
					segment.setField(currentField,value);
				}
			}
			currentField = null;
			currentFieldValue.setLength(0);
		} else {
			if (parsingEdiDcHeader) {
				parsingEdiDcHeader=false;
			} else {
				if (segmentStack.size()>0) {
					segmentStack.remove(segmentStack.size()-1);
				}
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (currentField==null) {
			String part=new String(ch,start,length).trim();
			if (StringUtils.isNotEmpty(part)) {
				throw new SAXParseException("found character content ["+part+"] outside a field", locator);
			}
			return;
		}
		currentFieldValue.append(ch,start,length);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		log.warn("Parser Error",e);
		super.error(e);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		log.warn("Parser FatalError",e);
		super.fatalError(e);
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		log.warn("Parser Warning",e);
		super.warning(e);
	}


	@Override
	public void setDocumentLocator(Locator locator) {
		super.setDocumentLocator(locator);
		this.locator=locator;
	}

}
