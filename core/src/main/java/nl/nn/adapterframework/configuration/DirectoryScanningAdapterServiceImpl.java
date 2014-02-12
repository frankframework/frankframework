package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);
    private static final FileFilter IS_XML =  new FileFilter() {
        public boolean accept (File pathname){
            return pathname.getName().endsWith(".xml");
        }
    };

    private long lastScan = 0l;

    private final Map<File, Collection<IAdapter>> watched = new HashMap<File, Collection<IAdapter>>();

    private final File directory;

    private ApplicationContext applicationContext;

    private ScheduledFuture<?> future;


    private int rateInSeconds = 60;


    public DirectoryScanningAdapterServiceImpl(String directory) {
        this.directory = new File(directory);
        schedule();
    }

    private void schedule() {
        if (future != null) {
            future.cancel(false);
        }
        future = EXECUTOR.scheduleAtFixedRate(new Runnable() {
            public void run() {

                DirectoryScanningAdapterServiceImpl.this.scan();
            }
        }, 0, rateInSeconds, TimeUnit.SECONDS);
    }


    public void setRateInSeconds(int rate) {
        this.rateInSeconds = rate;
        schedule();
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
        LOG.debug("Scanning " + directory);
        if (directory.exists()) {
            if (directory.isDirectory()) {

                File[] files = directory.listFiles(IS_XML);

                {
                    Map<File, Collection<IAdapter>> toRemove = new HashMap<File, Collection<IAdapter>>();
                    toRemove.putAll(watched);
                    for (File file : files ) {
                        toRemove.remove(file);
                    }
                    for (Map.Entry<File, Collection<IAdapter>> removedAdapter : toRemove.entrySet()) {
                        LOG.info("File " + removedAdapter.getKey() + " not found any more, unregistering adapters" + removedAdapter.getValue());
                        for (IAdapter a : removedAdapter.getValue()) {
                            stopAndUnRegister(a);
                        }
                        watched.remove(removedAdapter.getKey());
                    }
                }
                for (File file : files) {
                    try {
                        Map<String, IAdapter> adapters = read(file.toURI().toURL());
                        if (adapters == null) {
                            LOG.warn("Could not digest " + file);
                            continue;
                        }
                        if (file.lastModified() > lastScan || ! watched.containsKey(file)) {
                            if (watched.containsKey(file)) {
                                for (IAdapter adapter : watched.get(file)) {
                                    stopAndUnRegister(adapter);
                                }
                            }
                            for (Map.Entry<String, IAdapter> entry : adapters.entrySet()) {
                                if (super.getAdapters().get(entry.getValue().getName()) == null) {
                                    registerAndStart(entry.getValue());
                                } else {
                                    LOG.warn("Cannot register adapter " + entry.getValue().getName() + " because it is registered already");
                                }
                            }
                            watched.put(file, adapters.values());
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

        } else {
            LOG.debug("" + directory + " does not exist");
        }
        lastScan = System.currentTimeMillis();
        notify();
    }

    protected void stopAndUnRegister(IAdapter adapter) {
        if (adapter.getRunState() != RunStateEnum.STARTED) {
            adapter.stopRunning();
        }
        unRegisterAdapter(adapter.getName());
    }

    protected void registerAndStart(final IAdapter adapter) throws ConfigurationException {
        registerAdapter(adapter);
        EXECUTOR.execute(new Runnable() {
            public void run() {
                if (adapter.isAutoStart() && adapter.getRunState() != RunStateEnum.STARTED) {
                    adapter.startRunning();
                }
            }
        });
    }

    synchronized Map<String, IAdapter> read(URL url) throws IOException, SAXException, InterruptedException {
        if (applicationContext == null) {
            wait();
        }
        try {
            ConfigurationDigester configurationDigester = (ConfigurationDigester) applicationContext.getBean("configurationDigester"); // somewhy proper dependency injection gives circular dependency problems

            Digester digester = configurationDigester.getDigester();
            AdapterService catcher = new AdapterServiceImpl();
            digester.push(catcher);
            digester.parse(url.openStream());
            // Does'nt work. I probably don't know how it is supposed to work.
            return catcher.getAdapters();
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
