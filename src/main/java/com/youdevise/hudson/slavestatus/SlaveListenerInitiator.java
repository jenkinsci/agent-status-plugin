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
    
    private final Logger logger;
    private int port; 
    private List<StatusReporter> reporters = new ArrayList<StatusReporter>();

    public SlaveListenerInitiator(int port) {
        this(port, Logger.getLogger(SlaveListenerInitiator.class.getName()));
    }
    
    public SlaveListenerInitiator(int port, Logger logger) {
        setPort(port);
        this.logger = logger;
        logger.info(STARTUP_LOG_MESSAGE);
        register(new IsRunningReporter());
        register(new MemoryReporter());
    }

    @Override
    public void onOnline(Computer slave) {
        logger.info(String.format(SLAVE_STARTUP_LOG_MESSAGE, slave.getName()));
        try {
            SlaveListener listener = new SlaveListener(port, reporters.toArray(new StatusReporter[0]));
            slave.getChannel().callAsync(listener);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not call slave", e);
        }
    }

    public void register(StatusReporter reporter) {
        reporters.add(reporter);
    }

    public void setPort(int port) {
        this.port = port;
    }
}
