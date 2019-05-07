package nl.nn.adapterframework.extensions.cxf;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.log4j.Logger;

/**
 * Publish an Endpoint for a given address. Older WebServiceSenders using the 
 * RPCrouter don't necessarily listen to an URL but rather a NamespaceURI.
 * This allows for backwards compatibility for those services.
 * 
 * @author Niels Meijer
 * 
 */
public class NamespaceUriProviderManager {

	protected Logger log = LogUtil.getLogger(this);
	private EndpointImpl namespaceRouter = null;

	public void init() {
		if(namespaceRouter  == null) {
			log.debug("registering NamespaceURI Provider with JAX-WS CXF Dispatcher");
			namespaceRouter = new EndpointImpl(BusFactory.getDefaultBus(), new NamespaceUriProvider());
			namespaceRouter.publish("/rpcrouter");

			if(namespaceRouter.isPublished())
				log.debug("published NamespaceURI Provider on CXF endpoint[rpcrouter] with SpringBus["+namespaceRouter.getBus().getId()+"]");
			else
				log.error("unable to NamespaceURI Service Provider on CXF endpoint[rpcrouter]");
		}
	}

	public void destroy() {
		if(namespaceRouter != null && namespaceRouter.isPublished())
			namespaceRouter.stop();

		namespaceRouter = null;
	}
}
