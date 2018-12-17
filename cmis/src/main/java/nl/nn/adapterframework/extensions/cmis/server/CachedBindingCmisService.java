package nl.nn.adapterframework.extensions.cmis.server;

import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.PolicyService;
import org.apache.chemistry.opencmis.commons.spi.RelationshipService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;

/**
 * Provides a framework to cache a {@link CmisBinding} object for a
 * {@link FilterCmisService}.
 */
public abstract class CachedBindingCmisService extends FilterCmisService {

	private static final long serialVersionUID = 1L;

	private CmisBinding clientBinding;

	@Override
	public void setCallContext(CallContext context) {
		super.setCallContext(context);

		clientBinding = getCmisBindingFromCache();
		if (clientBinding == null) {
			clientBinding = putCmisBindingIntoCache(createCmisBinding());
		}
	}

	/**
	 * Returns a cached {@link CmisBinding} object or <code>null</code> if no
	 * appropriate object can be found in the cache.
	 * @return a cached CmisBinding link
	 */
	public abstract CmisBinding getCmisBindingFromCache();

	/**
	 * Puts the provided {@link CmisBinding} object into the cache and
	 * associates it somehow with the current {@link CallContext}.
	 * 
	 * The implementation may return another {@link CmisBinding} object if
	 * another thread has already added an object for the current
	 * {@link CallContext}.
	 * @param binding -
	 */
	public abstract CmisBinding putCmisBindingIntoCache(CmisBinding binding);

	/**
	 * Creates a new {@link CmisBinding} object based on the current
	 * {@link CallContext}.
	 */
	public abstract CmisBinding createCmisBinding();

	/**
	 * Returns the current {@link CmisBinding} object.
	 */
	public CmisBinding getCmisBinding() {
		return clientBinding;
	}

	/**
	 * Returns the current {@link HttpServletRequest}.
	 */
	public HttpServletRequest getHttpServletRequest() {
		return (HttpServletRequest) getCallContext().get(CallContext.HTTP_SERVLET_REQUEST);
	}

	@Override
	public RepositoryService getRepositoryService() {
		return clientBinding.getRepositoryService();
	}

	@Override
	public NavigationService getNavigationService() {
		return clientBinding.getNavigationService();
	}

	@Override
	public ObjectService getObjectService() {
		return clientBinding.getObjectService();
	}

	@Override
	public VersioningService getVersioningService() {
		return clientBinding.getVersioningService();
	}

	@Override
	public DiscoveryService getDiscoveryService() {
		return clientBinding.getDiscoveryService();
	}

	@Override
	public MultiFilingService getMultiFilingService() {
		return clientBinding.getMultiFilingService();
	}

	@Override
	public RelationshipService getRelationshipService() {
		return clientBinding.getRelationshipService();
	}

	@Override
	public AclService getAclService() {
		return clientBinding.getAclService();
	}

	@Override
	public PolicyService getPolicyService() {
		return clientBinding.getPolicyService();
	}

	@Override
	public void close() {
		super.close();
		clientBinding = null;
	}
}
