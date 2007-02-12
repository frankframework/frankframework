/*
 * $Log: Afm2EdiFactSender.java,v $
 * Revision 1.3  2007-02-12 13:47:04  europe\L190409
 * Logger from LogUtil
 *
 * Revision 1.2  2005/02/24 12:20:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.1  2005/02/02 16:37:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copied Afm2Edifact code from fb-broker
 *
 */
package nl.nn.adapterframework.extensions.afm;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Domparser om AFM-XML berichten om te zetten in edifactberichten (voor de backoffice).
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.afm.Afm2EdiFactSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestination(String) destination}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPostbus(String) postbus}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTpnummer(String) tpnummer}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *  
 * @author: Erik van de Wetering, fine tuned and wrapped for Ibis by Gerrit van Brakel
 */
public class Afm2EdiFactSender implements ISender {
	public static final String version="$RCSfile: Afm2EdiFactSender.java,v $ $Revision: 1.3 $ $Date: 2007-02-12 13:47:04 $";
	protected Logger logger = LogUtil.getLogger(this);

	public final static String VERWERKTAG = "VRWRKCD";
	public final static String TPNRTAG = "AL_RECCRT";
	
	private final static String contractRoot = "Contractdocument";
	private final static String mantelRoot = "Mantel";
	private final static String onderdeelRoot = "Onderdeel";

	private String destination = "   "; // 3 tekens
	private String tpnummer = "999999";
	// 6 tekens indien label AL_RECCRT ontbreekt
	private String postbus = "                "; //16 tekens

	private String name;

	public void configure() {
	}

	public void open() {
	}

	public void close() {
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message)	throws SenderException {
		try {
			return execute(message);
		} catch (Exception e) {
			throw new SenderException("transforming AFM-XML to EdiFact",e);
		}
	}

	private void appendArray(char aArray[], StringBuffer aRes) {
		String aStr = new String(aArray);
		appendString(aStr, aRes);
	}
	private void appendString(String aStr, StringBuffer aRes) {
		if (aStr != null) {
			String lHlpStr = aStr.trim();  //TODO: checken of dit wel klopt, stond zo in originele EvdW-code
			if (aStr.length() > 1) {
				aRes.append(aStr.intern() + "\r\n");
			}
		}
	}
	private boolean bevatWaarde(Node aNode) {
		String lWaarde = getWaardeForNode(aNode);
		boolean lRes = false;
		if ((lWaarde != null) && (!lWaarde.equalsIgnoreCase(""))) {
			lRes = true;
		}
		if (!lRes) {
			NodeList lList = aNode.getChildNodes();
			for (int i = 0; i <= lList.getLength() - 1; i++) {
				Node aSubNode = lList.item(i);
				lWaarde = getWaardeForNode(aNode);
				if ((lWaarde != null) && (!lWaarde.equalsIgnoreCase(""))) {
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
	private void closeList(StringBuffer aRes, int regelTeller) {
		// UNT
		char untRegel[] = new char[21];
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
		StringBuffer resultaat = new StringBuffer();
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
		char unzRegel[] = new char[23];
		for (int i = 0; i < 23; i++)
			unzRegel[i] = ' ';
		"UNZ000001".getChars(0, "UNZ000001".length(), unzRegel, 0);
		return unzRegel;
	}

	public char[] getInitResultaat() {
		// UNB
		char unbRegel[] = new char[206];
		for (int i = 0; i < 206; i++)
			unbRegel[i] = ' ';
		String lStart = "UNBUNOC1INFONET " + getDestination() + "     TP";
		lStart.getChars(0, lStart.length(), unbRegel, 0);
		getTpnummer().getChars(0, getTpnummer().length(), unbRegel, 26);
		String lPostbus = getPostbus();
		lPostbus.getChars(0, lPostbus.length(), unbRegel, 61);
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
		String dateTime = sdf.format(new Date());
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
		char unhRegel[] = new char[74];
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
	private StringBuffer HandleList(NodeList aList, StringBuffer aRes) {
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
	private int HandleSubList(NodeList aList, StringBuffer aRes, int regelTeller) {
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
							if (labelNaam.indexOf(VERWERKTAG) > -1) {
								//Verwerktags niet in edifact zetten
							} else {
								lHlp = "LBW" + labelNaam.substring(3);

								// Spaties toevoegen
								for (int lTel = lHlp.length(); lTel < 10; lTel++) {
									lHlp += " ";
								}
								String lWaarde = this.getWaardeForNode(aNode);
								if ((lWaarde != null)
									&& (!lWaarde.equalsIgnoreCase(""))) {
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

	public void setName(String name) {
		this.name=name;
	}
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
		logger.info("Tpnr: " + newTpnummer);
		tpnummer = newTpnummer;
	}
	public String getTpnummer() {
		return tpnummer;
	}
}
