package com.youdevise.hudson.slavestatus;

import java.io.Serializable;

public class MemoryReporter implements StatusReporter, Serializable {
    private static final long serialVersionUID = 1L;

    public String getName() { return "memory"; }

    public String getContent() {
        Runtime runtime = Runtime.getRuntime();
        return String.format("<free>%s</free><total>%s</total><max>%s</max>", 
                             runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory());
    }
}
