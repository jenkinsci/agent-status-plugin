package com.youdevise.hudson.slavestatus;

import java.io.IOException;

import net.sf.json.JSONObject;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Descriptor.FormException;
import org.kohsuke.stapler.StaplerRequest;

public class SlaveStatusPlugin extends Plugin {
    private static final int DEFAULT_PORT = 3141;

    @Extension
    public static final SlaveListenerInitiator initiator = new SlaveListenerInitiator(DEFAULT_PORT);

    private int port = DEFAULT_PORT;
    
    @Override
    public void start() throws Exception {
        load();
        initiator.setPort(port);
    }
    
    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, FormException {
        port = formData.optInt("port", DEFAULT_PORT);
        initiator.setPort(port);
        save();
    }
    
    public int getPort() { return port; }
}
