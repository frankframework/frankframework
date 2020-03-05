package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import javax.annotation.security.DeclareRoles;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * IsUserInRolePipe Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>Mar 5, 2020</pre>
 */
@DeclareRoles({"IbisTester", "developer, sys-admin"})
public class IsUserInRolePipeTest extends PipeTestBase<IsUserInRolePipe>{

    public void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<HTML><HEAD><TITLE>Servlet Output</TITLE> </HEAD><BODY>");
        if (req.isUserInRole("j2ee") && !req.isUserInRole("guest")) {
            out.println("Hello World");
        } else {
            out.println("Invalid roles");
        }
        out.println("</BODY></HTML>");
    }

    @Override
    public IsUserInRolePipe createPipe() {
        return new IsUserInRolePipe();
    }

    @Before
    public void before() throws Exception {
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
        pipe.setRole("IbisTester");
        pipe.doPipe("asd", session);

    }

    @Test
    public void testUserInRoleViaInput() throws Exception {

    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipe() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: getRole()
     */
    @Test
    public void testGetRole() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setRole(String string)
     */
    @Test
    public void testSetRole() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: getNotInRoleForwardName()
     */
    @Test
    public void testGetNotInRoleForwardName() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setNotInRoleForwardName(String string)
     */
    @Test
    public void testSetNotInRoleForwardName() throws Exception {
//TODO: Test goes here... 
    }



}
