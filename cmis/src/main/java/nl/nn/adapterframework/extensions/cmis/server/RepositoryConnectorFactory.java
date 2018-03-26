package nl.nn.adapterframework.extensions.cmis.server;

import java.util.Map;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.apache.log4j.Logger;


/**
 * Implementation of a repository factory without back-end for test purposes.
 */
public class RepositoryConnectorFactory extends AbstractServiceFactory {

	private static final Logger LOG = LogUtil.getLogger(RepositoryConnectorFactory.class);
	private ThreadLocal<CallContextAwareCmisService> threadLocalService = new ThreadLocal<CallContextAwareCmisService>();

	@Override
	public void init(Map<String, String> parameters) {
		LOG.info("Initialized proxy repository service");
	}

	@Override
	public void destroy() {
		LOG.info("Destroyed proxy repository service");
	}

	@Override
	public CmisService getService(CallContext context) {
//		HttpServletRequest req = (HttpServletRequest) context.get(CallContext.HTTP_SERVLET_REQUEST);
//		ServletContext cont = (ServletContext) context.get(CallContext.SERVLET_CONTEXT);
//		System.out.println(req.getRequestURI());

		CallContextAwareCmisService service = threadLocalService.get();
		if (service == null) {
			service = new ConformanceCmisServiceWrapper(createService(context));
			threadLocalService.set(service);
		}

		service.setCallContext(context);

		return service;
	}

	protected FilterCmisService createService(CallContext context) {
		HttpSessionCmisService service = null;
		try {
			service = new HttpSessionCmisService(context);
		} catch (Exception e) {
			throw new CmisRuntimeException("Could not create service instance: " + e, e);
		}

		return service;
	}
}