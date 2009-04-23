package com.youdevise.hudson.slavestatus;

public class Daemon {
    public enum RunResult {CONTINUE, ABORT};
    public enum RunType {ONCE_ONLY, FOREVER};

    private final DaemonRunner daemonRunner;

    public Daemon(DaemonRunner daemonRunner) {
        this.daemonRunner = daemonRunner;
    }

    public RunResult go(RunType runType) {
        RunResult result;
        do {
            result = daemonRunner.run();
        } while (RunType.FOREVER == runType && RunResult.CONTINUE == result);
        return result;
    }
}

interface DaemonRunner {
    Daemon.RunResult run();
}