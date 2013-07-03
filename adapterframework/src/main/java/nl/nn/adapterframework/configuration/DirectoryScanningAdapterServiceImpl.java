package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * An implementation of {@link AdapterService} that watches a directory with XML's, every XML representing exactly one adapter.
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public class DirectoryScanningAdapterServiceImpl extends BasicAdapterServiceImpl {

    private static final Logger LOG = LogUtil.getLogger(DirectoryScanningAdapterServiceImpl.class);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final FileFilter IS_XML =  new FileFilter() {
        public boolean accept (File pathname){
            return pathname.getName().endsWith(".xml");
        }
    };

    private long lastScan = 0l;

    private final Map<File, String> watched = new HashMap<File, String>();

    private final File directory;

    private final ConfigurationDigester configurationDigester;

    public DirectoryScanningAdapterServiceImpl(String directory, ConfigurationDigester configurationDigester) {
        this.directory = new File(directory);
        this.configurationDigester = configurationDigester;
        EXECUTOR.scheduleAtFixedRate(new Runnable() {
            public void run() {

                DirectoryScanningAdapterServiceImpl.this.scan();
            }
        }, 1, 1, TimeUnit.MINUTES);
        scan();
    }



    protected synchronized void scan()  {
        if (directory.exists()) {
            if (directory.isDirectory()) {
                for (File file : directory.listFiles(IS_XML)) {
                    try {
                        IAdapter adapter = read(file.toURI().toURL());
                        if (file.lastModified() > lastScan) {
                            if (watched.containsKey(file)) {
                                unRegisterAdapter(watched.get(file));
                            }
                            registerAdapter(adapter);
                            watched.put(file, adapter.getName());
                        }
                    } catch (MalformedURLException e) {
                        LOG.error(e.getMessage(), e);
                    } catch (ConfigurationException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            } else {
                LOG.warn("" + directory + " is not a directory");
            }

        }
        lastScan = System.currentTimeMillis();
    }


    IAdapter read(URL url) {

        Adapter adapter = new Adapter();
        adapter.setName("TODO");
        // TODO digester stuff
        return adapter;
    }
}
