/*
 * $Log: SapFunctionFacade.java,v $
 * Revision 1.4  2012-04-23 14:41:09  m00f069
 * Added support for JCoStructure
 *
 * Revision 1.3  2012/03/28 16:41:51  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fixed sending TABLES in response to SAP too
 *
 * Revision 1.2  2012/03/27 15:08:36  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fixed sending TABLES with function call
 *
 * Revision 1.1  2012/02/06 14:33:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.20  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.18  2010/05/06 12:49:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * alternative way to set tables from XML
 *
 * Revision 1.17  2009/09/08 14:20:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unneccesary warning
 *
 * Revision 1.16  2008/01/30 14:43:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified javadoc
 *
 * Revision 1.15  2008/01/29 15:43:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for dynamic selection of sapsystem
 *
 * Revision 1.14  2007/10/08 12:17:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.13  2007/08/03 08:41:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid rare NPE
 *
 * Revision 1.12  2007/06/07 15:16:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now implements HasPhysicalDestination
 *
 * Revision 1.11  2007/05/02 11:33:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for handling parameters
 *
 * Revision 1.10  2007/02/12 13:47:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.9  2006/01/05 13:59:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2005/12/28 08:42:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected handling of input parameters
 *
 * Revision 1.7  2005/08/08 09:42:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked SAP classes to provide better refresh of repository when needed
 *
 * Revision 1.6  2005/03/14 17:26:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * bugfix: removed setting of return table parameters, as this caused the process to lock
 *
 * Revision 1.5  2004/10/05 10:40:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code and imports
 *
 * Revision 1.4  2004/08/20 12:29:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in findFieldIndex
 *
 * Revision 1.3  2004/07/19 09:45:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getLogPrefix()
 *
 * Revision 1.2  2004/07/15 07:44:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.3  2004/06/30 12:36:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved exception reporting
 *
 * Revision 1.2  2004/06/22 12:14:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made Logger protected instead of private
 *
 * Revision 1.1  2004/06/22 06:56:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3;

import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;

import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
/**
 * Wrapper round SAP-functions, either SAP calling Ibis, or Ibis calling SAP.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Ibis-object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the {@link SapSystem} used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldIndex(int) correlationIdFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>0</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldName(String) correlationIdFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestFieldIndex(int) requestFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>0</td></tr>
 * <tr><td>{@link #setRequestFieldName(String) requestFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyFieldIndex(int) replyFieldIndex}</td><td>Index of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>0</td></tr>
 * <tr><td>{@link #setReplyFieldName(String) replyFieldName}</td><td>Name of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>&nbsp;</td></tr>
 * </table>
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted from/to xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted from/to xml. 
 * </p>
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class SapFunctionFacade implements INamedObject, HasPhysicalDestination {
	public static final String version="$RCSfile: SapFunctionFacade.java,v $  $Revision: 1.4 $ $Date: 2012-04-23 14:41:09 $";
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String sapSystemName;
	
	private int correlationIdFieldIndex=0;
	private String correlationIdFieldName;
	private int requestFieldIndex=0;
	private String requestFieldName;
	private int replyFieldIndex=0;
	private String replyFieldName;

	private JCoFunctionTemplate ftemplate;
	private SapSystem sapSystem;
	private boolean fieldIndicesCalculated=false;

	static Map extractors = new HashMap();

	protected String getLogPrefix() {
		return this.getClass().getName()+" ["+getName()+"] ";
	}

	public void configure() throws ConfigurationException {
//		if (StringUtils.isEmpty(getSapSystemName())) {
//			throw new ConfigurationException("attribute sapSystemName must be specified");
//		}
		if (StringUtils.isNotEmpty(getSapSystemName())) {
			sapSystem=SapSystem.getSystem(getSapSystemName());
			if (sapSystem==null) {
				throw new ConfigurationException(getLogPrefix()+"cannot find SapSystem ["+getSapSystemName()+"]");
			}
 		} else {
 			SapSystem.configureAll();
 		}
	}

	public void openFacade() throws SapException {
		if (sapSystem!=null) {
			sapSystem.openSystem();
			if (!StringUtils.isEmpty(getFunctionName())) {
				ftemplate = getFunctionTemplate(sapSystem, getFunctionName());
				try {
					calculateStaticFieldIndices(ftemplate);
					fieldIndicesCalculated=true;
				} catch (Exception e) {
					throw new SapException(getLogPrefix()+"Exception calculation field-indices ["+getFunctionName()+"]", e);
				}
			}
		} else {
			SapSystem.openSystems();
		}
	}
	
	public void closeFacade() {
		if (sapSystem!=null) {
			sapSystem.closeSystem();
		} else {
			SapSystem.closeSystems();
		}
		fieldIndicesCalculated=false;
		ftemplate = null;
	}


	public String getPhysicalDestinationName() {
		String result;
		if (sapSystem==null) {
			return "dynamical determined"; // to avoid NPE
		}
		result = "mandant ["+sapSystem.getMandant()+"] on gwhost ["+sapSystem.getGwhost()+"] system ["+sapSystem.getSystemnr()+"]";
		return result;
	}



	static protected void setParameters(JCoParameterList params, String message, int fieldIndex) throws SapException {
		if (params != null && StringUtils.isNotEmpty(message)) {
			if (fieldIndex>0) {
				params.setValue(message,fieldIndex-1);
			} else {
				JCoMetaData metaData = params.getMetaData();
				for (int i = 0; i < metaData.getFieldCount(); i++) {
					String xpath;
					String outputMethod;
					String paramName=params.getMetaData().getName(i);
					if (params.getMetaData().getType(i) == JCoMetaData.TYPE_TABLE) {
						xpath = "/*/TABLES/"+paramName;
						outputMethod = "xml";
					} else if (params.getMetaData().getType(i) == JCoMetaData.TYPE_STRUCTURE) {
						xpath = "/*/INPUT/"+paramName;
						outputMethod = "xml";
					} else {
						xpath = "/*/INPUT/"+paramName;
						outputMethod = "text";
					}
					TransformerPool tp = (TransformerPool)extractors.get(outputMethod+":"+xpath);
					if (tp==null) {
						try {
							tp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(xpath,outputMethod));
							extractors.put(outputMethod+":"+xpath,tp);
						} catch (Exception e) {
							throw new SapException("exception creating Extractor for xpath ["+xpath+"]", e);
						}
					}
					String xpathResult;
					try {
						xpathResult = tp.transform(message,null);
					} catch (Exception e) {
						throw new SapException("exception extracting ["+paramName+"]", e);
					}
					if (StringUtils.isNotEmpty(xpathResult)) {
						if (params.getMetaData().getType(i) == JCoMetaData.TYPE_TABLE) {
							JCoTable jcoTable = params.getTable(i);
							try {
								ContentHandler ch = new TableHandler(jcoTable);
								XmlUtils.parseXml(ch,xpathResult);
							} catch (Exception e) {
								System.out.println("exception: " + e);
								throw new SapException("exception parsing table ["+paramName+"]", e);
							}
						} else if (params.getMetaData().getType(i) == JCoMetaData.TYPE_STRUCTURE) {
							JCoStructure jcoStructure = params.getStructure(i);
							try {
								ContentHandler ch = new StructureHandler(jcoStructure);
								XmlUtils.parseXml(ch,xpathResult);
							} catch (Exception e) {
								System.out.println("exception: " + e);
								throw new SapException("exception parsing table ["+paramName+"]", e);
							}
						} else {
							params.setValue(i, xpathResult);
						}
					}
				}
			}
		}
	}

