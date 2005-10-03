/*
 * $Log: DB2XMLWriter.java,v $
 * Revision 1.9  2005-10-03 13:17:40  europe\L190409
 * added attribute trimSpaces, default=true
 *
 * Revision 1.8  2005/09/29 13:57:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made null an attribute of each field, where applicable
 *
 * Revision 1.7  2005/07/28 07:42:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return a value for CLOBs too;
 * catch numberFormatException for precision
 *
 * Revision 1.6  2005/06/13 11:52:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.util;

import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Transforms a java.sql.Resultset to a XML stream.
 * Example of a result:
 * <code><pre>
 * &lt;result&gt;
	&lt;fielddefinition&gt;
		&lt;field name="fieldname"
		          type="columnType" 
		          columnDisplaySize=""
		          precision=""
		          scale=""
		          isCurrency=""
		          columnTypeName=""
		          columnClassName=""/&gt;
		 &lt;field ...../&gt;
	&lt;/fielddefinition&gt;
	&lt;rowset&gt;
		&lt;row number="1"&gt;
			&lt;field name="fieldname"&gt;value&lt;/field&gt;
			&lt;field name="fieldname" null="true" &gt;&lt;/field&gt;
			&lt;field name="fieldname"&gt;value&lt;/field&gt;
			&lt;field name="fieldname"&gt;value&lt;/field&gt;
		&lt;/row&gt;
	&lt;/rowset&gt;
&lt;/result&gt;
</pre></code>
 *
 * @author Johan Verrips
 * @version Id
 **/

public class DB2XMLWriter {
	public static final String version="$RCSfile: DB2XMLWriter.java,v $ $Revision: 1.9 $ $Date: 2005-10-03 13:17:40 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String docname = new String("result");
	private String recordname = new String("rowset");
	private String nullValue = "";
	private boolean trimSpaces=true;

   
    public static String getFieldType (int type) {
	    switch (type) {
	          case Types.INTEGER : return ("INTEGER");
	          case Types.NUMERIC : return ("NUMERIC");
	          case Types.CHAR :    return ("CHAR");
	          case Types.DATE :    return ("DATE");
	          case Types.TIMESTAMP : return ("TIMESTAMP");
	          case Types.DOUBLE : return ("DOUBLE");
	          case Types.FLOAT : return ("FLOAT");
	          case Types.ARRAY : return ("ARRAY");
	          case Types.BLOB : return ("BLOB");
	          case Types.CLOB : return ("CLOB");
	          case Types.DISTINCT : return ("DISTINCT");
	          case Types.LONGVARBINARY : return ("LONGVARBINARY");
	          case Types.VARBINARY : return ("VARBINARY");
	          case Types.BINARY : return ("BINARY");
	          case Types.REF : return ("REF");
	          case Types.STRUCT : return ("STRUCT");
	          case Types.JAVA_OBJECT  : return ("JAVA_OBJECT");
	          case Types.VARCHAR  : return ("VARCHAR");
	          case Types.TINYINT: return ("TINYINT");
	          case Types.TIME: return ("TIME");
	          case Types.REAL: return ("REAL");
		}
     	return ("Unknown");
    }
    
    /**
     * This method gets the value of the specified column
     */
    private String getValue(final ResultSet rs, int colNum, int type) throws SQLException
    {
        switch(type)
        {
        	// return "undefined" for types that cannot be rendered to strings easily
            case Types.ARRAY :
            case Types.BLOB :
            case Types.DISTINCT :
            case Types.LONGVARBINARY :
            case Types.VARBINARY :
            case Types.BINARY :
            case Types.REF :
            case Types.STRUCT :
                return "undefined";
            default :
            {
                String value = rs.getString(colNum);
                if(value == null)
                    return getNullValue();
                else
                	if (isTrimSpaces()) {
						value.trim();
                	}
					return value;

            }
        }
    }

