/*
 * $Log: SapFunctionFacade.java,v $
 * Revision 1.1  2004-06-22 06:56:44  L190409
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.sap;

import java.util.HashMap;

import javax.xml.transform.Transformer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import com.sap.mw.jco.*;
/**
 * Wrapper round SAP-functions, either SAP calling Ibis, or Ibis calling SAP.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Ibis-object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) sapSystemName}</td><td>name of the SapSystem used by this object</td><td>&nbsp;</td></tr>
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
 * @author Gerrit van Brakel
 * @since 4.1.1
 */
public class SapFunctionFacade {
	public static final String version="$Id: SapFunctionFacade.java,v 1.1 2004-06-22 06:56:44 L190409 Exp $";
	private Logger log = Logger.getLogger(this.getClass());

	private String name;
	private String sapSystemName;
	
	private int correlationIdFieldIndex=0;
	private String correlationIdFieldName;
	private int requestFieldIndex=0;
	private String requestFieldName;
	private int replyFieldIndex=0;
	private String replyFieldName;

	private IFunctionTemplate ftemplate;
	private SapSystem sapSystem;

	static HashMap extractors = new HashMap();

	public void configure() throws ConfigurationException {
		sapSystem=SapSystem.getSystem(getSapSystemName());
		if (sapSystem==null) {
			throw new ConfigurationException("["+this.getClass().getName()+"] ["+getName()+"] cannot find SapSystem ["+getSapSystemName()+"]");
		}
	}

	public void openFacade() throws SapException {
		if (!StringUtils.isEmpty(getFunctionName())) {
			try {
				ftemplate = sapSystem.getRepository().getFunctionTemplate(getFunctionName());
			} catch (Exception e) {
				throw new SapException("exception obtaining template for function ["+getFunctionName()+"]");
			}
			if (ftemplate == null) {
				throw new SapException("could not obtain template for function ["+getFunctionName()+"]");
			}
			try {
				calculateStaticFieldIndices(ftemplate);
			} catch (Exception e) {
				throw new SapException("Exception calculation field-indices ["+getFunctionName()+"]");
			}
		}
	}
	
	public void closeFacade() {
		ftemplate = null;
	}




	static void setParameters(JCO.ParameterList params, String message, int fieldIndex) throws SapException {
		if (params !=null) {
			if (fieldIndex>0) {
				params.setValue(message,fieldIndex-1);
			} else {
				Transformer t = (Transformer)extractors.get(params.getName());
				if (t==null) {
					try {
						t = XmlUtils.createXPathEvaluator("/*/"+params.getName(),"xml");
						extractors.put(params.getName(),t);
					} catch (Exception e) {
						throw new SapException("exception creating Extractor for  ["+params.getName()+"]", e);
					}
				}
				try {
					String paramsXml = XmlUtils.transformXml(t, message);
					if (StringUtils.isEmpty(paramsXml)) {
						//log.debug("parameters ["+params.getName()+"] Xml ["+paramsXml+"]");
						params.fromXML(paramsXml);
					}
				} catch (Exception e) {
					throw new SapException("exception extracting ["+params.getName()+"]", e);
				}
			}
		}
	}

	/**
	 * This method must be called from configure().
	 * @param ft
	 */
	protected void calculateStaticFieldIndices(IFunctionTemplate ft) {
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
	protected int findFieldIndex(JCO.ParameterList params, int index, String name) {
		if (index!=0) {
			return index;
		}
		return (1+params.indexOf(name));
	}

	public String getCorrelationIdFromField(JCO.Function function) {
		JCO.ParameterList input = function.getImportParameterList();
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
				return input.getString(correlationIdFieldIndex-1);
			}
		return null;
	}


	public String functionCall2message(JCO.Function function) {
		JCO.ParameterList input = function.getImportParameterList();
		
		int messageFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		String result=null;
		if (messageFieldIndex>0) {
			if (input!=null) {
				result = input.getString(messageFieldIndex-1);
			}
		} else {
			String inputXml = "";
			String tableXml = "";
			result = "<request function=\""+function.getName()+"\">";
	
			JCO.ParameterList tables = function.getTableParameterList();
			
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

	public String functionResult2message(JCO.Function function) {
		JCO.ParameterList export = function.getExportParameterList();
		
		int replyFieldIndex = findFieldIndex(export, getReplyFieldIndex(), getReplyFieldName());
		String result=null;
		if (replyFieldIndex>0) {
			if (export!=null) {
				result = export.getString(replyFieldIndex-1);
			}
		} else {
			String inputXml = "";
			String tableXml = "";
			result = "<response function=\""+function.getName()+"\">";

			JCO.ParameterList tables = function.getTableParameterList();
		
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

	public void message2FunctionCall(JCO.Function function, String request, String correlationId) throws SapException {
		JCO.ParameterList input = function.getImportParameterList();
		int requestFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		setParameters(input, request, requestFieldIndex);
		if (requestFieldIndex<=0) {
			setParameters(function.getTableParameterList(),  request, 0);
		}
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
			input.setValue(correlationId, correlationIdFieldIndex-1);
		}
	}

	public void message2FunctionResult(JCO.Function function, String result) throws SapException {
		JCO.ParameterList output = function.getExportParameterList();
		int replyFieldIndex = findFieldIndex(output, getReplyFieldIndex(), getReplyFieldName());
		setParameters(function.getExportParameterList(),result, replyFieldIndex);
		if (replyFieldIndex<=0) {
			setParameters(function.getTableParameterList(), result, 0);
		}
	}
	

	public SapSystem getSapSystem() {
		return sapSystem;
	}


	protected IFunctionTemplate getFunctionTemplate() {
		return ftemplate;
	}


	/**
	 * @return
	 */
	public int getCorrelationIdFieldIndex() {
		return correlationIdFieldIndex;
	}

	/**
	 * @return
	 */
	public String getCorrelationIdFieldName() {
		return correlationIdFieldName;
	}

	/**
	 * @return
	 */
	public int getReplyFieldIndex() {
		return replyFieldIndex;
	}

	/**
	 * @return
	 */
	public String getReplyFieldName() {
		return replyFieldName;
	}

	/**
	 * @return
	 */
	public int getRequestFieldIndex() {
		return requestFieldIndex;
	}

	/**
	 * @return
	 */
	public String getRequestFieldName() {
		return requestFieldName;
	}

	/**
	 * @param i
	 */
	public void setCorrelationIdFieldIndex(int i) {
		correlationIdFieldIndex = i;
	}

	/**
	 * @param string
	 */
	public void setCorrelationIdFieldName(String string) {
		correlationIdFieldName = string;
	}

	/**
	 * @param i
	 */
	public void setReplyFieldIndex(int i) {
		replyFieldIndex = i;
	}

	/**
	 * @param string
	 */
	public void setReplyFieldName(String string) {
		replyFieldName = string;
	}

	/**
	 * @param i
	 */
	public void setRequestFieldIndex(int i) {
		requestFieldIndex = i;
	}

	/**
	 * @param string
	 */
	public void setRequestFieldName(String string) {
		requestFieldName = string;
	}
	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}


	/**
	 * @return
	 */
	public String getSapSystemName() {
		return sapSystemName;
	}


	/**
	 * @param string
	 */
	public void setSapSystemName(String string) {
		sapSystemName = string;
	}

	/**
	 * @return
	 */
	protected String getFunctionName() {
		return null;
	}

}
