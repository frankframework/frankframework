/*
   Copyright 2013 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.extensions.sap.jco3;

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

import nl.nn.adapterframework.util.LogUtil;

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
		//log.debug("startElement("+localName+")");
		if (doc==null) {
			log.debug("creating Idoc ["+localName+"]");
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
					log.debug("creating segment ["+localName+"]");
					IDocSegment parentSegment = (IDocSegment)segmentStack.get(segmentStack.size()-1);
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
		//log.debug("endElement("+localName+")");
		if (currentField!=null) {
			String value=currentFieldValue.toString().trim();
			if (StringUtils.isNotEmpty(value)) {
				if (parsingEdiDcHeader) {
					if (log.isDebugEnabled()) log.debug("parsed header field ["+currentField+"] value ["+value+"]");
					try {
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
						// Not available anymore in JCo 3.0
						// else if (currentField.equals("EXPRSS")) { doc.setExpressFlag(value); }
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
					} catch (IDocConversionException e) {
						throw new SAXException("could not parse header field, idoc conversion exception", e);
					} catch (IDocSyntaxException e) {
						throw new SAXException("could not parse header field, idoc syntax exception", e);
					}
				} else {
					IDocSegment segment = (IDocSegment)segmentStack.get(segmentStack.size()-1);
					if (log.isDebugEnabled()) log.debug("setting field ["+currentField+"] to ["+value+"]");
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
