/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package org.frankframework.extensions.afm;

import java.text.DecimalFormat;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.doc.Category;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlUtils;

/**
 * Domparser om AFM-XML berichten om te zetten in edifactberichten (voor de backoffice).
 *
 * @author Erik van de Wetering, fine tuned and wrapped for Ibis by Gerrit van Brakel
 */
@Category(Category.Type.NN_SPECIAL)
public class Afm2EdiFactSender implements ISender {
	protected Logger logger = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public static final String VERWERKTAG = "VRWRKCD";
	public static final String TPNRTAG = "AL_RECCRT";

	private static final String contractRoot = "Contractdocument";
	private static final String mantelRoot = "Mantel";
	private static final String onderdeelRoot = "Onderdeel";

	private String destination = "   "; // 3 tekens
	private String tpnummer = "999999";
	// 6 tekens indien label AL_RECCRT ontbreekt
	private String postbus = "                "; //16 tekens

	private String name;

	@Override
	public void configure() {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
		try {
			return new SenderResult(execute(message.asString()));
		} catch (Exception e) {
			throw new SenderException("transforming AFM-XML to EdiFact",e);
		}
	}

	private void appendArray(char[] aArray, StringBuilder aRes) {
		String aStr = new String(aArray);
		appendString(aStr, aRes);
	}
	private void appendString(String aStr, StringBuilder aRes) {
		if (aStr != null) {
			String lHlpStr = aStr.trim();  //TODO: checken of dit wel klopt, stond zo in originele EvdW-code
			if (aStr.length() > 1) {
				aRes.append(aStr.intern() + "\r\n");
			}
		}
	}
	private boolean bevatWaarde(Node aNode) {
		String lWaarde = getWaardeForNode(aNode);
		boolean lRes = (lWaarde != null) && (!"".equalsIgnoreCase(lWaarde));
		if (!lRes) {
			NodeList lList = aNode.getChildNodes();
			for (int i = 0; i <= lList.getLength() - 1; i++) {
				Node aSubNode = lList.item(i);
				lWaarde = getWaardeForNode(aNode);
				if ((lWaarde != null) && (!"".equalsIgnoreCase(lWaarde))) {
					lRes = true;
					break;
				} else {
					boolean lHlpRes = bevatWaarde(aSubNode);
					if (lHlpRes) {
						lRes = lHlpRes;
						break;
					}
				}
			}
		}
		return lRes;
	}
	private void closeList(StringBuilder aRes, int regelTeller) {
		// UNT
		char[] untRegel = new char[21];
		for (int i = 0; i < 21; i++)
			untRegel[i] = ' ';
		"UNT".getChars(0, "UNT".length(), untRegel, 0);
		DecimalFormat df = new DecimalFormat("000000");
		regelTeller++; //de UNT Regel zelf
		df.format(regelTeller).getChars(0,df.format(regelTeller).length(),untRegel,3);
		appendArray(untRegel, aRes);
		regelTeller = 0;
	}
	public String execute(String aInput) throws DomBuilderException {
		Document doc = XmlUtils.buildDomDocument(aInput);

		NodeList contractList = doc.getElementsByTagName(contractRoot);
		NodeList mantelList = doc.getElementsByTagName(mantelRoot);
		NodeList onderdeelList = doc.getElementsByTagName(onderdeelRoot);
		NodeList tpNr = doc.getElementsByTagName(TPNRTAG);
		if (tpNr.getLength() > 0) {
			Node lHlpNode = tpNr.item(0);
			setTpnummer(getWaardeForNode(lHlpNode));
		}
		StringBuilder resultaat = new StringBuilder();
		//start
		this.appendArray(getInitResultaat(), resultaat);
		//docs
		this.HandleList(contractList, resultaat);
		this.HandleList(mantelList, resultaat);
		this.HandleList(onderdeelList, resultaat);
		//finish
		this.appendArray(getCloseResultaat(), resultaat);

		return resultaat.toString();
	}
	public char[] getCloseResultaat() {
		// UNZ
		char[] unzRegel = new char[23];
		for (int i = 0; i < 23; i++)
			unzRegel[i] = ' ';
		"UNZ000001".getChars(0, "UNZ000001".length(), unzRegel, 0);
		return unzRegel;
	}

