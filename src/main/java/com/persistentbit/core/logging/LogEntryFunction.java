package com.persistentbit.core.logging;

import java.util.Optional;

/**
 * TODOC
 *
 * @author petermuys
 * @since 30/12/16
 */
public class LogEntryFunction implements LogEntry{
	private LogContext    source;
	private LogEntryGroup logs;
	private String        params;
	private Long		  timeStampDone;
	private String		  resultValue;

	private LogEntryFunction(LogContext source, String params, LogEntryGroup logs,Long timeStampDone,String resultValue) {
		this.source = source;
		this.params = params;
		this.logs = logs;
		this.timeStampDone = timeStampDone;
		this.resultValue = resultValue;
	}
	static public LogEntryFunction of(LogContext source){
		return new LogEntryFunction(source,null,LogEntryGroup.empty(),null,null);
	}

	@Override
	public LogEntryFunction append(LogEntry other) {
		return new LogEntryFunction(source, params, logs.append(other),timeStampDone,resultValue);
	}

	@Override
	public Optional<LogContext> getContext() {
		return Optional.ofNullable(source);
	}

	public Optional<String> getParams() {
		return Optional.ofNullable(params);
	}

	public LogEntryGroup getLogs() {
		return logs;
	}

	public Optional<String> getResult() {
		return Optional.ofNullable(resultValue);
	}

	public Optional<Long> getTimestampDone() {
		return Optional.ofNullable(timeStampDone);
	}

	public LogEntryFunction	withTimestampDone(long timeStampDone){
		return new LogEntryFunction(source,params,logs,timeStampDone,resultValue);
	}
	public LogEntryFunction withParams(String params){
		return new LogEntryFunction(source,params,logs,timeStampDone,resultValue);
	}
	public LogEntryFunction withResultValue(String resultValue){
		return new LogEntryFunction(source,params,logs,timeStampDone,resultValue);
	}

	@Override
	public String toString() {
		return "fun " + source.getMethodName() + "(" + getParams().orElse("") + ")" + "{ " + getLogs() + "}";
	}
}