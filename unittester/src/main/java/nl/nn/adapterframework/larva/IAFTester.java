package nl.nn.adapterframework.larva;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.larva.test.IbisTester;
import nl.nn.adapterframework.util.AppConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.transform.TransformerConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@RunWith(Parameterized.class)
public class IAFTester {

    static IbisTester tester = null;
    List<File> scenarios;
    String rootDirectory, testname;
    AppConstants appConstants;
    int waitBeforeCleanup;

    public static void initTest() throws ConfigurationException, TransformerConfigurationException, IOException {
        System.out.println("Starting IBIS");
        tester = new IbisTester();
        tester.initTester();
        System.out.println("Setting Properties");
        System.setProperty("HelloWorld.job.active", "false");
        System.setProperty("junit.active", "true");
        System.setProperty("configurations.names", "${instance.name},NotExistingConfig");
    }

    public IAFTester(String testname, List<File> scenarios, String rootDirectory, AppConstants appConstants, int waitBeforeCleanup) {
        System.out.println("Constructor!");
        this.scenarios = scenarios;
        this.rootDirectory = rootDirectory;
        this.appConstants = appConstants;
        this.waitBeforeCleanup = waitBeforeCleanup;
        this.testname = testname;
    }

    @Test
    public void ibisTest() throws InterruptedException {
        // The problem is different classpaths!!
        // Create message listener and make sure it prints to system.out
        System.out.println("Made it into here!!");
        MessageListener messageListener = new MessageListener();
        List<String> logLevels = messageListener.getLogLevels();
        try {
            messageListener.setSelectedLogLevel("Debug");
            messageListener.setSysOut(logLevels.get(logLevels.size() - 1), true, true);
        }catch (Exception ignored){}
        int[] results = {0, 0, 0};
        int numberOfScenarios = scenarios.size();
        // Run tester and wait for it to end
        messageListener.debugMessage("General", "Added a new message");
        ScenarioTester scenarioTester = new ScenarioTester(tester.getIbisContext(), messageListener, scenarios, rootDirectory, appConstants, results, waitBeforeCleanup, numberOfScenarios, true);
        scenarioTester.start();
        scenarioTester.join();

        /*
         * IT DOESNT START THE ADAPTERS IN <IBIS> TAG
         * DOESNT EVEN TRY
         * BUT START THOSE IN MODULE TAG
         */
        boolean a = exportListener(messageListener, testname);
        System.out.println(testname + " -> " + numberOfScenarios + " : " + results[TestTool.RESULT_OK] + " " + results[TestTool.RESULT_ERROR] + " " + results[TestTool.RESULT_AUTOSAVED]);
        // Check none of them failed and all passed.
        Assert.assertTrue(a);
        Assert.assertEquals(0, results[TestTool.RESULT_ERROR]);
        Assert.assertEquals(numberOfScenarios,results[TestTool.RESULT_OK] + results[TestTool.RESULT_AUTOSAVED]);
    }

    @Parameterized.Parameters( name = "{index}: {0}" )
    public static Collection<Object[]> data() throws IOException, TransformerConfigurationException, ConfigurationException {
        long start = System.currentTimeMillis();
        IAFTester.initTest();
        System.out.println("INITIALIZATION TIME: " + (System.currentTimeMillis()-start));
        System.out.println("#Started Adapters: " + tester.getRunningAdapterCount());
        String rootDirectory = parseAbsolutePath("larva.rootDir");
        String paramExecute = parseAbsolutePath("larva.execute");

        int waitBeforeCleanup = parseInteger("larva.waitBeforeCleanup", 100);
        //AppConstants appConstants = TestTool.getAppConstants(tester.getIbisContext());
        AppConstants appConstants = AppConstants.getInstance();
        if(!paramExecute.startsWith(rootDirectory))
            throw new IllegalArgumentException("Scenario is not in the root directory.");

        System.out.println("Getting the tests ready!!");
        Map<String, List<File>> scenarioFiles = TestPreparer.readScenarioFiles(paramExecute, true, appConstants);
        Iterator<Map.Entry<String, List<File>>> scenarioFilesIterator = scenarioFiles.entrySet().iterator();
        List<Object[]> tests = new ArrayList<Object[]>(scenarioFiles.size());
        while (scenarioFilesIterator.hasNext()) {
            Map.Entry<String, List<File>> scenarioEntry = scenarioFilesIterator.next();
            List<File> scenarioFileList = scenarioEntry.getValue();
            String folderName = getFolderName(scenarioFileList.get(0));
            Object[] vars = {folderName, scenarioFileList, rootDirectory, appConstants, waitBeforeCleanup};
            tests.add(vars);
        }
        System.out.println("Returning " + tests.size() + " tests! in " + (System.currentTimeMillis()-start));
        return tests;
    }

    private static String getFolderName(File file) {
        if(file == null)
            return "Null";
        String parent = file.getParent();
        if(parent == null)
            return "Null";
        String[] arr = parent.split(Pattern.quote(File.separator));
        if (arr.length<1)
            return parent;
        return arr[arr.length-1];
    }

    private static int parseInteger(String key, int def) {
        String str = System.getProperty(key);
        if (str != null)
            def = Integer.parseInt(str);
        System.out.println("Parameter [" + key + "] is " + def);
        return def;
    }

    private static String parseAbsolutePath(String key) throws IOException {
        String path = System.getProperty(key);
        File file = new File(path);
        String canonicalPath = file.getCanonicalPath();
        System.out.println("Parameter [" + key + "] is " + canonicalPath);
        return canonicalPath;
    }

    private boolean exportListener(MessageListener messageListener, String filename) {
        String reportFolderPath = System.getProperty("larva.reportFolder");
        if (reportFolderPath == null)
            return false;
        try {
            JSONArray messages = messageListener.getMessages();
            String content = new JSONObject().put(filename, messages).toString();

            File reportFolder = new File(reportFolderPath);
            filename = filename.replaceAll(Pattern.quote(File.separator), "_");
            File output = new File(reportFolder.getCanonicalPath(), filename + ".json");
            File parent = output.getParentFile();
            if (!parent.exists())
                parent.mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            writer.write(content);
            writer.close();

            System.out.println("Finished writing output to " + output.getCanonicalPath());
            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}