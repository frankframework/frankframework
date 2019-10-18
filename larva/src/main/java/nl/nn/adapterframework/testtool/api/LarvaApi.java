package nl.nn.adapterframework.testtool.api;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.testtool.MessageListener;
import nl.nn.adapterframework.testtool.TestPreparer;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.api.ApiException;
import org.apache.log4j.Logger;
import org.jboss.resteasy.core.ExceptionAdapter;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private static IbisContext ibisContext;
    private static String realPath;

    private static Logger logger = LogUtil.getLogger(LarvaApi.class);

    /**
     * Returns the param names and possible values that will be required for execution.
     * @return json object that contains all the params.
     * @throws ApiException If there has been a problem creating json.
     */
    @GET
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/params")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParams() throws ApiException {
        getContext();
        JSONArray logLevels = new JSONArray(MessageListener.getLogLevels());
        JSONObject toReturn = new JSONObject();
        try {
            Map<String, Object> scenarioList = getScenarioList(null, TestTool.getAppConstants(ibisContext), realPath);
            JSONObject rootDirectories = new JSONObject(TestPreparer.scenariosRootDirectories);
            toReturn.append("scenarios", scenarioList.get("scenarios"));
            toReturn.append("defaultRootDirectory", scenarioList.get("rootDirectory"));
            toReturn.append("rootDirectories", rootDirectories);
            toReturn.append("logLevels", logLevels);
            toReturn.append("selectedLogLevel", MessageListener.getSelectedLogLevel());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new ApiException(e.getMessage());
        }
        logger.debug("Returning params.");
        return Response.status(Response.Status.OK).entity(toReturn.toString()).build();
    }

    /**
     * Returns the messages after a given timestamp.
     * @param timestamp Timestamp to use for filtering.
     * @return List of messages that fit the criteria.
     */
    @GET
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/messages/{timestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessages(@PathParam("timestamp") long timestamp) {
        getContext();
        JSONArray messages = MessageListener.getMessages(timestamp);
        if(messages.length() == 0) {
            logger.debug("No messages exist.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        logger.debug("Returnin messages after " + timestamp);
        return Response.status(Response.Status.OK).entity(messages.toString()).build();
    }

    /**
     * Executes the tests with the given parameters.
     * @param input Form Data that contains scenario and rootDirectory (required); and logLevel, waitBeforeCleanup and numberOfThreads (optionally)
     * @return Status.OK if the execution started successfully.
     */
    @POST
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/execute")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response executeTests(MultipartFormDataInput input) {
        getContext();
        int waitBeforecleanup = 100;
        int numberOfThreads = 1;
        int timeout = Integer.MAX_VALUE;
        String currentScenariosRootDirectory, paramExecute;

        MessageListener.cleanLogs(true);
        Map<String, List<InputPart>> data = input.getFormDataMap();

        logger.debug("Parsing parameters for execution.");
        try {
            waitBeforecleanup = data.get("waitBeforeCleanup").get(0).getBody(Integer.class, int.class);
        }catch (IOException | NullPointerException | ExceptionAdapter e) {
            e.printStackTrace();
            logger.error("Could not decode waitBeforeCleanup, using default instead.\n");
        }
        try {
            String logLevel = data.get("logLevel").get(0).getBodyAsString();
            MessageListener.setSelectedLogLevel(logLevel);
        } catch (IOException | NullPointerException | ExceptionAdapter e) {
            logger.debug("No log level found.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error setting the log level: " + e.getMessage());
        }
        try {
            numberOfThreads = data.get("numberOfThreads").get(0).getBody(Integer.class, int.class);
        }catch (IOException | NullPointerException | ExceptionAdapter e) {
            e.printStackTrace();
            logger.error("Could not decode number of threads, using default instead." );
        }
        try {
            timeout = data.get("timeout").get(0).getBody(Integer.class, int.class);
            if (timeout == -1)
                timeout = Integer.MAX_VALUE;
        }catch (IOException | NullPointerException | ExceptionAdapter e) {
            e.printStackTrace();
            logger.error("Could not decode number of threads, using default instead." );
        }
        try {
            currentScenariosRootDirectory = data.get("rootDirectory").get(0).getBodyAsString();
            paramExecute = data.get("scenario").get(0).getBodyAsString();
        }catch (IOException | NullPointerException e) {
            logger.error("Could not decode parameters! " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.debug("Creating test runner thread.");
        Thread testRunner = new Thread() {
            String paramExecute, currentScenariosRootDirectory;
            int paramWaitBeforeCleanUp, numberOfThreads, timeout;

            @Override
            public void run() {
                TestTool.runScenarios(paramExecute, paramWaitBeforeCleanUp, currentScenariosRootDirectory, numberOfThreads, timeout);
            }

            Thread initTestParams(String paramExecute, int paramWaitBeforeCleanUp, String currentScenariosRootDirectory, int numberOfThreads, int timeout) {
                this.paramExecute = paramExecute;
                this.paramWaitBeforeCleanUp = paramWaitBeforeCleanUp;
                this.currentScenariosRootDirectory = currentScenariosRootDirectory;
                this.numberOfThreads = numberOfThreads;
                this.timeout = timeout;
                return this;
            }
        }.initTestParams(paramExecute, waitBeforecleanup, currentScenariosRootDirectory, numberOfThreads, timeout);

        logger.info("Starting to execute tests.");
        testRunner.start();

        if(testRunner.isAlive())
            return Response.status(Response.Status.OK).build();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Sets the log level
     * @param json the log level to be set.
     * @return Status.OK if it was set correctly, otherwise BAD request.
     */
    @POST
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/loglevel")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setLogLevel(LinkedHashMap<String, Object> json){
        getContext();
        try {
            MessageListener.setSelectedLogLevel(json.get("logLevel").toString());
            return Response.status(Response.Status.OK).build();
        }catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * @return The logs for the previous test execution.
     */
    @GET
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/archive")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessages() {
        getContext();

        JSONArray archive = MessageListener.getArchive();
        if (archive.length() == 0) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        return Response.status(Response.Status.OK).entity(archive.toString()).build();
    }

    /**
     * Returns the scenarios in a given directory.
     * @param input JSON object containing the directory to start the search from.
     * @return List of scenarios in the given directory.
     */
    @POST
    @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
    @Path("/larva/scenarios/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getScenarios(Map<String, Object> input){
        getContext();
        String rootDirectory = input.get("rootDirectory").toString();
        System.out.println(rootDirectory);
        Map<String, Object> list = getScenarioList(rootDirectory, TestTool.getAppConstants(ibisContext), realPath);
        if(list.size() == 0) {
           return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response.status(Response.Status.OK).entity(new JSONObject(list).toString()).build();
    }

    /**
     * Returns the scenarios found and the root directory they were found in. If rootDirectory is null, it will search for root directories.
     * @param rootDirectory Directory to get the scenarios from.
     * @param appConstants App Constants to use during search.
     * @param realPath Path of the current execution.
     * @return List of scenarios and the root directory they were found in.
     */
    private Map<String, Object> getScenarioList(String rootDirectory, AppConstants appConstants, String realPath) {
        String appConstantsRealPath = appConstants.getResolvedProperty("webapp.realpath");
        if(appConstantsRealPath != null) {
            realPath = appConstantsRealPath + "larva/";
        }
        rootDirectory = TestPreparer.initScenariosRootDirectories(realPath, rootDirectory, appConstants);
        appConstants = TestPreparer.getAppConstantsFromDirectory(rootDirectory, appConstants);
        Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(rootDirectory, false, appConstants);
        Collections.sort(scenarioFiles.get(""));
        Map<String, String> scenarioList = TestPreparer.getScenariosList(scenarioFiles, rootDirectory, appConstants);
        Map<String, Object> result = new HashMap<>();
        result.put("scenarios", scenarioList);
        result.put("rootDirectory", rootDirectory);
        return result;
    }

    /**
     * Initializes the IbisContext.
     */
    private void getContext() {
        if(ibisContext!=null)
            return;
        String servletPath = ResteasyProviderFactory.getContextData(HttpServletRequest.class).getServletPath();
        logger.debug("Servlet Path: " + servletPath);
        int i = servletPath.lastIndexOf('/');
        realPath = servletConfig.getServletContext().getRealPath(servletPath.substring(0, i));
        logger.debug("Real Path: " + realPath);
        ibisContext = TestTool.getIbisContext(servletConfig.getServletContext());
    }
}
