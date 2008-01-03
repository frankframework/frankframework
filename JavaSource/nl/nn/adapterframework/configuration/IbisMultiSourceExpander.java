/*
 * $Log: IbisMultiSourceExpander.java,v $
 * Revision 1.4  2008-01-03 15:39:20  europe\L190409
 * remove superfluous logging
 *
 * Revision 1.3  2007/10/16 09:12:27  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.1.2.4  2007/10/15 11:35:52  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix direct retrieving of Logger w/o using the LogUtil
 *
 * Revision 1.1.2.3  2007/10/10 14:30:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.digester.substitution.VariableExpander;
import org.apache.log4j.Logger;

/**
 * 
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class IbisMultiSourceExpander implements VariableExpander {
    private final static Logger log = LogUtil.getLogger(IbisMultiSourceExpander.class);
    
    private List sources = new ArrayList();
    private boolean environmentFallback = false;
    
    private boolean trace=false;
    
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
        if (trace && log.isDebugEnabled()) {
            log.debug("Requested to expand input-string [" + inp + "]");
        }
        int lastVarEnd = 0;
        int varMarkerPos = inp.indexOf("${");
        if (varMarkerPos == -1) {
			if (trace && log.isDebugEnabled()) { log.debug("No substitutions to be made"); } 
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
        if (trace && log.isDebugEnabled()) {
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
