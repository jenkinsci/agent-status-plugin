package com.youdevise.hudson.slavestatus;

import com.youdevise.hudson.slavestatus.Daemon.RunResult;
import com.youdevise.hudson.slavestatus.Daemon.RunType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SlaveListenerTest {  
    private static final String STARTUP_LOG_MESSAGE            = "Slave-status listener starting";
    private static final String WAITING_LOG_MESSAGE            = "Slave-status listener waiting for connection";
    private static final String GOT_CONNECTION_LOG_MESSAGE     = "Slave-status listener got connection";
    private static final String READ_INPUT_LOG_MESSAGE         = "Slave-status listener read input";
    private static final String WROTE_OUTPUT_LOG_MESSAGE       = "Slave-status listener wrote output";
    private static final String FLUSHED_AND_CLOSED_LOG_MESSAGE = "Slave-status listener flushed and closed connection";

    private static final String HTTP_OUTPUT
        = "HTTP/1.0 200 OK\n"
        + "Content-Type: text/xml\n"
        + "Server: Hudson slave-status plugin\n"
        + "\n"
        + "<slave><test>Hello</test></slave>";
    
    private SlaveListener listener;
    private MockLogger logger;
    private MockHTTPListener httpListener;

    @Before
    public void setUp() { 
        httpListener = new MockHTTPListener();
        logger = new MockLogger();
        listener = new SlaveListener(0, RunType.ONCE_ONLY, new DummyStatusReporter());
        listener.setLogger(logger);
        listener.setHTTPListener(httpListener);
    }
    
    @Test
    public void canBeSerialised() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());
        out.writeObject(listener);
    }

    @Test
    public void sendsSameResponseForAnyConnection() throws Throwable {
        assertEquals(RunResult.CONTINUE, listener.call());
        assertTrue("Should wait for connection", httpListener.waitForConnectionCalled);
        assertTrue("Should read all incoming bytes", httpListener.allBytesRead);
        assertEquals(HTTP_OUTPUT, new String(httpListener.outputStream.toByteArray()));
        assertTrue("Should flush and close", httpListener.flushAndCloseCalled);
    }
    
    @Test
    public void logsWhenAllGoesWell() throws Throwable {
        listener.call();
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                          new LogRecord(Level.FINE, WAITING_LOG_MESSAGE),
                          new LogRecord(Level.FINE, GOT_CONNECTION_LOG_MESSAGE),
                          new LogRecord(Level.FINE, READ_INPUT_LOG_MESSAGE),
                          new LogRecord(Level.FINE, WROTE_OUTPUT_LOG_MESSAGE),
                          new LogRecord(Level.FINE, FLUSHED_AND_CLOSED_LOG_MESSAGE));
        
    }
    
    @SuppressWarnings("serial")
    @Test
    public void logsAndDiesOnIOExceptionDuringWait() throws Throwable {
        httpListener = new MockHTTPListener() {
            @Override
            public void waitForConnection() throws IOException {
                throw new BindException("Address already in use");
            }
        };
        listener = new SlaveListener(0, RunType.ONCE_ONLY);
        listener.setLogger(logger);
        listener.setHTTPListener(httpListener);
        assertEquals(RunResult.ABORT, listener.call());
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                          new LogRecord(Level.FINE, WAITING_LOG_MESSAGE),
                          logger.makeThrowableLogRecord(Level.SEVERE, new BindException()));
    }
    
    @SuppressWarnings("serial")
    @Test
    public void logsAndContinuesOnIOExceptionDuringRead() throws Throwable {
        httpListener = new MockHTTPListener() {
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException();
                    }
                };
            }
        };
        listener = new SlaveListener(0, RunType.ONCE_ONLY);
        listener.setLogger(logger);
        listener.setHTTPListener(httpListener);
        assertEquals(RunResult.CONTINUE, listener.call());
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                          new LogRecord(Level.FINE, WAITING_LOG_MESSAGE),
                          new LogRecord(Level.FINE, GOT_CONNECTION_LOG_MESSAGE),
                          logger.makeThrowableLogRecord(Level.SEVERE, new IOException()));
    }

    @SuppressWarnings("serial")
    @Test
    public void logsAndContinuesOnIOExceptionDuringWrite() throws Throwable {
        httpListener = new MockHTTPListener() {
            @Override
            public OutputStream getOutputStream() {
                return new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        throw new IOException();
                    }
                };
            }
        };
        listener = new SlaveListener(0, RunType.ONCE_ONLY);
        listener.setLogger(logger);
        listener.setHTTPListener(httpListener);
        assertEquals(RunResult.CONTINUE, listener.call());
        logger.verifyLogs(new LogRecord(Level.INFO, STARTUP_LOG_MESSAGE),
                          new LogRecord(Level.FINE, WAITING_LOG_MESSAGE),
                          new LogRecord(Level.FINE, GOT_CONNECTION_LOG_MESSAGE),
                          new LogRecord(Level.FINE, READ_INPUT_LOG_MESSAGE),
                          logger.makeThrowableLogRecord(Level.SEVERE, new IOException()));
    }
}

class MockHTTPListener implements HTTPListener {
    private static final long serialVersionUID = 1L;
    private static final String HTTP_INPUT_HEADERS 
        = "GET / HTTP/1.1\n"
        + "Host: localhost:8080\n"
        + "User-Agent: Mozilla/5.0\n"
        + "\n";
    private static final byte BYTES_TO_READ[] = HTTP_INPUT_HEADERS.getBytes();
    
    public boolean waitForConnectionCalled = false;
    public boolean allBytesRead = false;
    public transient ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    public boolean flushAndCloseCalled = false;
    
    public void waitForConnection() throws IOException {
        waitForConnectionCalled = true;
    }

    public InputStream getInputStream() {
        return new InputStream() { 
            private int i = 0;
            
            @Override
            public int read() throws IOException {
                if (i >= BYTES_TO_READ.length) {
                    return -1;
                }
                if (i == BYTES_TO_READ.length - 1) {
                    allBytesRead = true;
                }
                return BYTES_TO_READ[i++];
            }
        };
    }

    public OutputStream getOutputStream() { return outputStream; }

    public void flushAndClose() {
        flushAndCloseCalled = true;
    }
}

class DummyStatusReporter implements StatusReporter, Serializable {
    private static final long serialVersionUID = 1L;
    public String getName() { return "test"; }
    public String getContent() { return "Hello"; }
}
