package com.youdevise.hudson.slavestatus;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;
import hudson.Plugin;
import hudson.model.Descriptor.FormException;

public class SlaveStatusPlugin extends Plugin {
    private static final int DEFAULT_PORT = 3141;

    private SlaveListenerInitiator initiator;
    private int port = DEFAULT_PORT;
    
    public void start() throws Exception {
        load();
        initiator = new SlaveListenerInitiator(port);
        initiator.register();
    }
    
    @Override
    public void configure(JSONObject formData) throws IOException, ServletException, FormException {
        port = (int) formData.optInt("port", DEFAULT_PORT);
        initiator.setPort(port);
        save();
    }
    
    public int getPort() { return port; }
}
