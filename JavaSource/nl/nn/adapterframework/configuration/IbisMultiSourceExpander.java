/*
 * Created on 11-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.substitution.VariableExpander;
import org.apache.log4j.Logger;

/**
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class IbisMultiSourceExpander implements VariableExpander {
    private final static Logger log = Logger.getLogger(IbisMultiSourceExpander.class);
    
    private List sources = new ArrayList();
    private boolean environmentFallback = false;
    
	/**
	 * 
	 */
	public IbisMultiSourceExpander() {
		super();
	}
    
    public IbisMultiSourceExpander(boolean environmentFallback) {
        this();
        this.environmentFallback = environmentFallback;
    }
    
    public void addSource(Map source) {
        sources.add(source);
    }
    
	/* (non-Javadoc)
	 * @see org.apache.commons.digester.substitution.VariableExpander#expand(java.lang.String)
	 */
	public String expand(String inp) {
        if (log.isDebugEnabled()) {
            log.debug("Requested to expand input-string [" + inp + "]");
        }
        int lastVarEnd = 0;
        int varMarkerPos = inp.indexOf("${");
        if (varMarkerPos == -1) {
            log.debug("No substitutions to be made");
            return inp;
        }
        StringBuffer result = new StringBuffer();
        while (varMarkerPos >= 0) {
            // append to result everything from end of last var
            // until now
            result.append(inp.substring(lastVarEnd, varMarkerPos));
            
            // find ending '}'
            lastVarEnd = inp.indexOf('}', varMarkerPos);
            
            // If ending '}' not found, then just append
            // rest of input and bail out
            if (lastVarEnd < 0) {
                result.append(inp.substring(varMarkerPos));
                break;
            }
            
            // Extract name of the variable
            String varName = inp.substring(varMarkerPos+2, lastVarEnd);
            
            // Get the value and append to result
            String value = getValue(varName);
            if (value == null) {
                result.append(inp.substring(varMarkerPos, lastVarEnd+1));
            } else {
                // Recursive expansion
                value = expand(value);
                result.append(value);
            }
            
            // Make last-pos point to just past end of last var
            ++lastVarEnd;
            
            // Find start of next variable
            varMarkerPos = inp.indexOf("${", lastVarEnd);
        }
        // Add remainder of input string to the output
        if (lastVarEnd < inp.length()) {
            result.append(inp.substring(lastVarEnd));
        }
        String resultString = result.toString();
        if (log.isDebugEnabled()) {
            log.debug("Input-string [" + inp + "] expanded to [" + resultString + "]");
        }
		return resultString;
	}

	/**
	 * @param varName
	 * @return
	 */
	public String getValue(String varName) {
        for (Iterator iter = sources.iterator(); iter.hasNext();) {
			Map source = (Map) iter.next();
			if (source.containsKey(varName)) {
                return String.valueOf(source.get(varName));
			}
		}
        // Variable not found in any of the sources;
        // get it from environment.
        if (environmentFallback) {
            return System.getenv(varName);
        } else {
            log.warn("No substitution can be found for variable name ["
                + varName + "]");
            return null;
        }
	}
	/**
	 * @return
	 */
	public boolean isEnvironmentFallback() {
		return environmentFallback;
	}

	/**
	 * @return
	 */
	public List getSources() {
		return sources;
	}

	/**
	 * @param b
	 */
	public void setEnvironmentFallback(boolean b) {
		environmentFallback = b;
	}

	/**
	 * @param list
	 */
	public void setSources(List list) {
		sources = list;
	}

}
