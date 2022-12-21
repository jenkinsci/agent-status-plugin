package com.youdevise.hudson.slavestatus;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.NodeDescriptor;
import hudson.slaves.RetentionStrategy;
import hudson.util.ClockDifference;
import hudson.util.DescribableList;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class SlaveListenerInitiatorTest {
    private static final String    COMPUTER_NAME_1           = "slave 1";

    private static final String    COMPUTER_NAME_2           = "slave 2";

    private static final String    STARTUP_LOG_MESSAGE       = "Initialising slave-status plugin";

    private static final String    SLAVE_1_START_LOG_MESSAGE = "Starting slave-status listener on "
                                                                     + COMPUTER_NAME_1;

    private static final String    SLAVE_2_START_LOG_MESSAGE = "Starting slave-status listener on "
                                                                     + COMPUTER_NAME_2;

    private static final int       PORT_1                    = 2230;

    private static final int       PORT_2                    = 2232;

    private MockChannel            channel1;

    private Computer               computer1;

    private MockChannel            channel2;

    private Computer               computer2;

    private MockLogger             logger;

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
        initiator.onOnline(computer1, null);
        initiator.onOnline(computer2, null);

        assertNotNull(channel1.callable);
        checkCallable(channel1, PORT_1, DummyReporter.class);
        assertNotNull(channel2.callable);
        checkCallable(channel2, PORT_1, DummyReporter.class);
    }

    @Test
    public void canChangePortOnTheFly() throws FormException {
        initiator.onOnline(computer1, null);
        assertNotNull(channel1.callable);
        checkCallable(channel1, PORT_1);

        initiator.setPort(PORT_2);
        initiator.onOnline(computer2, null);
        assertNotNull(channel2.callable);
        checkCallable(channel2, PORT_2);
    }

    @SuppressWarnings("unchecked")
    private void checkCallable(final MockChannel channel, final int port,
            final Class... additionalReporterClasses) {
        final Callable callable = channel.callable;
        assertEquals(SlaveListener.class, callable.getClass());
        final SlaveListener listener = (SlaveListener) callable;
        assertEquals(port, listener.getPort());

        final List<Class> expectedReporterClasses = new ArrayList<Class>();
        expectedReporterClasses.add(IsRunningReporter.class);
        expectedReporterClasses.add(MemoryReporter.class);
        expectedReporterClasses
                .addAll(Arrays.asList(additionalReporterClasses));

        int i = 0;
        final List<StatusReporter> actualReporterClasses = listener
                .getReporters();
        for (final Class reporterClass : expectedReporterClasses) {
            assertEquals(reporterClass, actualReporterClasses.get(i++)
                    .getClass());
        }
    }

    @Test
    public void logsOnConstruction() {
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE));
    }

    @Test
    public void logsWhenSlavesStart() {
        initiator.onOnline(computer1, null);
        initiator.onOnline(computer2, null);

        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                new LogRecord(Level.INFO, SLAVE_1_START_LOG_MESSAGE),
                new LogRecord(Level.INFO, SLAVE_2_START_LOG_MESSAGE));
    }

    @Test
    public void logsExceptionWhenSlaveCallFails() throws FormException {
        channel1.shouldThrowException = true;
        initiator.onOnline(computer1, null);
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                new LogRecord(Level.INFO, SLAVE_1_START_LOG_MESSAGE),
                logger.makeThrowableLogRecord(Level.SEVERE, new IOException()));
    }
}

class MockChannel implements VirtualChannel {
    @SuppressWarnings("unchecked")
    public Callable callable;

    public boolean  shouldThrowException = false;

    public <V, T extends Throwable> Future<V> callAsync(
            final Callable<V, T> callable) throws IOException {
        if (shouldThrowException) {
            throw new IOException();
        }

        this.callable = callable;
        return null;
    }

    public <V, T extends Throwable> V call(final Callable<V, T> callable)
            throws IOException, T, InterruptedException {
        return null;
    }

    public void close() throws IOException {
    }

    public <T> T export(final Class<T> type, final T instance) {
        return null;
    }

    @Override
    public void syncLocalIO() throws InterruptedException {

    }

    public void join() throws InterruptedException {
    }

    public void join(final long arg0) throws InterruptedException {
    }
}

class MockComputer extends Computer {
    private final String         name;

    private final VirtualChannel channel;

    public MockComputer(final String name, final VirtualChannel channel)
            throws FormException {
        super(new MockNode());
        this.name = name;
        this.channel = channel;
    }

    @Override
    public VirtualChannel getChannel() {
        return channel;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Boolean isUnix() {
        return null;
    }

    @Override
    public java.util.concurrent.Future<?> _connect(final boolean forceReconnect) {
        return null;
    }

    @Override
    public void doLaunchSlaveAgent(final StaplerRequest req,
            final StaplerResponse rsp) throws IOException, ServletException {
    }

    @Override
    public Charset getDefaultCharset() {
        return null;
    }

    @Override
    public List<LogRecord> getLogRecords() throws IOException,
            InterruptedException {
        return null;
    }

    @Override
    public RetentionStrategy<Computer> getRetentionStrategy() {
        return null;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }
}

class MockNode extends Node {
    @Override
    public String getNodeName() {
        return null;
    }

    @Override
    public Computer createComputer() {
        return null;
    }

    @Override
    public Launcher createLauncher(final TaskListener listener) {
        return null;
    }

    @Override
    public FilePath createPath(final String absolutePath) {
        return null;
    }

    @Override
    public Set<LabelAtom> getAssignedLabels() {
        return null;
    }

    @Override
    public ClockDifference getClockDifference() throws IOException,
            InterruptedException {
        return null;
    }

    @Override
    public Callable<ClockDifference, IOException> getClockDifferenceCallable() {
        return null;
    }

    @Override
    public NodeDescriptor getDescriptor() {
        return null;
    }

    @Override
    public String getLabelString() {
        return null;
    }

    public Set<Label> getDynamicLabels() {
        return null;
    }

    @Override
    public String getNodeDescription() {
        return null;
    }

    @Override
    public int getNumExecutors() {
        return 0;
    }

    @Override
    public FilePath getRootPath() {
        return null;
    }

    @Override
    public LabelAtom getSelfLabel() {
        return null;
    }

    @Override
    public FilePath getWorkspaceFor(final TopLevelItem item) {
        return null;
    }

    @Override
    public void setNodeName(final String name) {
    }

    @Override
    public ACL getACL() {
        return null;
    }

    @Override
    public Mode getMode() {
        return null;
    }

    @Override
    public DescribableList<NodeProperty<?>, NodePropertyDescriptor> getNodeProperties() {
        return null;
    }
}

@SuppressWarnings("serial")
class DummyReporter implements StatusReporter {
    public String getName() {
        return "test";
    }

    public String getContent() {
        return "Hello";
    }
}
