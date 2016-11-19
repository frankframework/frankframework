package nl.nn.adapterframework.webcontrol.api;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ApiExceptionHandler implements ExceptionMapper<ApiException> 
{
	@Produces(MediaType.APPLICATION_JSON)
    public Response toResponse(ApiException exception)
    {
        return Response.status(Status.BAD_REQUEST).entity(("{\"status\":\"error\", \"error\":\"" + exception.getMessage() + "\"}")).build();  
    }
}