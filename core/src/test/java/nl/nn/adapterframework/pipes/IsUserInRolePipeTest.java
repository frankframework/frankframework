package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import org.apache.commons.lang.NotImplementedException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mockito;

import javax.annotation.security.DeclareRoles;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * IsUserInRolePipe Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>Mar 5, 2020</pre>
 */
public class IsUserInRolePipeTest extends PipeTestBase<IsUserInRolePipe> implements ISecurityHandler {

    private HttpSecurityHandler mockedHTTPRequest = Mockito.mock(HttpSecurityHandler.class);
    //private HttpSecurityHandler mockedHTTPRequest = Mockito.mock(HttpSecurityHandler.class);


    @Override
    public IsUserInRolePipe createPipe() {
        return new IsUserInRolePipe();
    }



    public ISecurityHandler sessionConfig(ISecurityHandler secHandler, IPipeLineSession session) {
        ISecurityHandler handler = secHandler;
        session.setSecurityHandler(handler);
        return handler;
    }

    @After
    public void after() throws Exception {
    }

    @Test
    public void NullNotInRoleForward() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("notInRoleForwardName [blabla] not found");
        pipe.setNotInRoleForwardName("blabla");
        pipe.configure();
        pipe.doPipe("sad", session);
    }


    @Test
    public void testEmptyRoleEmptyInput() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("role cannot be empty");
        pipe.doPipe("", session);

    }

    /**
     * Method: assertUserIsInRole(IPipeLineSession session, String role)
     */
    @Test
    public void testUserIsInRoleViaSetRole() throws Exception {
        sessionConfig(mockedHTTPRequest, session);
        pipe.setRole("IbisTester");
        pipe.doPipe("asd", session);

    }


    @Override
    public boolean isUserInRole(String role, IPipeLineSession session) throws NotImplementedException {
        return false;
    }

    @Override
    public Principal getPrincipal(IPipeLineSession session) throws NotImplementedException {
        return null;
    }


}
