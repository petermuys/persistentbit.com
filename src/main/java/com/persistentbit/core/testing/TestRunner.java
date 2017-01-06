package com.persistentbit.core.testing;

import com.persistentbit.core.Nothing;
import com.persistentbit.core.logging.*;
import com.persistentbit.core.result.Result;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * A TestRunner runs {@link TestCase} instances.
 *
 * @author Peter Muys
 * @since 5/01/2017
 */
public final class TestRunner extends AbstractLogEntryLogging {

	private LogEntryFunction entry;

	private TestRunner(LogEntryFunction entry) {
		super(2);
		this.entry = entry;
	}

	@Override
	public Nothing add(LogEntry logEntry) {
		entry = entry.append(logEntry);
		return Nothing.inst;
	}



	public static void runAndPrint(TestCase testCase, LogEntryPrinter printer){
		Result<TestCase> resultCase = getTestRunResult(testCase);
		printer.print(resultCase.getLog());
		resultCase.orElseThrow();
	}

	public static void runAndPrint(Class testClass, LogEntryPrinter printer){
		runAndPrint(TestCase.forTestClass(testClass),printer);
	}

	public static void runAndPrint(TestCase testCase) {
		runAndPrint(testCase,defaultTestRunPrinter());
	}
	public static void runAndPrint(Class testClass){
		runAndPrint(testClass,defaultTestRunPrinter());
	}

	public static final LogEntryPrinter defaultTestRunPrinter() {
		return LogPrinter.consoleInColor();
	}


	public static Result<TestCase> getTestRunResult(TestCase testCode) {

		LogEntryFunction fun = LogEntryFunction.of(testCode.getContext()
													   .withTimestamp(System.currentTimeMillis()))
			.withParams("\"" + testCode.getName() + "\"");

		TestRunner tr = new TestRunner(fun);
		try {
			testCode.getTestCode().accept(tr);
			fun = tr.entry.withTimestampDone(System.currentTimeMillis());
			LogEntryFunction finalLog = fun.withResultValue("OK");
			return Result.success(testCode).mapLog(finalLog::append);
		} catch(LoggedException le) {
			fun = tr.entry.withTimestampDone(System.currentTimeMillis());
			LogEntryFunction finalLog = fun
				.withResultValue("TEST FAILED")
				.append(le.getLogs())
				.append(new LogEntryException(le));
			return Result.<TestCase>failure(le).mapLog(finalLog::append);
		} catch(Throwable e) {
			fun = tr.entry.withTimestampDone(System.currentTimeMillis());
			LogEntryFunction finalLog = fun
				.withResultValue("TEST FAILED")
				.append(new LogEntryException(e));
			return Result.<TestCase>failure(e).mapLog(finalLog::append);
		}
	}


	public void isSuccess(Result<?> res) {

		if(res.isPresent() == false){
			throw new TestException("Expected Success, got " + res,0);
		}
	}

	public void isEmpty(Result<?> res) {
		if(res.isEmpty()) {
			return;
		}
		throw new TestException("Expected Empty, got " + res);
	}

	public void isFailure(Result<?> res) {
		if(res.isError()) {
			return;
		}
		throw new TestException("Expected Failure, got " + res);
	}

	public void throwsException(Callable<?> code) {
		throwsException(code, e -> true);
	}

	public void throwsException(Callable<?> code, Predicate<Exception> verifyException) {
		try {
			code.call();
		} catch(Exception e) {
			if(verifyException.test(e) == false) {
				throw new TestException("Verification of thrown Exception failed.", e);
			}
			return;
		}
		throw new TestException("Expected an exception.");
	}

	public void isEquals(Object left, Object right) {
		if(left != null && left.getClass().isArray()){
			if(Arrays.deepEquals((Object[])left,(Object[])right)){
				return;
			}
			throw new TestException("Arrays are not equal:" + left + " != " + right);
		}

		if(Objects.equals(left,right)){
			return;
		}
		throw new TestException("Objects are not equal:" + left + " != " + right);
	}
	public void isNotEquals(Object left, Object right){
		if(left != null && left.getClass().isArray()){
			if(Arrays.deepEquals((Object[])left,(Object[])right) == false){
				return;
			}
			throw new TestException("Arrays are equal:" + left + " != " + right);
		}

		if(Objects.equals(left,right) == false){
			return;
		}
		throw new TestException("Objects are equal:" + left + " != " + right);
	}

	public <T> T runNoException(Callable<T> code) {

		try {
			return code.call();
		} catch(Exception e) {
			throw new TestException("Unexpected exception :-)", e);
		}
	}

	public void isTrue(boolean b) {
		if(b == false) {
			throw new TestException("Expected condition to be true");
		}
	}

	public void isTrue(boolean b, String error) {
		if(b == false) {
			throw new TestException(error);
		}
	}

	public void isFalse(boolean b) {
		if(b == true) {
			throw new TestException("Expected condition to be false");
		}
	}

	public void isFalse(boolean b, String error) {
		if(b == true) {
			throw new TestException(error);
		}
	}


}
