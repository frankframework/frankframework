package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.SAXException;

/**
 * An implementation of {@link AdapterService} that watches a directory with XML's, every XML representing exactly one adapter.
 * @author Michiel Meeuwissen
 * @since 5.4
 */
public class DirectoryScanningAdapterServiceImpl extends BasicAdapterServiceImpl implements ApplicationContextAware {

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

    private ApplicationContext applicationContext;


    public DirectoryScanningAdapterServiceImpl(String directory) {
        this.directory = new File(directory);
        EXECUTOR.scheduleAtFixedRate(new Runnable() {
            public void run() {

                DirectoryScanningAdapterServiceImpl.this.scan();
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public Map<String, IAdapter> getAdapters() {
        if (lastScan == 0) {
            synchronized(this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }
            }

        }
        return super.getAdapters();
    }



    protected synchronized void scan()  {
        LOG.info("Scanning " + directory);
        if (directory.exists()) {
            if (directory.isDirectory()) {
                for (File file : directory.listFiles(IS_XML)) {
                    try {
                        IAdapter adapter = read(file.toURI().toURL());
                        if (adapter == null) {
                            LOG.warn("Could not digest " + file);
                            continue;
                        }
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
                    } catch (SAXException e) {
                        LOG.error(e.getMessage(), e);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            } else {
                LOG.warn("" + directory + " is not a directory");
            }

        }
        lastScan = System.currentTimeMillis();
        notify();
    }


    synchronized IAdapter read(URL url) throws IOException, SAXException, InterruptedException {
        if (applicationContext == null) {
            wait();
        }
        try {
            ConfigurationDigester configurationDigester = (ConfigurationDigester) applicationContext.getBean("configurationDigester"); // somewhy proper dependency injection gives circular dependency problems

            Digester digester = configurationDigester.getDigester();
            digester.push(this);
            digester.parse(url.openStream());
            // Does'nt work. I probably don't know how it is supposed to work.
            return (IAdapter) digester.getRoot();
        } catch (Throwable t) {
            LOG.error("For " + url + ": " + t.getMessage(), t);
            return null;
        }
    }

  /*  public void setConfigurationDigester(ConfigurationDigester configurationDigester) {
        this.configurationDigester = configurationDigester;
    }*/

    public synchronized void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        notify();

    }
}
