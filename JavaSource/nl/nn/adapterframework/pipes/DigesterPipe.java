package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.*;
import java.io.*;
import java.net.URL;

/**
 * Converts an XML string (input) to a set of java object using the
 * <a href="http://jakarta.apache.org/commons/digester">digester</a>.
 * <p>The result is an anonymous object. Your digester-rules file should specify
 * how the xml file is parsed, and what the root object will be.</p>
  * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setDigesterRulesFile(String) setDigesterRulesFile}</td><td>name of file that containts the rules for xml parsing</td><td>(none)</td></tr>
 * </table>
 
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * </table></p>
 * <p>$Id: DigesterPipe.java,v 1.2 2004-02-09 11:06:52 a1909356#db2admin Exp $</p>
 * @author Richard Punt
 * @since 4.0.1 : adjustments to support multi-threading
 */

public class DigesterPipe extends FixedForwardPipe {

	private PipeRunResult pipeRunResult;
	private String digesterRulesFile;
	private URL rulesURL;

	public DigesterPipe() {
		super();
	}

	public void configure() throws ConfigurationException {
		super.configure();

		try {
     		 rulesURL = ClassUtils.getResourceURL(this, digesterRulesFile);
 			Digester digester = DigesterLoader.createDigester(rulesURL);
		} catch (Exception e) {
			throw new ConfigurationException(
				"Pipe ["
					+ super.getName()
					+ "] Digester rules file not found: "
					+ digesterRulesFile);
		}
		log.debug("End configuration of pipe [" + super.getName() + "]");
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		//Multi threading: instantiate digester for each request as the digester is NOT thread-safe.		
		Digester digester = DigesterLoader.createDigester(rulesURL);
			
		PipeRunResult pipeRunResult = new PipeRunResult();
		pipeRunResult.setPipeForward(findForward("success"));

		try {
			ByteArrayInputStream xmlInputStream =
				new ByteArrayInputStream(input.toString().getBytes());

			return new PipeRunResult(
				findForward("success"),
				digester.parse(xmlInputStream));
				
		} catch (Exception e) {
			log.error(
				new PipeRunException(
					this,
					"Pipe [" + super.getName() + "]: " + e.getMessage()),
				e);
			throw new PipeRunException(
				this,
				"Pipe [" + super.getName() + "]: " + e.getMessage(),
				e);
		}
	}

	public String getDigesterRulesFile() {
		return digesterRulesFile;
	}

	public void setDigesterRulesFile(String rhs) {
		digesterRulesFile = rhs;
	}

}
