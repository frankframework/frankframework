package nl.nn.adapterframework.configuration;

import java.net.URL;

import org.apache.commons.digester.Digester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public class DirectoryScanningAdapterServiceImplTest {


    @Test
    public void test() {
        URL directory = getClass().getClassLoader().getResource("watcheddirectory");
        System.out.println(directory.getFile());
        AdapterService adapterService = new DirectoryScanningAdapterServiceImpl(directory.getFile(), new ConfigurationDigester() {
            @Override
            protected Digester createDigester() {
                throw new UnsupportedOperationException();
            }
        });
        assertEquals(1, adapterService.getAdapters().size());
        assertEquals("TODO", adapterService.getAdapter("TODO").getName());
    }
}
