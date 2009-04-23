package com.youdevise.hudson.slavestatus;

import java.io.Serializable;

public interface StatusReporter extends Serializable {
    String getName();
    String getContent();
}
