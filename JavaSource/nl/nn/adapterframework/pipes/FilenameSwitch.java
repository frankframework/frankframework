/*
 * $Log: FilenameSwitch.java,v $
 * Revision 1.1  2008-02-15 14:09:04  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;


/**
 * Selects an exitState, based on the last (filename) part of the path that is the input.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlSwitch</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNotFoundForwardName(String) notFoundForwardName}</td><td>Forward returned when the pipename derived from the stylesheet could not be found.</i></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setToLowercase(boolean) toLowercase}</td><td>convert the result to lowercase</td><td>true</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>&lt;filenname part of the path&gt;</td><td>default</td></tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FilenameSwitch extends AbstractPipe {
	
    private String notFoundForwardName=null;
    private boolean toLowercase=true;

	public void configure() throws ConfigurationException {
		super.configure();
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				log.warn(getLogPrefix(null)+"has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
			}
		}
	}
	
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String forward="";
	    String sInput=(String) input;
	    PipeForward pipeForward=null;

		int slashPos=sInput.lastIndexOf('/');
		if (slashPos>0) {
			sInput=sInput.substring(slashPos+1);
		}
		slashPos=sInput.lastIndexOf('\\');
		if (slashPos>0) {
			sInput=sInput.substring(slashPos+1);
		}
		forward=sInput;
		if (isToLowercase()) {
			forward=forward.toLowerCase();
		}
		log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

		if (findForward(forward) != null) 
			pipeForward=findForward(forward);
		else {
			log.info(getLogPrefix(session)+"determined forward ["+forward+"], which is not defined. Will use ["+getNotFoundForwardName()+"] instead");
			pipeForward=findForward(getNotFoundForwardName());
		}
		
		if (pipeForward==null) {
			  throw new PipeRunException (this, getLogPrefix(session)+"cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, input);
	}
	
	
	public void setNotFoundForwardName(String notFound){
		notFoundForwardName=notFound;
	}
	public String getNotFoundForwardName(){
		return notFoundForwardName;
	}
	
	public void setToLowercase(boolean b) {
		toLowercase = b;
	}
	public boolean isToLowercase() {
		return toLowercase;
	}

}
