package nl.nn.adapterframework.configuration;

import java.net.URL;

import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public class DirectoryScanningAdapterServiceImplTest {


    @Test
    public void test() {
        IbisContext ibisContext = new IbisContext();
        ibisContext.initConfig("/springContextTEST.xml", IbisManager.DFLT_CONFIGURATION, "false");

        URL directory = getClass().getClassLoader().getResource("watcheddirectory");
   /*     System.out.println(directory.getFile());
        AdapterService adapterService = new DirectoryScanningAdapterServiceImpl(directory.getFile(),
                ibisContext.getIbisManager().getConfiguration());*/

    //assertEquals(1, adapterService.getAdapters().size());
    //assertEquals("TODO", adapterService.getAdapter("TODO").getName());
    }
}
