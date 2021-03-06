package com.persistbit.core;

import com.persistentbit.core.experiments.javasource.JavaClass;
import com.persistentbit.core.experiments.javasource.JavaDoc;
import com.persistentbit.core.experiments.javasource.JavaField;
import com.persistentbit.core.experiments.javasource.JavaFunction;
import com.persistentbit.core.printing.PrintableText;
import com.persistentbit.core.testing.TestCase;

/**
 * TODOC
 *
 * @author petermuys
 * @since 9/01/17
 */
public class JavaSourceGenTest{

	private static final JavaFunction funTestAll  =
		JavaFunction.name("testAll").doc(JavaDoc.of(PrintableText.fromString("TODO\r\n")));
	static final         TestCase     functionGen = TestCase.name("Java Function generator").code(l -> {
		String txt = funTestAll.printToString();
		l.info(txt);
	});

	static final TestCase classGen = TestCase.name("Java Class generator").code(l -> {
		JavaClass cls = JavaClass.of("com.persistentbit.experiments.tests", "TestClass")
			.javaDoc("Hello world!" + System.lineSeparator())
			.addField(JavaField.of("Integer","id").annotate("@Id").init("null"))
			.addMethod(JavaFunction.name("TestClass")
						   .asConstructor()
						   .doc(JavaDoc.of(jdoc -> {
							   jdoc.println("Main Constructor");
						   }))
			)
			.addMethod(JavaFunction.name("of")
					.asStatic()
					.result("TestClass")
					.content((PrintableText) out -> {
						out.println("return new TestClass();");
					})
			)
			.addMethod(JavaFunction.name("toString")
					.annotation("@Override")
					.result("String")
					.content((PrintableText) out -> {
						out.println("return \"TestCase[\" + id + \"]\";");
					})
			)
			.addMethod(funTestAll);
		String txt = cls.printToString();
		l.info(txt);
	});


	public void testAll() {
		CoreTest.runTests(JavaSourceGenTest.class);
	}

	public static void main(String[] args) {
		new JavaSourceGenTest().testAll();
	}
}
