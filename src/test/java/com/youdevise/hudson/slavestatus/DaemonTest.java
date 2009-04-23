package com.youdevise.hudson.slavestatus;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.youdevise.hudson.slavestatus.Daemon.RunResult;
import com.youdevise.hudson.slavestatus.Daemon.RunType;

public class DaemonTest {

    private static final int RUNS_BEFORE_ABORT = 100;

    @Test
    public void runsOnceIfRequestedAndReturnsContinueIfRunnerDoes() {
        MockDaemonRunner runner = new AlwaysContinueRunner();
        Daemon daemon = new Daemon(runner);
        
        RunResult result = daemon.go(RunType.ONCE_ONLY);
        assertEquals(RunResult.CONTINUE, result);
        assertEquals(1, runner.runs);
    }
    
    public void runsOnceIfRequestedAndReturnsAbortIfRunnerDoes() {
        MockDaemonRunner runner = new AlwaysAbortRunner();
        Daemon daemon = new Daemon(runner);
        
        RunResult result = daemon.go(RunType.ONCE_ONLY);
        assertEquals(RunResult.ABORT, result);
        assertEquals(1, runner.runs);
    }
    
    @Test
    public void abortsWhenRunnerSaysTo() {
        MockDaemonRunner runner = new MockDaemonRunner(RUNS_BEFORE_ABORT);
        Daemon daemon = new Daemon(runner);
        
        RunResult result = daemon.go(RunType.FOREVER);
        assertEquals(RunResult.ABORT, result);
        assertEquals(RUNS_BEFORE_ABORT, runner.runs);
    }
}

class AlwaysContinueRunner extends MockDaemonRunner {
    public AlwaysContinueRunner() { super(Integer.MAX_VALUE); }
}

class AlwaysAbortRunner extends MockDaemonRunner {
    public AlwaysAbortRunner() { super(0); }
}

class MockDaemonRunner implements DaemonRunner {
    public int runs;

    private final int runsBeforeAborting;
    
    public MockDaemonRunner(int runsBeforeAborting) {
        this.runsBeforeAborting = runsBeforeAborting;
    }

    public RunResult run() {
        runs++;
        if (runs >= runsBeforeAborting) {
            return RunResult.ABORT;
        } else {
            return RunResult.CONTINUE;
        }
    }
}
