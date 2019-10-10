package nl.nn.adapterframework.testtool.api;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.testtool.MessageListener;
import nl.nn.adapterframework.testtool.TestPreparer;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.api.ApiException;
import org.apache.log4j.Logger;
import org.bouncycastle.util.test.Test;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Path("/")
public class LarvaApi {
    @Context
    ServletConfig servletConfig;

    static IbisContext ibisContext;
    static String realPath;

    private static Logger logger = LogUtil.getLogger(LarvaApi.class);

    @GET
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/params")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParams() throws ApiException {
        getContext();
        JSONArray logLevels = new JSONArray(MessageListener.getLogLevels());
        JSONObject toReturn = new JSONObject();
        try {
            JSONObject scenarioList = getScenarioList(null, TestTool.getAppConstants(ibisContext), realPath);
            JSONObject rootDirectories = new JSONObject(TestPreparer.scenariosRootDirectories);
            toReturn.append("scenarios", scenarioList);
            toReturn.append("rootDirectories", rootDirectories);
            toReturn.append("logLevels", logLevels);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new ApiException(e.getMessage());
        }

        System.out.println("Ready to return!!" + toReturn.toString());
        return Response.status(Response.Status.OK).entity(toReturn.toString()).build();
    }

    @GET
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/messages/{timestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessages(@PathParam("timestamp") long timestamp) {
        getContext();
        JSONArray messages = MessageListener.getMessages(timestamp);
        if(messages.length() == 0) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        return Response.status(Response.Status.OK).entity(messages.toString()).build();
    }

    @POST
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/execute")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response executeTests(MultipartFormDataInput input) {
        getContext();
        int waitBeforecleanup = 100;
        int numberOfThreads = 1;
        String currentScenariosRootDirectory, paramExecute;

        Map<String, List<InputPart>> data = input.getFormDataMap();

        try {
            waitBeforecleanup = data.get("waitBeforeCleanup").get(0).getBody(Integer.class, int.class);
        }catch (IOException | NullPointerException e) {
            e.printStackTrace();
            logger.error("Could not decode waitBeforeCleanup, using default instead.\n");
        }
        try {
            numberOfThreads = data.get("numberOfThreads").get(0).getBody(Integer.class, int.class);
        }catch (IOException | NullPointerException e) {
            e.printStackTrace();
            logger.error("Could not decode number of threads, using default instead." );
        }
        try {
            currentScenariosRootDirectory = data.get("currentScenariosRootDirectory").get(0).getBodyAsString();
            paramExecute = data.get("paramExecute").get(0).getBodyAsString();
        }catch (IOException | NullPointerException e) {
            logger.error("Could not decode parameters! " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.debug("Creating test runner thread.");
        Thread testRunner = new Thread() {
            String paramExecute, currentScenariosRootDirectory;
            int paramWaitBeforeCleanUp, numberOfThreads;

            @Override
            public void run() {
                System.out.println("Started thread.");
                TestTool.runScenarios(paramExecute, paramWaitBeforeCleanUp, currentScenariosRootDirectory, numberOfThreads);
            }

            Thread initTestParams(String paramExecute, int paramWaitBeforeCleanUp, String currentScenariosRootDirectory, int numberOfThreads) {
                this.paramExecute = paramExecute;
                this.paramWaitBeforeCleanUp = paramWaitBeforeCleanUp;
                this.currentScenariosRootDirectory = currentScenariosRootDirectory;
                this.numberOfThreads = numberOfThreads;

                return this;
            }
        }.initTestParams(paramExecute, waitBeforecleanup, currentScenariosRootDirectory, numberOfThreads);

        logger.info("Starting Tests...");
        testRunner.start();

        if(testRunner.isAlive())
            return Response.status(Response.Status.OK).build();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @POST
//    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/loglevel")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setLogLevel(LinkedHashMap<String, Object> json){
        getContext();
        try {
            MessageListener.setSelectedLogLevel(json.get("logLevel").toString());
            System.out.println("Set the log level to " + json.get("logLevel").toString());
            return Response.status(Response.Status.OK).build();
        }catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/scenarios/{rootDirectory}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getScenarios(@PathParam("rootDirectory") String rootDirectory){
        getContext();
        try {
            JSONObject list = getScenarioList(rootDirectory, TestTool.getAppConstants(ibisContext), null);
            return Response.status(Response.Status.OK).entity(list).build();
        }catch (JSONException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }


    private JSONObject getScenarioList(String rootDirectory, AppConstants appConstants, String realPath) throws JSONException {
        String appConstantsRealPath = appConstants.getResolvedProperty("webapp.realpath");
        if(appConstantsRealPath != null) {
            realPath = appConstantsRealPath + "larva/";
        }
        rootDirectory = TestPreparer.initScenariosRootDirectories(realPath, rootDirectory, appConstants);
        appConstants = TestPreparer.getAppConstantsFromDirectory(rootDirectory, appConstants);
        Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(rootDirectory, false, appConstants);
        Collections.sort(scenarioFiles.get(""));
        Map<String, String> scenarioList = TestPreparer.getScenariosList(scenarioFiles, rootDirectory, appConstants);
        return new JSONObject(scenarioList);
    }

    private void getContext() {
        if(ibisContext!=null)
            return;
        String servletPath = ResteasyProviderFactory.getContextData(HttpServletRequest.class).getServletPath();
        System.out.println("Servlet Path: " + servletPath);
        int i = servletPath.lastIndexOf('/');
        realPath = servletConfig.getServletContext().getRealPath(servletPath.substring(0, i));
        System.out.println("Real Path: " + realPath);
        ibisContext = TestTool.getIbisContext(servletConfig.getServletContext());
    }
}
