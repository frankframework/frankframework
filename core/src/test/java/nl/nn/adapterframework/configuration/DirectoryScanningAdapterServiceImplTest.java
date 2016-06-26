package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 5.4
 */
public class DirectoryScanningAdapterServiceImplTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test() {
        IbisContext ibisContext = new IbisContext();
        ibisContext.setApplicationServerType("TEST");
        ibisContext.init();
        String directory = getClass().getClassLoader().getResource("watcheddirectory").getFile();
        System.out.println("Watching " + directory);
        DirectoryScanningAdapterServiceImpl adapterService = new DirectoryScanningAdapterServiceImpl(directory);
        assertEquals(1, adapterService.getAdapters().size());
        assertEquals("HelloWorld", adapterService.getAdapter("HelloWorld").getName());
    }

}