   /**
    * Retrieve the Resultset as a well-formed XML string
    */
	public synchronized String getXML(ResultSet rs) {
		return getXML(rs, Integer.MAX_VALUE);
	}

	/**
	 * Retrieve the Resultset as a well-formed XML string
	 */
	public synchronized String getXML(ResultSet rs, int maxlength) {
		if (null == rs)
			return "";
	
		if (maxlength < 0)
			maxlength = Integer.MAX_VALUE;
	
		XmlBuilder mainElement = new XmlBuilder(docname);
		int rowCounter=0;
		try {
			ResultSetMetaData rsmeta = rs.getMetaData();
			int nfields = rsmeta.getColumnCount();
	
			XmlBuilder fields = new XmlBuilder("fielddefinition");
			for (int j = 1; j <= nfields; j++) {
				XmlBuilder field = new XmlBuilder("field");
	
				field.addAttribute("name", "" + rsmeta.getColumnName(j));
	
				//Not every JDBC implementation implements these attributes!
				try {
					field.addAttribute("type", "" + getFieldType(rsmeta.getColumnType(j)));
				} catch (SQLException e) {
					log.debug(e);
				}
				try {
					field.addAttribute("columnDisplaySize", "" + rsmeta.getColumnDisplaySize(j));
				} catch (SQLException e) {
					log.debug(e);
				}
				try {
					field.addAttribute("precision", "" + rsmeta.getPrecision(j));
				} catch (SQLException e) {
					log.warn(e);
				} catch (NumberFormatException e2) {
					log.debug(e2);
				}
				try {
					field.addAttribute("scale", "" + rsmeta.getScale(j));
				} catch (SQLException e) {
					log.debug(e);
				}
				try {
					field.addAttribute("isCurrency", "" + rsmeta.isCurrency(j));
				} catch (SQLException e) {
					log.debug(e);
				}
				try {
					field.addAttribute("columnTypeName", "" + rsmeta.getColumnTypeName(j));
				} catch (SQLException e) {
					log.debug(e);
				}
				try {
					field.addAttribute("columnClassName", "" + rsmeta.getColumnClassName(j));
				} catch (SQLException e) {
					log.debug(e);
				}
				fields.addSubElement(field);
			}
			mainElement.addSubElement(fields);
		
			//----------------------------------------
			// Process result rows
			//----------------------------------------
	
			XmlBuilder queryresult = new XmlBuilder(recordname);
			while (rs.next() & rowCounter < maxlength) {
				XmlBuilder row = new XmlBuilder("row");
				row.addAttribute("number", "" + rowCounter);
	
				for (int i = 1; i <= nfields; i++) {
					XmlBuilder resultField = new XmlBuilder("field");
	
					resultField.addAttribute("name", "" + rsmeta.getColumnName(i));
	
					try {
						String value = getValue(rs, i, rsmeta.getColumnType(i));
						if (rs.wasNull()) {
							resultField.addAttribute("null","true");
						}
						resultField.setValue(value);
	
					} catch (Exception e) {
						log.error("error getting fieldvalue column ["+i+"] fieldType ["+getFieldType(rsmeta.getColumnType(i))+ "]", e);
					}
					row.addSubElement(resultField);
				}
				queryresult.addSubElement(row);
				rowCounter++;
			}
			mainElement.addSubElement(queryresult);
		} catch (Exception e) {
			log.error("Error occured at row: " + rowCounter, e);
		}
		String answer = mainElement.toXML();
		return answer;
	}


   public void setDocumentName(String s) {
     docname = s;
   }
   public void setRecordName(String s) {
     recordname = s;
   }
   
   /**
	* Set the presentation of a <code>Null</code> value
	**/
   public void setNullValue(String s) {
	 nullValue=s;
   }
   /**
	* Get the presentation of a <code>Null</code> value
	**/
   public String getNullValue () {
	 return nullValue;
   }

	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}
	public boolean isTrimSpaces() {
		return trimSpaces;
	}


}
