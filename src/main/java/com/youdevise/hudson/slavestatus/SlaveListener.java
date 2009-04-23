package com.youdevise.hudson.slavestatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.remoting.Callable;

import static com.youdevise.hudson.slavestatus.Daemon.RunType;
import static com.youdevise.hudson.slavestatus.Daemon.RunResult;

public class SlaveListener implements Callable<Object, Throwable>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String HTTP_HEADERS
        = "HTTP/1.0 200 OK\n"
        + "Content-Type: text/xml\n"
        + "Server: Hudson slave-status plugin\n"
        + "\n";

    private final int port;
    private final RunType runType;
    private final List<StatusReporter> reporters = new ArrayList<StatusReporter>();
    private transient HTTPListener httpListener;
    private transient Logger logger;
    
    public SlaveListener(int port, StatusReporter ...reporters) throws IOException {
        this(port, RunType.FOREVER, reporters);
    }
    public SlaveListener(int port, RunType runType, StatusReporter ...reporters) {
        this.port = port;
        this.runType = runType;
        this.reporters.addAll(Arrays.asList(reporters));
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void setHTTPListener(HTTPListener httpListener) {
        this.httpListener = httpListener;
    }

    public Object call() throws Throwable { 
        if (null == logger) { logger = Logger.getLogger(this.getClass().getName()); }
        if (null == httpListener) { httpListener = new SocketHTTPListener(port, logger); }

        logger.info("Slave-status listener starting");
        
        Daemon daemon = new Daemon(new DaemonRunner() {
            public RunResult run() {
                logger.fine("Slave-status listener waiting for connection");
                try {
                    httpListener.waitForConnection();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Could not listen on port", e);
                    return RunResult.ABORT;
                }
                try {
                    logger.fine("Slave-status listener got connection");
                    readAndIgnoreInput(httpListener);
                    logger.fine("Slave-status listener read input");
                    httpListener.getOutputStream().write(getOutput());
                    logger.fine("Slave-status listener wrote output");
                    httpListener.flushAndClose();
                    logger.fine("Slave-status listener flushed and closed connection");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Exception when handling request", e);
                }
                return RunResult.CONTINUE;
            }
        });
        
        return daemon.go(runType);
    }
    
    private byte[] getOutput() {
        StringBuffer xml = new StringBuffer();
        xml.append("<slave>");
        for (StatusReporter reporter : reporters) {
            xml.append(String.format("<%s>%s</%s>", reporter.getName(), reporter.getContent(), reporter.getName()));
        }
        xml.append("</slave>");
        return (HTTP_HEADERS + xml).getBytes();
    }
    
    private void readAndIgnoreInput(HTTPListener httpListener) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpListener.getInputStream()));
        String line;
        do {
            line = reader.readLine();
        } while (line.length() > 0);
    }
    
    public int getPort() { return port; }
    public List<StatusReporter> getReporters() { return reporters; }
}

interface HTTPListener {
    void waitForConnection() throws IOException;
    InputStream getInputStream() throws IOException;
    OutputStream getOutputStream() throws IOException;
    void flushAndClose() throws IOException;
}

class SocketHTTPListener implements HTTPListener {
    private final int port;
    private final Logger logger;

    private ServerSocket serverSocket = null;
    private Socket socket = null;

    public SocketHTTPListener(int port, Logger logger) {
        this.port = port;
        this.logger = logger;
    }
    
    public void waitForConnection() throws IOException { 
        if (null == serverSocket) {
            serverSocket = new ServerSocket(port);
            logger.info("Slave-status listener ready on port " + port);
        }
        socket = serverSocket.accept();
    }

    public InputStream getInputStream() throws IOException { return socket.getInputStream(); }
    public OutputStream getOutputStream() throws IOException { return socket.getOutputStream(); }

    public void flushAndClose() throws IOException {
        if (null != socket) {
            socket.getOutputStream().flush();
            socket.close();
        }
    }
}