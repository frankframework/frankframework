/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.extensions.rekenbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.Forward;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Transforms between ascii-ADIOS and an XML representation of ADIOS.
 *
 * <p>
 * Sample xml:<br/>
 * <pre>{@code
 * <adios rekenbox="L76HB150">
 *     <rubriek naam="BER_VERZ_CD" waarde="COMBIFLEX_BELEGGING" />
 *     <rubriek naam="INBR_CD" waarde="NIEUWE_VERZEKERING" />
 *     <rubriek naam="PENS_DT_BEP_CD"  waarde="DT_UIT_PENS_LFT" />
 *     <rubriek nummer="313" naam="AS_OPSL_PRD_TRM_PRM" index="3" recordnr="74" record="VUT_VERZEKERING" waarde="52.34" />
 *     ...
 * </adios>
 * }</pre>
 * <br/>
 * For input, a 'naam' or a 'nummer'-attribute must be specified. If both are specified, their match is checked.
 * On output, 'nummer', 'naam' and 'waarde'-attributes are always present in each rubriek-element.
 * Where applicable 'index', 'recordnr', 'record' and 'recordindex' are present, too.
 * If sub-records exist, they are present with a 'sub' prefix to all attributes.
 * </p>
 *
 * @author Gerrit van Brakel
 */
@Forward(name = "noConversionForwardName", description = "when successful, but no conversion took place")
@Category(Category.Type.NN_SPECIAL)
public class Adios2XmlPipe extends FixedForwardPipe {

	private Hashtable rubriek2nummer;
	private Hashtable record2nummer;
	private Hashtable nummer2rubriek;
	private Hashtable nummer2record;

	protected static final String recordIdentifier = "RECORDS";
	protected static final String rubriekIdentifier = "rubriek";

	private String adiosDefinities="nnrscons.pas";
	private String rekenbox=null;
	private String rekenboxSessionKey=null;
	private String noConversionForwardName="noconversion";

	private Xml2AdiosHandler handler;
	private SAXParser saxParser;

	private PipeForward noConversionForward = null;

	private Direction direction=null;
	public enum Direction {
		/** Transform an Adios-XML file to ASCII-Adios */
		Xml2Adios,
		/** Transform an ASCII-Adios file to Adios-XML */
		Adios2Xml
	}

	class Xml2AdiosHandler extends DefaultHandler {
		private final StringBuilder result = new StringBuilder(16 * 1024);

		public String getResult() {
			return result.toString();
		}

		public void clear() {
			result.setLength(0);
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

			String elementName = localName;
			if(elementName == null || "".equals(elementName)) {
				elementName = qName;
			}
			// log.debug("elementName ["+elementName+"]:
			// @nummer=["+attributes.getValue("nummer")+"]
			// @naam=["+attributes.getValue("naam")+"]
			// @index=["+attributes.getValue("index")+"]");

			if("rubriek".equals(elementName)) {
				String nummer = attributes.getValue("nummer");
				String naam = attributes.getValue("naam");
				String index = attributes.getValue("index");
				String recordnr = attributes.getValue("recordnr");
				String record = attributes.getValue("record");
				String recordindex = attributes.getValue("recordindex");
				String waarde = attributes.getValue("waarde");

				// find nummer from naam
				if(naam != null && !"".equals(naam)) {
					String nummerByNaam = (String) rubriek2nummer.get(naam);
					if(nummerByNaam == null) {
						throw new SAXException("cannot find nummer for [" + naam + "] in rubriek");
					}
					// check if nummer from naam matches nummer in rubriek, if present
					if(nummer != null && !"".equals(nummer) && !nummer.equals(nummerByNaam)) {
						throw new SAXException("nummer [" + nummerByNaam + "] found for naam [" + naam + "] does not match nummer [" + nummer + "] in rubriek");
					}
					nummer = nummerByNaam;
				}
				if(nummer == null || "".equals(nummer)) {
					throw new SAXException("cannot find 'naam' or 'nummer' in rubriek");
				}

				// find recordnr from recordname
				if(record != null && !"".equals(record)) {
					String nummerByNaam = (String) record2nummer.get(record);
					if(nummerByNaam == null) {
						throw new SAXException("cannot find recordnr for record [" + record + "] in rubriek");
					}
					// check if recordnr from recordname matches recordnr in rubriek, if present
					if(recordnr != null && !"".equals(recordnr) && !recordnr.equals(nummerByNaam)) {
						throw new SAXException("recordnr [" + nummerByNaam + "] found for record [" + record + "] does not match recordnr [" + recordnr + "] in rubriek");
					}
					recordnr = nummerByNaam;
				}
				if("".equals(recordnr)) {
					recordnr = null;
				}

				if(recordnr != null) {
					result.append(recordnr);
					if(recordindex != null && !"".equals(recordindex))
						result.append("[").append(recordindex).append("]");
					result.append(",");
				}
				result.append(nummer);
				if(index != null && !"".equals(index)) {
					result.append("[").append(index).append("]");
				}
				result.append(":").append(waarde).append(";").append(SystemUtils.LINE_SEPARATOR);
			} else if("adios".equals(elementName)) {
				result.append(attributes.getValue("rekenbox") + ":;" + SystemUtils.LINE_SEPARATOR);
			}
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getNoConversionForwardName())) {
			noConversionForward = findForward(getNoConversionForwardName());
		}
		if (noConversionForward==null) {
			noConversionForward=getSuccessForward();
			log.info("no forward found for [{}], setting to forward for success [{}]", getNoConversionForwardName(), getSuccessForward().getPath());
		}
		initializeConversionTables();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;

