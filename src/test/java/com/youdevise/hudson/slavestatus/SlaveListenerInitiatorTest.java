package com.youdevise.hudson.slavestatus;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.servlet.ServletException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor.FormException;
import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.slaves.NodeDescriptor;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.RetentionStrategy;
import hudson.util.ClockDifference;
import hudson.util.DescribableList;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertEquals;

public class SlaveListenerInitiatorTest {
    private static final String COMPUTER_NAME_1 = "slave 1";
    private static final String COMPUTER_NAME_2 = "slave 2";
    private static final String STARTUP_LOG_MESSAGE = "Initialising slave-status plugin";
    private static final String SLAVE_1_START_LOG_MESSAGE = "Starting slave-status listener on " + COMPUTER_NAME_1;
    private static final String SLAVE_2_START_LOG_MESSAGE = "Starting slave-status listener on " + COMPUTER_NAME_2;
    private static final int PORT_1 = 2230;
    private static final int PORT_2 = 2232;

    private MockChannel channel1;
    private Computer computer1;
    private MockChannel channel2;
    private Computer computer2;
    private MockLogger logger;
    private SlaveListenerInitiator initiator;
    
    @Before
    public void setUp() throws FormException {
        channel1 = new MockChannel();
        computer1 = new MockComputer(COMPUTER_NAME_1, channel1);
        channel2 = new MockChannel();
        computer2 = new MockComputer(COMPUTER_NAME_2, channel2);
        logger = new MockLogger();
        initiator = new SlaveListenerInitiator(PORT_1, logger);
    }
    
    @Test
    public void startsListenersWhenSlavesStart() throws FormException {
        initiator.register(new DummyReporter());
        initiator.onOnline(computer1);
        initiator.onOnline(computer2);

        assertNotNull(channel1.callable);
        checkCallable(channel1, PORT_1, DummyReporter.class);
        assertNotNull(channel2.callable);
        checkCallable(channel2, PORT_1, DummyReporter.class);
    }
    
    @Test
    public void canChangePortOnTheFly() throws FormException {
        initiator.onOnline(computer1);
        assertNotNull(channel1.callable);
        checkCallable(channel1, PORT_1);

        initiator.setPort(PORT_2);
        initiator.onOnline(computer2);
        assertNotNull(channel2.callable);
        checkCallable(channel2, PORT_2);
    }

    @SuppressWarnings("unchecked")
    private void checkCallable(MockChannel channel, int port, Class ...additionalReporterClasses) {
        Callable callable = channel.callable;
        assertEquals(SlaveListener.class, callable.getClass());
        SlaveListener listener = (SlaveListener) callable; 
        assertEquals(port, listener.getPort());
        
        List<Class> expectedReporterClasses = new ArrayList<Class>();
        expectedReporterClasses.add(IsRunningReporter.class);
        expectedReporterClasses.add(MemoryReporter.class);
        expectedReporterClasses.addAll(Arrays.asList(additionalReporterClasses));
        
        int i=0;
        List<StatusReporter> actualReporterClasses = listener.getReporters();
        for (Class reporterClass : expectedReporterClasses) {
            assertEquals(reporterClass, actualReporterClasses.get(i++).getClass());
        }
    }
    
    @Test
    public void logsOnConstruction() { 
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE));
    }
    
    @Test 
    public void logsWhenSlavesStart() {
        initiator.onOnline(computer1);
        initiator.onOnline(computer2);
        
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                          new LogRecord(Level.INFO, SLAVE_1_START_LOG_MESSAGE),
                          new LogRecord(Level.INFO, SLAVE_2_START_LOG_MESSAGE));
    }

    @Test
    public void logsExceptionWhenSlaveCallFails() throws FormException {
        channel1.shouldThrowException = true;
        initiator.onOnline(computer1); 
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                          new LogRecord(Level.INFO, SLAVE_1_START_LOG_MESSAGE),
                          logger.makeThrowableLogRecord(Level.SEVERE, new IOException()));
    }
}

class MockChannel implements VirtualChannel {
    @SuppressWarnings("unchecked")
    public Callable callable;
    public boolean shouldThrowException = false;
    
    public <V, T extends Throwable> Future<V> callAsync(Callable<V, T> callable) throws IOException {
        if (shouldThrowException) { throw new IOException(); }
        
        this.callable = callable;
        return null;
    }

    public <V, T extends Throwable> V call(Callable<V, T> callable) throws IOException, T, InterruptedException { return null; }
    public void close() throws IOException { }
    public <T> T export(Class<T> type, T instance) { return null; }
    public void join() throws InterruptedException { }
    public void join(long arg0) throws InterruptedException { }
}

class MockComputer extends Computer {
    private final String name;
    private final VirtualChannel channel;
    
    public MockComputer(String name, VirtualChannel channel) throws FormException { 
        super(new MockNode());
        this.name = name;
        this.channel = channel; 
    }
    @Override public VirtualChannel getChannel() { return channel; }
    @Override public String getName() { return name; }
    
    @Override public java.util.concurrent.Future<?> connect(boolean forceReconnect) { return null; }
    @Override public void doLaunchSlaveAgent(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException { }
    @Override public Charset getDefaultCharset() { return null; }
    @Override public List<LogRecord> getLogRecords() throws IOException, InterruptedException { return null; }
    @Override public RetentionStrategy<Computer> getRetentionStrategy() { return null; }
    @Override public boolean isConnecting() { return false; }
}

class MockNode extends Node {
    public String getNodeName() { return null; }
    public Computer createComputer() { return null; }
    public Launcher createLauncher(TaskListener listener) { return null; }
    public FilePath createPath(String absolutePath) { return null; }
    public Set<Label> getAssignedLabels() { return null; }
    public ClockDifference getClockDifference() throws IOException, InterruptedException { return null; }
    public NodeDescriptor getDescriptor() { return null; }
    public Set<Label> getDynamicLabels() { return null; }
    public String getNodeDescription() { return null; }
    public int getNumExecutors() { return 0; }
    public FilePath getRootPath() { return null; }
    public Label getSelfLabel() { return null; }
    public FilePath getWorkspaceFor(TopLevelItem item) { return null; }
    public void setNodeName(String name) { }
    public ACL getACL() { return null; }
    @Override
    public Mode getMode() { return null; }
    @Override
    public DescribableList<NodeProperty<?>, NodePropertyDescriptor> getNodeProperties() { return null; }
}

@SuppressWarnings("serial")
class DummyReporter implements StatusReporter {
    public String getName() { return "test"; }
    public String getContent() { return "Hello"; }
}