	public char[] getInitResultaat() {
		// UNB
		char[] unbRegel = new char[206];
		for (int i = 0; i < 206; i++)
			unbRegel[i] = ' ';
		String lStart = "UNBUNOC1INFONET " + getDestination() + "     TP";
		lStart.getChars(0, lStart.length(), unbRegel, 0);
		getTpnummer().getChars(0, getTpnummer().length(), unbRegel, 26);
		String lPostbus = getPostbus();
		lPostbus.getChars(0, lPostbus.length(), unbRegel, 61);
		String dateTime = DateFormatUtils.now("yyMMddHHmm");
		dateTime.getChars(0, dateTime.length(), unbRegel, 114);
		"0".getChars(0, "0".length(), unbRegel, 169);
		"0".getChars(0, "0".length(), unbRegel, 205);
		return unbRegel;
	}
	private String getLabelNaam(String aLabel) {

		String lRes = aLabel;
		if (lRes != null) {
			if (lRes.startsWith("Q")) {
				lRes = "#" + lRes.substring(1);
			}
		}
		return lRes;
	}
	private char[] getNewDocInit() {
		char[] unhRegel = new char[74];
		for (int i = 0; i < 74; i++)
			unhRegel[i] = ' ';
		"UNH".getChars(0, "UNH".length(), unhRegel, 0);
		"INSLBW001000IN".getChars(0, "INSLBW001000IN".length(), unhRegel, 17);
		"00".getChars(0, "00".length(), unhRegel, 72);
		return unhRegel;
	}


	private String getVerwerkCdNaamForNode(Node aNode) {
		String lRes = aNode.getNodeName() + "_" + VERWERKTAG;
		return lRes;
	}
	private String getVerwerkCdWaarde(Node aNode) {
		NodeList aList = aNode.getChildNodes();
		String lRes = "";
		String verwerkCdNaam = this.getVerwerkCdNaamForNode(aNode);
		for (int i = 0; i <= aList.getLength() - 1; i++) {
			Node aChild = aList.item(i);
			if (verwerkCdNaam.equalsIgnoreCase(aChild.getNodeName())) {
				lRes = getWaardeForNode(aChild);
				break;
			}
		}
		return lRes;
	}
	private String getWaardeForNode(Node aNode) {
		String lRes = "";
		NodeList lList = aNode.getChildNodes();
		for (int i = 0; i <= lList.getLength() - 1; i++) {
			Node aSubNode = lList.item(i);
			if ((aSubNode.getNodeType() == Node.TEXT_NODE)
				|| (aSubNode.getNodeType() == Node.CDATA_SECTION_NODE)) {
				lRes = lRes + aSubNode.getNodeValue();
			}
		}
		return lRes;
	}
	private StringBuilder HandleList(NodeList aList, StringBuilder aRes) {
		if (aList != null) {
			if (aList.getLength() > 0) {
				for (int i = 0; i <= aList.getLength() - 1; i++) {
					int regelTeller = 1;
					this.appendArray(getNewDocInit(), aRes);
					Node aNode = aList.item(i);
					NodeList aSubList = aNode.getChildNodes();
					regelTeller = HandleSubList(aSubList, aRes, regelTeller);
					closeList(aRes,regelTeller);
				}
			}
		}
		return aRes;
	}
	private int HandleSubList(NodeList aList, StringBuilder aRes, int regelTeller) {
		String lHlp = "";
		if (aList != null) {
			for (int i = 0; i <= aList.getLength() - 1; i++) {
				Node aNode = aList.item(i);
				if (aNode.getNodeType() == Node.ELEMENT_NODE) {
					if (bevatWaarde(aNode)) {
						String labelNaam =
							this.getLabelNaam(aNode.getNodeName());
						if (labelNaam.length() == 2) {
							// Entiteit gevonden
							lHlp = "ENT" + labelNaam + getVerwerkCdWaarde(aNode);
							appendString(lHlp, aRes);
							regelTeller++;
							NodeList aSubList = aNode.getChildNodes();
							regelTeller = HandleSubList(aSubList, aRes, regelTeller);
						} else {
							if (labelNaam.contains(VERWERKTAG)) {
								//Verwerktags niet in edifact zetten
							} else {
								lHlp = "LBW" + labelNaam.substring(3);

								// Spaties toevoegen
								for (int lTel = lHlp.length(); lTel < 10; lTel++) {
									lHlp += " ";
								}
								String lWaarde = this.getWaardeForNode(aNode);
								if ((lWaarde != null)
									&& (!"".equalsIgnoreCase(lWaarde))) {
									lHlp = lHlp + lWaarde;
									this.appendString(lHlp, aRes);
									regelTeller++;
								}
							}
						}
					}
				}
			}
		}
		return regelTeller;
	}

	@Override
	public void setName(String name) {
		this.name=name;
	}
	@Override
	public String getName() {
		return name;
	}

	public void setDestination(String newDestination) {
		destination = newDestination;
	}
	public String getDestination() {
		return destination;
	}

	public void setPostbus(String newPostbus) {
		postbus = newPostbus;
	}
	public String getPostbus() {
		return postbus;
	}

	public void setTpnummer(String newTpnummer) {
		logger.info("Tpnr: {}", newTpnummer);
		tpnummer = newTpnummer;
	}
	public String getTpnummer() {
		return tpnummer;
	}
}
