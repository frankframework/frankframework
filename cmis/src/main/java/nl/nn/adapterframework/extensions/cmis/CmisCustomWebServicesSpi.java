package nl.nn.adapterframework.extensions.cmis;

import static org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper.HTTP_INVOKER_OBJECT;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.webservices.CmisWebServicesSpi;

public class CmisCustomWebServicesSpi extends CmisWebServicesSpi {
	private final BindingSession bindingSession;

	/**
	 * Constructor.
	 *
	 * @param session
	 */
	public CmisCustomWebServicesSpi(BindingSession session) {
		super(session);
		this.bindingSession = session;
	}

	@Override
	public void close() {
		Object invoker = bindingSession.get(HTTP_INVOKER_OBJECT);
		if (invoker instanceof CmisHttpInvoker) {
			CmisHttpInvoker cmisHttpInvoker = (CmisHttpInvoker) invoker;
			cmisHttpInvoker.close();
		}
		super.close();
	}
}
