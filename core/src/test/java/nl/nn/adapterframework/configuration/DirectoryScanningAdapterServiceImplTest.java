package nl.nn.adapterframework.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 5.4
 */
public class DirectoryScanningAdapterServiceImplTest {


    @SuppressWarnings("ConstantConditions")
    @Test
    public void test() {
        IbisContext ibisContext = new IbisContext();
        ibisContext.initContext("/springContextTEST.xml");

        String directory = getClass().getClassLoader().getResource("watcheddirectory").getFile();
        System.out.println("Watching " + directory);
        DirectoryScanningAdapterServiceImpl adapterService = new DirectoryScanningAdapterServiceImpl(directory);
        adapterService.setApplicationContext(ibisContext.getApplicationContext());

        assertEquals(1, adapterService.getAdapters().size());

        adapterService.getAdapter("HelloWorld").getName();

        //assertEquals("TODO", adapterService.getAdapter("TODO").getName());
    }


}
