/*
 * $Log: QueryParameterParser.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML-parser that sets parameters to a statement
 *
 * Sample xml:<br/><code><pre>
 *	&lt;parameters&gt;
 *	    &lt;parameter type="string" value="woensdag" /&gt;
 *	    &lt;parameter type="int" value="14" /&gt;
 *	&lt;/parameters&gt;
 * </pre></code> <br/>
 * 
 * <p>$Id: QueryParameterParser.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class QueryParameterParser extends DefaultHandler implements IQueryParameterParser {
	protected Logger log = Logger.getLogger(this.getClass());

	protected PreparedStatement stmt=null;
	protected int parameterNumber;
	
	public void startElement(
		String namespaceUri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException {
			if ("parameter".equals(qName)) {
				String parType=attributes.getValue("type");
				String parValue=attributes.getValue("value");
				
				log.debug("setting parameter ["+parameterNumber+"] of type ["+parType+"] to ["+parValue+"]");
				try {
					if (parType==null || parType.equalsIgnoreCase("string")) {
						stmt.setString(++parameterNumber,parValue);
					} else {
						if (parType.equalsIgnoreCase("int")) {
							stmt.setInt(++parameterNumber,Integer.parseInt(parValue));
						} else {
							log.warn("no specific parameter setter for type ["+parType+"], trying 'string'");
							stmt.setString(++parameterNumber,parValue);
						}
					}
				} catch (SQLException e) {
						throw new SAXException("problem setting parameter ["+parameterNumber+"] of type ["+parType+"] to ["+parValue+"]");
				}
			}
	}

public void parse(PreparedStatement stmt, String correlationID, String message) throws JdbcException{
    SAXParserFactory factory = SAXParserFactory.newInstance();
	factory.setNamespaceAware(true);
	factory.setValidating(true);
	this.stmt=stmt;
	parameterNumber=0;

	SAXParser saxParser;
	try {
		saxParser = factory.newSAXParser();
		saxParser.parse(message, this);
	} catch (Exception e) {
		throw new JdbcException("exception parsing query parameters from msg, ID["+correlationID+"]",e);
	}
}
	
}
