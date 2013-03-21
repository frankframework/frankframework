/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: CoolGenWrapperPipe.java,v $
 * Revision 1.8  2012-06-01 10:52:59  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.7  2011/11/30 13:52:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2005/05/31 09:13:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added catch for DomBuilderException
 *
 * Revision 1.4  2004/03/31 12:04:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2004/03/24 15:26:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.extensions.coolgen;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.coolgen.proxy.CoolGenXMLProxy;
import nl.nn.coolgen.proxy.XmlProxyException;

/**
 * Perform the call to a CoolGen proxy with pre- and post transformations.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setProxyClassName(String) proxyClassName}</td><td>classname of proxy-class to be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClientId(String) clientId}</td><td>CICS userId of account perform operation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClientPassword(String) clientPassword}</td><td>password corresponding with userId</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreProcStylesheetName(String) preProcStylesheetName}</td><td>optional URL of XSLT-stylesheet to apply to message before calling proxy</td><td>no transformation</td></tr>
 * <tr><td>{@link #setPostProcStylesheetName(String) postProcStylesheetName}</td><td>optional URL of XSLT-stylesheet to apply to result of proxy </td><td>no transformation</td></tr>
 * <tr><td>{@link #setProxyInputSchema(String) proxyInputSchema}</td><td>optional URL of XML-Schema of proxy input message. If specified it is used to validate the input message</td><td>no validation</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 * @version $Id$
 */
public class CoolGenWrapperPipe extends FixedForwardPipe {
	public static final String version="$RCSfile: CoolGenWrapperPipe.java,v $ $Revision: 1.8 $ $Date: 2012-06-01 10:52:59 $";

    private String clientId;
    private String clientPassword;
    private String proxyClassName;
    private String postProcStylesheetName;
    private String preProcStylesheetName;
    private String proxyInputSchema;
    private Transformer preProcTransformer = null;
    private Transformer postProcTransformer = null;
    private Transformer proxyInputFixTransformer = null;


	/**
	  * configure the pipe by creating the required XSLT-transformers using
	  * {@link #createTransformers() }
	  */
	public void configure() throws ConfigurationException {
		super.configure();
	    createTransformers();
	}
	public void start() throws PipeStartException{
		log.debug(getLogPrefix(null)+"creates proxy with class [" + proxyClassName + "]");
		try {
			createProxy(proxyClassName); // just to try if is possible...
		} catch (ConfigurationException e) {
			throw new PipeStartException(e);
		}

	}
	
