package nl.nn.adapterframework.extensions.cmis.server;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpSession;

import nl.nn.adapterframework.util.AppConstants;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;

/**
 * Uses HTTP sessions to cache {@link CmisBinding} objects.
 */
public class HttpSessionCmisService extends CachedBindingCmisService {

	private static final long serialVersionUID = 1L;

	/** Key in the HTTP session. **/
	public static final String CMIS_BINDING = "org.apache.chemistry.opencmis.bridge.binding";

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public HttpSessionCmisService(CallContext context) {
		setCallContext(context);
	}

	@Override
	public CmisBinding getCmisBindingFromCache() {
		HttpSession httpSession = getHttpSession(false);
		if (httpSession == null) {
			return null;
		}

		lock.readLock().lock();
		try {
			return (CmisBinding) httpSession.getAttribute(CMIS_BINDING);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public CmisBinding putCmisBindingIntoCache(CmisBinding binding) {
		HttpSession httpSession = getHttpSession(true);

		lock.writeLock().lock();
		try {
			CmisBinding existingBinding = (CmisBinding) httpSession.getAttribute(CMIS_BINDING);
			if (existingBinding == null) {
				httpSession.setAttribute(CMIS_BINDING, binding);
			} else {
				binding = existingBinding;
			}

			return binding;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Returns the current {@link HttpSession}.
	 * 
	 * @param create
	 *            <code>true</code> to create a new session, <code>false</code>
	 *            to return <code>null</code> if there is no current session
	 */
	public HttpSession getHttpSession(boolean create) {
		return getHttpServletRequest().getSession(create);
	}

	@Override
	public CmisBinding createCmisBinding() {
		return CmisServletDispatcher.getInstance().getCmisBinding();
	}

	@Override
	public ObjectService getObjectService() {
		return new ObjectServiceImpl(super.getObjectService());
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {

		RepositoryInfo repInfo = getCmisBinding().getRepositoryService().getRepositoryInfo(repositoryId, extension);

		RepositoryInfoImpl newRepInfo = new RepositoryInfoImpl(repInfo);
		String description = repInfo.getDescription();
		if(!description.isEmpty())
			description += " ";
		newRepInfo.setDescription(description + "(forwarded by "+AppConstants.getInstance().get("instance.name")+")");

		return newRepInfo;
	}
}
