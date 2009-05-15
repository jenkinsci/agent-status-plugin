package com.youdevise.hudson.slavestatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Computer;
import hudson.slaves.ComputerListener;

public class SlaveListenerInitiator extends ComputerListener {
    private static final String STARTUP_LOG_MESSAGE       = "Initialising slave-status plugin";
    private static final String SLAVE_STARTUP_LOG_MESSAGE = "Starting slave-status listener on %s";
    
    private transient Logger logger = null;
    private int port; 
    private List<StatusReporter> reporters = new ArrayList<StatusReporter>();

    public SlaveListenerInitiator(int port) {
        init(port);
    }

    public SlaveListenerInitiator(int port, Logger logger) {
        this.logger = logger;
        init(port);
    }

    private void init(int port) {
        getLogger().info(STARTUP_LOG_MESSAGE);
        setPort(port);
        register(new IsRunningReporter());
        register(new MemoryReporter());
    }

    @Override
    public void onOnline(Computer slave) {
        getLogger().info(String.format(SLAVE_STARTUP_LOG_MESSAGE, slave.getName()));
        try {
            SlaveListener listener = new SlaveListener(port, reporters.toArray(new StatusReporter[0]));
            slave.getChannel().callAsync(listener);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not call slave", e);
        }
    }

    public void register(StatusReporter reporter) {
        reporters.add(reporter);
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    private Logger getLogger() {
        if (null == logger) {
            logger = Logger.getLogger(SlaveListenerInitiator.class.getName());
        }
        return logger;
    }
}
