package com.youdevise.hudson.slavestatus;

import java.io.Serializable;

public class IsRunningReporter implements StatusReporter, Serializable {
    private static final long serialVersionUID = 1L;
    
    public String getName() { return "status"; }
    public String getContent() { return "Running"; }
}