		try {
			if (getDirection() == Direction.Xml2Adios) {
				result = makeAdios(message.asInputSource(),session);
			} else {
				String inputstring = message.asString();
				String firstToken = new StringTokenizer(inputstring).nextToken();
				if (firstToken.startsWith("<")) {
					log.info("input is already XML, no conversion performed");
					return new PipeRunResult(noConversionForward, inputstring);
				}
				result = makeXml(message.asString(),session);
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	/**
	 * Checks if a String consists of digits only hence it is a label number
	 */
	private boolean alldigits(String s) {
		boolean tr;
		int i;
		char c;

		tr = true;
		for(i = 0; i <= s.length() - 1; i++) {
			c = s.charAt(i);
			tr = tr && Character.isDigit(c);
		}
		return tr;
	}

	protected void initializeConversionTables() throws ConfigurationException	{
		// lees de template file en store het in een hashtable
		if (StringUtils.isNotEmpty(getAdiosDefinities())) {

			rubriek2nummer = new Hashtable(3000);
			record2nummer  = new Hashtable(1000);
			nummer2rubriek = new Hashtable(3000);
			nummer2record = new Hashtable(1000);

			try {
				handler = new Xml2AdiosHandler();
				SAXParserFactory parserFactory = XmlUtils.getSAXParserFactory();
				saxParser = parserFactory.newSAXParser();
			} catch (Throwable e) {
				throw new ConfigurationException("cannot configure a parser", e);
			}

			try {
				URL url = ClassLoaderUtils.getResourceURL(this, getAdiosDefinities());
				if(url == null) {
					throw new ConfigurationException("cannot find adios definitions from resource [" + getAdiosDefinities() + "]");
				}
				BufferedReader bufinput = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(url.openStream()));
				String line, labelnr, waarde;

				line = bufinput.readLine();

				labelnr = "";
				waarde = "";

				// read in the rubrieken
				while(line != null && !waarde.equals(recordIdentifier)) {
					StringTokenizer st = new StringTokenizer(line, "{};= \n");
					if(st.countTokens() >= 1) {
						waarde = st.nextToken();
						if(!waarde.equals(recordIdentifier)) {
							waarde = waarde.substring(3);
						}
						if(st.hasMoreTokens()) {
							labelnr = st.nextToken();
							if(alldigits(labelnr)) {
								// als de key al bestaat betekend dit dat er een fout zit in de invoer
								if(nummer2rubriek.containsKey(labelnr)) {
									throw new ConfigurationException("rubriek [" + labelnr + "] komt meermaals voor. Waarde1: [" + nummer2rubriek.get(labelnr) + "], Waarde2: [" + waarde + "]");
								}
								nummer2rubriek.put(labelnr, waarde);
								rubriek2nummer.put(waarde, labelnr);
							}
						}

					}
					line = bufinput.readLine();
				}

				// Read in the records
				while(line != null) {
					StringTokenizer st1 = new StringTokenizer(line, "{};= \n");
					if(st1.countTokens() >= 1) {
						waarde = st1.nextToken();
						waarde = waarde.substring(3);
						if(st1.hasMoreTokens()) {
							labelnr = st1.nextToken();
							if(alldigits(labelnr)) {
								// labeln = Integer.parseInt(labelnr);
								if(nummer2record.containsKey(labelnr)) {
									throw new ConfigurationException("record [" + labelnr + "] komt meermaals voor. Waarde1: [" + nummer2record.get(labelnr) + "], Waarde2: [" + waarde + "]");
								}
								nummer2record.put(labelnr, waarde);
								record2nummer.put(waarde, labelnr);
							}
						}

					}

					line = bufinput.readLine();
				}
				bufinput.close();
			} catch (IOException e) {
				throw new ConfigurationException("IOException on [" + getAdiosDefinities() + "]", e);
			}
		}
	}

	public String findRekenbox(PipeLineSession session) {
		if(getRekenboxSessionKey() != null) {
			return session.getString(getRekenboxSessionKey());
		}
		return getRekenbox();
	}

	public String makeAdios(InputSource bericht, PipeLineSession session) throws PipeRunException {
		try {
			// Parse the input
			handler.clear();
			saxParser.parse(bericht, handler);
			return handler.getResult();

		} catch (Throwable t) {
			throw new PipeRunException(this, "got error while transforming xml to adios, input [" + bericht + "]", t);
		}
	}

	/**
	 * The calcbox tool Adios exports the file "NNRSCONS.PAS" = adiosDefinities in configuration attributes.
	 * this file containts both "rubrieken" and "records" which together form
	 * the adios message te be send to the calcbox
	 * the most difficult format used is record[recordindex],label[index]:waarde;
	 * mind the delimiters,where a record has or hasnot an indexnummer and a label likewise.
	 */
	public String makeXml(String s, PipeLineSession session) throws PipeRunException {

		XmlBuilder bericht = new XmlBuilder("adios");
		bericht.addAttribute("type", "rekenuitvoer");
		String rekenbox = findRekenbox(session);
		if (rekenbox != null) {
			bericht.addAttribute("rekenbox", rekenbox);
		}

		StringTokenizer st1 = new StringTokenizer(s,";\n\r");
		while (st1.hasMoreTokens()) {
			String regel=st1.nextToken();
			StringTokenizer st2 = new StringTokenizer(regel,":");
			if (st2.hasMoreTokens()) {
				String label = st2.nextToken();
				//log.debug("label ["+label+"]");
				String waarde;

				if (regel.length()>label.length()) {
					waarde = regel.substring(regel.indexOf(':')+1); // 'waarde' might contain colons, so nextToken() doesn't work correctly
				} else {
					waarde="NVT";
				}
				waarde = waarde.trim();

				XmlBuilder rubriek = new XmlBuilder("rubriek");
				String prefix = "";

				StringTokenizer st3 = new StringTokenizer(label,",");
				while(st3.hasMoreTokens()) {
					String item = st3.nextToken();
					//log.debug("item ["+item+"]");

					if (st3.hasMoreTokens()) {
						addItem(item, rubriek, nummer2record, prefix+"record", prefix+"recordnr", prefix+"recordindex" );
						prefix=prefix+"sub";
					} else {
						addItem(item, rubriek, nummer2rubriek, "naam", "nummer", "index" );
					}
				}

				rubriek.addAttribute("label", label);
				rubriek.addAttribute("waarde", waarde);
				bericht.addSubElement(rubriek);
			}
		}
		return bericht.asXmlString();
	}

	public void addItem(String item, XmlBuilder builder, Map nummer2naam, String naamLabel, String nummerLabel, String indexLabel) {

		String nummer;
		String naam = null;
		String index=null;

		if (item.indexOf('[')<0) {
			nummer=item;
		} else {
			StringTokenizer st1 = new StringTokenizer(item,"[]");
			nummer = st1.nextToken();
			if (st1.hasMoreTokens()) {
				index= st1.nextToken();
			}
		}
		if (nummer2naam!=null) {
			naam = (String)nummer2naam.get(item);
			if (naam==null) {
				naam="UNKNOWN";
			}
			builder.addAttribute(naamLabel,naam);
		}

		builder.addAttribute(nummerLabel,nummer);
		if (index!=null) {
			builder.addAttribute(indexLabel,index);
		}
	}


	/**
	 * sets URL to the pascal file with label-constants generated by the ADIOS-utility.
	 * @ff.default nnrscons.pas
	 */
	public void setAdiosDefinities(String newAdiosDefinities) {
		adiosDefinities = newAdiosDefinities;
	}
	public String getAdiosDefinities() {
		return adiosDefinities;
	}

	/** Transformation direction.
	 * @ff.default Adios2Xml */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}
	public Direction getDirection() {
		return direction;
	}

	/** Sets name of the rekenbox to be called */
	public void setRekenbox(String newRekenbox) {
		rekenbox = newRekenbox;
	}
	public String getRekenbox() {
		return rekenbox;
	}

	/** Name of the SessionKey to retrieve the rekenbox name from */
	public void setRekenboxSessionKey(String newRekenboxSessionKey) {
		rekenboxSessionKey = newRekenboxSessionKey;
	}
	public String getRekenboxSessionKey() {
		return rekenboxSessionKey;
	}


	/**
	 * Sets the name of the forward used when no conversion to XML was performed, because the input was already XML.
	 * @ff.default noconversion
	 */
	public void setNoConversionForwardName(String string) {
		noConversionForwardName = string;
	}
	public String getNoConversionForwardName() {
		return noConversionForwardName;
	}
}