    public CoolGenXMLProxy createProxy(String proxyName)
        throws ConfigurationException {
        CoolGenXMLProxy proxy;
        try {
            Class klass = Class.forName(proxyName);
            proxy = (CoolGenXMLProxy) klass.newInstance();
            proxy.setClientId(getClientId());
            proxy.setClientPassword(getClientPassword());
            if (log.isDebugEnabled())
                proxy.setTracing(1);
            else
                proxy.setTracing(0);
        } catch (Exception e) {
            throw new ConfigurationException(getLogPrefix(null)+"could not create proxy ["+proxyName+"]", e);
        }
        return proxy;
    }
/**
 * create the required XSLT-transformers.
 *
 * <p>Transformers are created using the URIs supplied in:
 * <ul>
 * <li>{@link #setPreProcStylesheetName(String) preProcStylesheetName}</li>
 * <li>{@link #setPostProcStylesheetName(String) postProcStylesheetName}</li>
 * <li>{@link #setProxyInputSchema(String) proxyInputSchema}</li>
 * <ul>
 * For the proxyInputSchema a transformer is created to check the conformance to the schema
 * the URI supplieds points to.
 *
 * Creation of the proxy itself is done in the {@link #start()} method, to be able to recreate it
 * by restarting the adapter.
 */
public void createTransformers() throws ConfigurationException {

    // Create transformers
    if (preProcStylesheetName != null) {
        try {

            URL preprocUrl = ClassUtils.getResourceURL(this, preProcStylesheetName);

            if (preprocUrl == null)
                throw new ConfigurationException(
                        getLogPrefix(null)+"cannot find resource for preProcTransformer, URL-String ["
                        + preProcStylesheetName
                        + "]");

            log.debug(getLogPrefix(null)+"creating preprocTransformer from URL ["
                    + preprocUrl.toString()
                    + "]");
            preProcTransformer = XmlUtils.createTransformer(preprocUrl);
        } catch (IOException e) {
            throw new ConfigurationException(
                getLogPrefix(null)+"cannot retrieve [" + preProcStylesheetName + "]",
                e);
        } catch (javax.xml.transform.TransformerConfigurationException te) {
            throw new ConfigurationException(
                getLogPrefix(null)+"got error creating transformer from file ["
                    + preProcStylesheetName
                    + "]",
                te);
        }
    }
    if (postProcStylesheetName != null) {
        try {

            URL postprocUrl = ClassUtils.getResourceURL(this, postProcStylesheetName);
            if (postprocUrl == null)
                throw new ConfigurationException(
                    getLogPrefix(null)+"cannot find resource for postProcTransformer, URL-String ["
                        + postProcStylesheetName
                        + "]");

            log.debug(
                getLogPrefix(null)+"creating postprocTransformer from URL ["
                    + postprocUrl.toString()
                    + "]");
            postProcTransformer = XmlUtils.createTransformer(postprocUrl);
        } catch (IOException e) {
            throw new ConfigurationException(
                getLogPrefix(null)+"cannot retrieve [" + postProcStylesheetName + "]",
                e);
        } catch (javax.xml.transform.TransformerConfigurationException te) {
            throw new ConfigurationException(
                getLogPrefix(null)+"got error creating transformer from file ["
                    + postProcStylesheetName
                    + "]",
                te);
        }
    }

    if (proxyInputSchema != null) {
        String stylesheet;
        URL schemaUrl = ClassUtils.getResourceURL(this, proxyInputSchema);

        if (schemaUrl == null)
            throw new ConfigurationException(
                getLogPrefix(null)+"cannot find resource for proxyInputSchema, URL-String ["
                    + proxyInputSchema
                    + "]");

        log.debug(
            getLogPrefix(null)+"creating CoolGenInputViewSchema from URL ["
                + schemaUrl.toString()
                + "]");

        // construct a xslt-stylesheet to perform validation to supplied schema
        stylesheet =
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">";
        stylesheet += "	<xsl:template match=\"/*\">";
        stylesheet += "		<xsl:copy>";
        stylesheet += "			<xsl:attribute name=\"xsi:noNamespaceSchemaLocation\">"
            + schemaUrl.toString()
            + "</xsl:attribute>";
        stylesheet += "			<xsl:apply-templates select=\"node()\"/>";
        stylesheet += "		</xsl:copy>";
        stylesheet += "	</xsl:template>";
        stylesheet += "	<xsl:template match=\"@*|node()\">";
        stylesheet += "		<xsl:copy>";
        stylesheet += "			<xsl:apply-templates select=\"@*|node()\"/>";
        stylesheet += "		</xsl:copy>";
        stylesheet += "	</xsl:template>";
        stylesheet += "</xsl:stylesheet>";

        try {
            proxyInputFixTransformer = XmlUtils.createTransformer(stylesheet);
        } catch (javax.xml.transform.TransformerConfigurationException te) {
            throw new ConfigurationException(
                getLogPrefix(null)+"got error creating transformer from string ["
                    + stylesheet
                    + "]",
                te);
        }
    }
}
/**
 * Transform the input (optionally), check the conformance to the schema (optionally),
 * call the required proxy, transform the output (optionally)
 */
public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {

    Writer proxyResult;
    String proxypreProc = null;
    Variant inputVar;
    String wrapperResult = "";
    CoolGenXMLProxy proxy;
    
    ActionListener actionListener = new ActionListener() {
        /**
         * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
         */
        public String errorMessage;
        public void actionPerformed(ActionEvent e) {
            errorMessage = e.toString();

        }

        public String toString() {
            return errorMessage;
        }
    };

    Source in;

    // TEMPORARY FIX:
    // create proxy before every request, to work around broken connections caused by restarting the comm-bridge
    // should be solved in another way in a more definitive implementation
        
    try {
        log.info(getLogPrefix(session)+"instantiating proxy ["+proxyClassName+"] as a temporary fix for broken comm-bridge connections");
        proxy = createProxy(proxyClassName);
    } catch (ConfigurationException ce) {
	    String msg =getLogPrefix(session)+"cannot recreate proxy after exception";
	    log.error(msg, ce);
	    throw new PipeRunException(this, msg, ce);
    }


    proxy.addExceptionListener(actionListener);

    try {
        inputVar = new Variant(input);
        in = inputVar.asXmlSource();

        if (preProcTransformer != null) {
            proxypreProc = XmlUtils.transformXml(preProcTransformer, in);
            log.debug(
                getLogPrefix(session)
                    + "] preprocessing transformed message into ["
                    + proxypreProc
                    + "]");
        } else
            proxypreProc = inputVar.asString();

        if (proxyInputFixTransformer != null)
            proxypreProc = XmlUtils.transformXml(proxyInputFixTransformer, proxypreProc);

        proxyResult = new StringWriter(10 * 1024);

        
        // Try to execute the service-preProc as per proxy

        try {
            proxy.clear();
        } catch (PropertyVetoException e) {
            throw new PipeRunException(this, getLogPrefix(session)+"cannot clear CoolGen proxy", e);
        }
        try {
        proxy.executeXML(new StringReader(proxypreProc), proxyResult);
        proxy.removeExceptionListener(actionListener);

        String err = actionListener.toString();
        if (err != null) {
	        // if an error occurs, recreate the proxy and throw an exception
            log.debug(
                getLogPrefix(session)
                    + "got error, recreating proxy with class ["
                    + proxyClassName
                    + "]");
            try {
                proxy = createProxy(proxyClassName);
            } catch (ConfigurationException e) {
                throw new PipeRunException(this,  getLogPrefix(session)+"cannot recreate proxy ["+proxyClassName+"]", e);
            }
            throw new PipeRunException(this,  getLogPrefix(session)+"error excuting proxy ["+proxyClassName+"]:"+ err);
        }
        } catch (XmlProxyException xpe) {
            try {
                proxy = createProxy(proxyClassName);
            } catch (ConfigurationException ce) {
                log.error(getLogPrefix(session)+"cannot recreate proxy", xpe);
            }
            throw new PipeRunException(this,  getLogPrefix(session)+"error excecuting proxy", xpe);
        }

        if (postProcTransformer != null) {
            log.debug(getLogPrefix(session)+" CoolGen proxy returned: [" + proxyResult.toString() + "]");
            wrapperResult = XmlUtils.transformXml(postProcTransformer, proxyResult.toString());
        } else
            wrapperResult = proxyResult.toString();
	} catch (DomBuilderException e) {
		throw new PipeRunException(this, getLogPrefix(session)+"DomBuilderException excecuting proxy", e);
    } catch (IOException e) {
        throw new PipeRunException(this, getLogPrefix(session)+"IOException excecuting proxy", e);
    } catch (TransformerException e) {
        throw new PipeRunException(this, getLogPrefix(session)+"TransformerException excecuting proxy", e);
    }

    return new PipeRunResult(getForward(),wrapperResult) ;
}


	public void setClientId(java.lang.String newClientId) {
		clientId = newClientId;
	}
	public String getClientId() {
	    return clientId;
	}
	
	public void setClientPassword(java.lang.String newClientPassword) {
		clientPassword = newClientPassword;
	}
	public String getClientPassword() {
	    return clientPassword;
	}

	public void setPreProcStylesheetName(String newPreProcStylesheetName) {
		preProcStylesheetName = newPreProcStylesheetName;
	}
	public String getPreProcStylesheetName() {
		return preProcStylesheetName;
	}

	public void setPostProcStylesheetName(String newPostProcStylesheetName) {
		postProcStylesheetName = newPostProcStylesheetName;
	}
	public String getPostProcStylesheetName() {
	    return postProcStylesheetName;
	}
	
	public String getProxyClassName() {
	    return proxyClassName;
	}
	public String getProxyInputSchema() {
	    return proxyInputSchema;
	}
	
	public void setProxyInputSchema(String newProxyInputSchema) {
		proxyInputSchema = newProxyInputSchema;
	}
	public void setProxyClassName(java.lang.String newProxyClassName) {
	    proxyClassName = newProxyClassName;
	}
}
