/*
   Copyright 2013 Nationale-Nederlanden, 2022-2023, 2026 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.conn.idoc.IDocConversionException;
import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocFieldNotFoundException;
import com.sap.conn.idoc.IDocIllegalTypeException;
import com.sap.conn.idoc.IDocMetaDataUnavailableException;
import com.sap.conn.idoc.IDocSegment;
import com.sap.conn.idoc.IDocSyntaxException;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.jco.JCoException;

import org.frankframework.util.LogUtil;

/**
 * DefaultHandler extension to parse SAP Idocs in XML format into JCoIDoc format.
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public class IdocXmlHandler extends DefaultHandler {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private final SapSystemImpl sapSystem;
	private IDocDocument doc=null;
	private final List<IDocSegment> segmentStack = new ArrayList<>();
	private String currentField;
	private final StringBuilder currentFieldValue = new StringBuilder();
	private boolean parsingEdiDcHeader=false;
	private Locator locator;

	public IdocXmlHandler(SapSystemImpl sapSystem) {
		super();
		this.sapSystem=sapSystem;
	}

	public IDocDocument getIdoc() {
		return doc;
	}


	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// log.debug("startElement("+localName+")");
		if (doc==null) {
			log.debug("creating Idoc [{}]", localName);
			try {
				doc = JCoIDoc.getIDocFactory().createIDocDocument(sapSystem.getIDocRepository(), localName);
			} catch (IDocMetaDataUnavailableException e) {
				throw new SAXException("could not create idoc document, idoc meta data unavailable", e);
			} catch (JCoException e) {
				throw new SAXException("could not create idoc document", e);
			}
			IDocSegment segment = doc.getRootSegment();
			segmentStack.add(segment);
		} else {
			if (attributes.getIndex("SEGMENT")>=0) {
				if (localName.startsWith("EDI_DC")) {
					parsingEdiDcHeader=true;
				} else {
					log.debug("creating segment [{}]", localName);
					IDocSegment parentSegment = segmentStack.get(segmentStack.size()-1);
					IDocSegment segment;
					try {
						segment = parentSegment.addChild(localName);
					} catch (IDocIllegalTypeException e) {
						throw new SAXException("could not parse segment, idoc illegal type", e);
					} catch (IDocMetaDataUnavailableException e) {
						throw new SAXException("could not parse segment, idoc meta data unavailable", e);
					}
					segmentStack.add(segment);
				}
			} else {
				currentField=localName;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// log.debug("endElement("+localName+")");
		if (currentField!=null) {
			String value=currentFieldValue.toString().trim();
			if (StringUtils.isNotEmpty(value)) {
				if (parsingEdiDcHeader) {
					if (log.isDebugEnabled()) log.debug("parsed header field [{}] value [{}]", currentField, value);
					try {
						switch (currentField) {
							case "ARCKEY" -> doc.setArchiveKey(value);
							case "MANDT" -> doc.setClient(value);
							case "CREDAT" -> doc.setCreationDate(value);
							case "CRETIM" -> doc.setCreationTime(value);
							case "DIRECT" -> doc.setDirection(value);
							case "REFMES" -> doc.setEDIMessage(value);
							case "REFGRP" -> doc.setEDIMessageGroup(value);
							case "STDMES" -> doc.setEDIMessageType(value);
							case "STD" -> doc.setEDIStandardFlag(value);
							case "STDVRS" -> doc.setEDIStandardVersion(value);
							case "REFINT" -> doc.setEDITransmissionFile(value);

							// Not available anymore in JCo 3.0
							// else if (currentField.equals("EXPRSS")) { doc.setExpressFlag(value); }
							case "DOCTYP" -> doc.setIDocCompoundType(value);
							case "DOCNUM" -> doc.setIDocNumber(value);
							case "DOCREL" -> doc.setIDocSAPRelease(value);
							case "IDOCTYP" -> doc.setIDocType(value);
							case "CIMTYP" -> doc.setIDocTypeExtension(value);
							case "MESCOD" -> doc.setMessageCode(value);
							case "MESFCT" -> doc.setMessageFunction(value);
							case "MESTYP" -> doc.setMessageType(value);
							case "OUTMOD" -> doc.setOutputMode(value);
							case "RCVSAD" -> doc.setRecipientAddress(value);
							case "RCVLAD" -> doc.setRecipientLogicalAddress(value);
							case "RCVPFC" -> doc.setRecipientPartnerFunction(value);
							case "RCVPRN" -> doc.setRecipientPartnerNumber(value);
							case "RCVPRT" -> doc.setRecipientPartnerType(value);
							case "RCVPOR" -> doc.setRecipientPort(value);
							case "SNDSAD" -> doc.setSenderAddress(value);
							case "SNDLAD" -> doc.setSenderLogicalAddress(value);
							case "SNDPFC" -> doc.setSenderPartnerFunction(value);
							case "SNDPRN" -> doc.setSenderPartnerNumber(value);
							case "SNDPRT" -> doc.setSenderPartnerType(value);
							case "SNDPOR" -> doc.setSenderPort(value);
							case "SERIAL" -> doc.setSerialization(value);
							case "STATUS" -> doc.setStatus(value);
							case "TEST" -> doc.setTestFlag(value);
							case null, default -> log.warn("header field [{}] value [{}] discarded", currentField, value);
						}
					} catch (IDocConversionException e) {
						throw new SAXException("could not parse header field, idoc conversion exception", e);
					} catch (IDocSyntaxException e) {
						throw new SAXException("could not parse header field, idoc syntax exception", e);
					}
				} else {
					IDocSegment segment = segmentStack.get(segmentStack.size()-1);
					if (log.isDebugEnabled()) log.debug("setting field [{}] to [{}]", currentField, value);
					try {
						segment.setValue(currentField,value);
					} catch (IDocFieldNotFoundException e) {
						throw new SAXException("could not set field ["+currentField+"] to ["+value+"], idoc field not found", e);
					} catch (IDocConversionException e) {
						throw new SAXException("could not set field ["+currentField+"] to ["+value+"], idoc conversion exception", e);
					} catch (IDocSyntaxException e) {
						throw new SAXException("could not set field ["+currentField+"] to ["+value+"], idoc syntax exception", e);
					}
				}
			}
			currentField = null;
			currentFieldValue.setLength(0);
		} else {
			if (parsingEdiDcHeader) {
				parsingEdiDcHeader=false;
			} else {
				if (!segmentStack.isEmpty()) {
					segmentStack.removeLast();
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
