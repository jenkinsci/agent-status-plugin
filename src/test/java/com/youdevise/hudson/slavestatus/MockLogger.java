package com.youdevise.hudson.slavestatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static junit.framework.Assert.fail;

@SuppressWarnings("serial")
public class MockLogger extends Logger implements Serializable { 
    private List<LogRecord> logRecords = new ArrayList<LogRecord>();

    protected MockLogger() { 
        super("mock", null); 
        setLevel(Level.ALL);
    }
    
    @Override
    public void log(LogRecord logRecord) {
        logRecords.add(logRecord);
    }
    
    public void verifyLogs(LogRecord ... expectedLogRecords) {
        if (logRecords.size() != expectedLogRecords.length) { 
            fail("Wrong number of log records - expected " + expectedLogRecords.length + ", got " + logRecords.size()); 
        }
        
        for (int i = 0; i < logRecords.size(); i++) {
            compareLogRecords(expectedLogRecords[i], logRecords.get(i));
        }
    }
    
    public LogRecord makeThrowableLogRecord(Level level, Throwable throwable) {
        LogRecord logRecord = new LogRecord(level, "");
        logRecord.setThrown(throwable);
        return logRecord;
    }

    private void compareLogRecords(LogRecord expectedLogRecord, LogRecord actualLogRecord) {
        String recordsString = String.format("expected: %s, actual: %s",
                                             logRecordToString(expectedLogRecord),
                                             logRecordToString(actualLogRecord));
        
        if (expectedLogRecord.getLevel() != actualLogRecord.getLevel()) { 
            fail("Levels don't match - " + recordsString); 
        }
        if (expectedLogRecord.getThrown() != null || actualLogRecord.getThrown() != null) {
            if (expectedLogRecord.getThrown().getClass() != actualLogRecord.getThrown().getClass()) {
                fail("Thrown objects don't match - " + recordsString);
            }
        } else {
            if (!expectedLogRecord.getMessage().equals(actualLogRecord.getMessage())) {
                fail("Messages don't match - " + recordsString);
            }
        }
    }

    private String logRecordToString(LogRecord logRecord) {
        String thrownString = "";
        if (logRecord.getThrown() != null) {
            thrownString = String.format(" (threw %s)", logRecord.getThrown().getClass().getName());
        } 
        return String.format("%s: %s%s",
                             logRecord.getLevel().getName(),
                             logRecord.getMessage(),
                             thrownString);
    }
}