//	static protected void setTables(JCoParameterList tableParams, String message) throws SapException {
//		if (tableParams != null && StringUtils.isNotEmpty(message)) {
//			String paramsName=tableParams.getMetaData().getName();
//			TransformerPool tp = (TransformerPool)extractors.get(paramsName);
//			if (tp==null) {
//				try {
////					log.debug("creating evaluator for parameter ["+paramName+"]");
//					tp = new TransformerPool(XmlUtils.createXPathEvaluatorSource("/*/"+paramsName,"xml"));
//					extractors.put(paramsName,tp);
//				} catch (Exception e) {
//					throw new SapException("exception creating Extractor for  ["+paramsName+"]", e);
//				}
//			}
//			try {
//				String paramsXml = tp.transform(message,null);
//				TableDigester td = new TableDigester();
//				td.digestTableXml(tableParams,paramsXml);
//			} catch (Exception e) {
//				throw new SapException("exception extracting ["+paramsName+"]", e);
//			}
//		}
//	}


	/**
	 * This method must be called from configure().
	 * @param ft
	 */
	protected void calculateStaticFieldIndices(JCoFunctionTemplate ft) {
		if (getRequestFieldIndex()== 0) {	
			if (StringUtils.isEmpty(getRequestFieldName())) {
				setRequestFieldIndex(-1);
			} else {
				if (ft!=null) {
					setRequestFieldIndex(1+ft.getImportParameterList().indexOf(getRequestFieldName()));
				}				
			}
		}
		if (getReplyFieldIndex()== 0) {	
			if (StringUtils.isEmpty(getReplyFieldName())) {
				setReplyFieldIndex(-1);
			} else {
				if (ft!=null) {
					setReplyFieldIndex(1+ft.getExportParameterList().indexOf(getReplyFieldName()));
				}				
			}
		}
		if (getCorrelationIdFieldIndex()== 0) {	
			if (StringUtils.isEmpty(getCorrelationIdFieldName())) {
				setCorrelationIdFieldIndex(-1);
			} else {
				if (ft!=null) {
					setCorrelationIdFieldIndex(1+ft.getImportParameterList().indexOf(getCorrelationIdFieldName()));
				}				
			}
		}
	}
			

	/**
	 * Calculate the index of the field that correspondes with the message as a whole.
	 * 
	 * return values
	 *  >0 : the required index
	 *  0  : no index found, convert all fields to/from xml.
	 */
	protected int findFieldIndex(JCoParameterList params, int index, String name) {
		if (index!=0 || StringUtils.isEmpty(name)) {
			return index;
		}
		try {
			return (1+params.getListMetaData().indexOf(name));
		} catch (Exception e) {
			log.warn("["+getName()+"] exception finding FieldIndex for name ["+name+"]", e);
			return 0;
		}
	}

	public String getCorrelationIdFromField(JCoFunction function) {
		JCoParameterList input = function.getImportParameterList();
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
				return input.getString(correlationIdFieldIndex-1);
			}
		return null;
	}


	public String functionCall2message(JCoFunction function) {
		JCoParameterList input = function.getImportParameterList();
		
		int messageFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		String result=null;
		if (messageFieldIndex>0) {
			if (input!=null) {
				result = input.getString(messageFieldIndex-1);
			}
		} else {
			result = "<request function=\""+function.getName()+"\">";
	
			JCoParameterList tables = function.getTableParameterList();
			
			if (input!=null) {
				result+=input.toXML();
			}
			if (tables!=null) {
				result+=tables.toXML();
			}
			result+="</request>";
		}

		return result;
	}

	public String functionResult2message(JCoFunction function) {
		JCoParameterList export = function.getExportParameterList();
		
		int replyFieldIndex = findFieldIndex(export, getReplyFieldIndex(), getReplyFieldName());
		String result=null;
		if (replyFieldIndex>0) {
			if (export!=null) {
				result = export.getString(replyFieldIndex-1);
			}
		} else {
			result = "<response function=\""+function.getName()+"\">";

			JCoParameterList tables = function.getTableParameterList();
		
			if (export!=null) {
				result+=export.toXML();
			}
			if (tables!=null) {
				result+=tables.toXML();
			}
			result+="</response>";
		}
		return result;
	}

	public void message2FunctionCall(JCoFunction function, String request, String correlationId, ParameterValueList pvl) throws SapException {
		JCoParameterList input = function.getImportParameterList();
		int requestFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		setParameters(input, request, requestFieldIndex);
		if (requestFieldIndex<=0) {
			setParameters(function.getTableParameterList(),  request, 0);
		}
		if (pvl!=null) {
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
				String name = pv.getDefinition().getName();
				String value = pv.asStringValue("");
				int slashPos=name.indexOf('/');
				if (slashPos<0) {
					input.setValue(value,name);
				} else {
					String structName=name.substring(0,slashPos);
					String elemName=name.substring(slashPos+1);
					JCoStructure struct=input.getStructure(structName);
					struct.setValue(value,elemName);
				}
			}
		}
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
			input.setValue(correlationId, correlationIdFieldIndex-1);
		}
	}

	public void message2FunctionResult(JCoFunction function, String result) throws SapException {
		JCoParameterList output = function.getExportParameterList();
		int replyFieldIndex = findFieldIndex(output, getReplyFieldIndex(), getReplyFieldName());
		setParameters(function.getExportParameterList(),result, replyFieldIndex);
		//log.warn("SapFunctionFacade.message2FunctionResult, skipped setting of return table parameters");
		if (replyFieldIndex<=0) {
			log.debug("SapFunctionFacade.message2FunctionResult, setting table parameters");
//			setTables(function.getTableParameterList(), result);
			setParameters(function.getTableParameterList(),result, replyFieldIndex);
		}
	}
	

	public SapSystem getSapSystem() throws SapException {
		if(sapSystem==null) {
			throw new SapException("no fixed sapSystem specified");
		}
		return sapSystem;
	}
	public SapSystem getSapSystem(String systemName) throws SapException {
		SapSystem sapSystem = SapSystem.getSystem(systemName);
		if(sapSystem==null) {
			throw new SapException("cannot find sapSystem ["+systemName+"]");
		}
		return sapSystem;
	}


	protected JCoFunctionTemplate getFunctionTemplate() throws SapException {
		if(ftemplate==null) {
			throw new SapException("no fixed functionName specified");
		}
		return ftemplate;
	}
	protected JCoFunctionTemplate getFunctionTemplate(SapSystem sapSystem, String functionName) throws SapException {
		JCoFunctionTemplate functionTemplate;
		try {
			functionTemplate = sapSystem.getJcoRepository().getFunctionTemplate(functionName);
		} catch (Exception e) {
			throw new SapException(getLogPrefix()+"exception obtaining template for function ["+functionName+"] from sapSystem ["+sapSystem.getName()+"]", e);
		}
		if (functionTemplate == null) {
			throw new SapException(getLogPrefix()+"could not obtain template for function ["+functionName+"] from sapSystem ["+sapSystem.getName()+"]");
		}
		return functionTemplate;
	}


	public int getCorrelationIdFieldIndex() {
		return correlationIdFieldIndex;
	}

	public String getCorrelationIdFieldName() {
		return correlationIdFieldName;
	}

	public int getReplyFieldIndex() {
		return replyFieldIndex;
	}

	public String getReplyFieldName() {
		return replyFieldName;
	}

	public int getRequestFieldIndex() {
		return requestFieldIndex;
	}

	public String getRequestFieldName() {
		return requestFieldName;
	}

	public void setCorrelationIdFieldIndex(int i) {
		correlationIdFieldIndex = i;
	}

	public void setCorrelationIdFieldName(String string) {
		correlationIdFieldName = string;
	}

	public void setReplyFieldIndex(int i) {
		replyFieldIndex = i;
	}

	public void setReplyFieldName(String string) {
		replyFieldName = string;
	}

	public void setRequestFieldIndex(int i) {
		requestFieldIndex = i;
	}

	public void setRequestFieldName(String string) {
		requestFieldName = string;
	}

	public String getName() {
		return name;
	}

	public void setName(String string) {
		name = string;
	}


	public String getSapSystemName() {
		return sapSystemName;
	}


	public void setSapSystemName(String string) {
		sapSystemName = string;
	}

	protected String getFunctionName() {
		return null;
	}


}
