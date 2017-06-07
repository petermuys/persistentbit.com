package com.persistentbit.core.javacodegen;

import com.persistentbit.core.Nullable;
import com.persistentbit.core.collections.PList;
import com.persistentbit.core.collections.PSet;
import com.persistentbit.core.printing.PrintableText;
import com.persistentbit.core.utils.BaseValueClass;
import com.persistentbit.core.utils.UString;

import java.util.function.Function;

/**
 * TODOC
 *
 * @author petermuys
 * @since 28/05/17
 */
public class JClass extends BaseValueClass{
	private final String packageName;
	private final String className;
	private final AccessLevel accessLevel;
	private final String extendsDef;
	private final PList<String> implementsDef;
	private final boolean isFinal;

	private final PList<JField> fields;
	private final PList<JMethod> methods;
	@Nullable
	private final String doc;
	private final PList<String> annotations;
	private final PList<JClass> internalClasses;
	private final boolean isStatic;
	private final PSet<JImport> imports;

	public JClass(String packageName, String className, AccessLevel accessLevel, String extendsDef,
				  PList<String> implementsDef,
				  boolean isFinal,
				  PList<JField> fields,
				  PList<JMethod> methods,
				  String doc,
				  PList<String> annotations,
				  PSet<JImport> imports,
				  PList<JClass> internalClasses,
				  boolean isStatic
	) {
		this.packageName = packageName;
		this.className = className;
		this.accessLevel = accessLevel;
		this.extendsDef = extendsDef;
		this.implementsDef = implementsDef;
		this.isFinal = isFinal;
		this.fields = fields;
		this.methods = methods;
		this.doc = doc;
		this.annotations = annotations;
		this.imports = imports;
		this.internalClasses = internalClasses;
		this.isStatic = isStatic;
	}

	public JClass(String packageName, String className){
		this(
			packageName,
			className,
			AccessLevel.Public,
			null,
			PList.<String>empty(),
			false,
			PList.<JField>empty(),
			PList.<JMethod>empty(),
			null,
			PList.<String>empty(),
			PSet.empty(),
			PList.empty(),
				false
		);
	}
	public JClass packagePrivate(){
		return copyWith("accessLevel",AccessLevel.Private);
	}
	public JClass asFinal() {
		return copyWith("isFinal",true);
	}
	public JClass asStatic() { return copyWith("isStatic",true);}
	public JClass extendsDef(String extendsDef){
		return copyWith("extendsDef",extendsDef);
	}
	public JClass addImplements(String implementsDef){
		return copyWith("implementsDef",this.implementsDef.plus(implementsDef));
	}
	public JClass addField(JField field){
		return copyWith("fields", fields.plus(field));
	}
	public JClass addField(String name, String def, Function<JField,JField> builder){
		return addField(builder.apply(new JField(name,def)));
	}
	public JClass addMethod(JMethod method){
		return copyWith("methods",methods.plus(method));
	}
	public JClass addMethod(String name, String typeDef, Function<JMethod,JMethod> builder){
		return addMethod(builder.apply(new JMethod(name,typeDef)));
	}

	public JClass addImport(JImport imp){
		return copyWith("imports", imports.plus(imp));
	}

	public JClass addInternalClass(JClass cls) {
		return copyWith("internalClasses",internalClasses.plus(cls));
	}

	public JClass addImport(String name){
		return addImport(new JImport(name));
	}

	public JClass addImport(Class cls){
		return addImport(cls.getName());
	}

	public PSet<JImport> getAllImports(){
		return imports
			.plusAll(fields.map(JField::getAllImports).flatten())
			.plusAll(internalClasses.map(JClass::getAllImports).flatten())
			.plusAll(methods.map(JMethod::getAllImports).flatten());
	}


	private PList<JField> getConstructorFields() {
		return fields
				.filter(f -> f.isStatic() == false)
				.filter(f -> f.isFinal() == false || f.getInitValue().isPresent() == false);
	}

	public JClass addMainConstructor(AccessLevel level) {
		JMethod m = new JMethod(className).withAccessLevel(level);
		PList<JField> constFields = getConstructorFields();
		for(JField f : constFields){
			m = m.addArg(f.asArgument());
		}
		m = m.code(out -> {
			for(JField f: constFields){
				out.indent(f.printConstructAssign());
			}
		});

		return addMethod(m);
	}

	public PrintableText printImports() {
		return out -> {
			getAllImports()
				.filter(imp -> imp.includeForPackage(packageName))
				.distinct()
				.forEach(imp -> out.println(imp.print()));
		};
	}
	public PrintableText printFields() {
		return out -> {
			fields.forEach(f -> out.print(f.printDef()));
		};
	}
	public PrintableText printMethods() {
		return out -> {
			methods
				.filter(m -> m.isConstructor() == false)
				.forEach(m -> out.print(m.print()));
		};
	}
	public PrintableText printConstructors() {
		return out -> {
			methods
				.filter(m -> m.isConstructor())
				.forEach(m -> out.print(m.print()));
		};
	}

	public JClass addGettersAndSetters() {
		JClass res = this;
		for(JField f : fields){
			if(f.isGenGetter()){
				res = res.addMethod(f.createGetter());
			}

			if(f.isGenWith()){
				res = res.addWithMethod(f);
			}
		}
		return res;
	}
	public JClass addWithMethod(JField field){
		JMethod m = new JMethod("with" + UString.firstUpperCase(field.getName()),className)
			.addArg(field.asArgument());
		m = m.withCode(out -> {
			String args = getConstructorFields().map(f -> f.getName()).toString(", ");
			out.println("return new " + className + "(" + args + ")");
		});
		return addMethod(m);
	}



	public PrintableText printInternalClasses() {
		return out -> {
			internalClasses.forEach(cls -> out.print(cls.printClass()));
		};
	}

	public PrintableText printClassContent() {
		return out -> {
			out.print(printFields());
			out.println();
			out.print(printInternalClasses());
			out.println();
			out.print(printConstructors());
			//out.print(printGettersSetters());
			out.print(printMethods());
		};
	}

	public PrintableText printClassFile(){
		return out -> {
			out.println("package " + packageName + ";");
			out.println();
			out.print(printImports());
			out.println();
			out.print(printClass());
		};
	}

	public PrintableText printClass() {
		return out -> {
			String res = isStatic ? "static " : "";

			res += accessLevel.label();
			if(res.isEmpty() == false){
				res += " ";
			}
			res += "class " + className;
			res += extendsDef == null ? "" : " extends " + extendsDef;
			if(implementsDef.isEmpty() == false){
				res += " implements " + implementsDef.toString(", ");
			}
			res += " {";
			out.println(res);
			out.indent(printClassContent());
			out.println("}");
		};
	}

	private PList<JField> getBuilderRequiredFields(){
		return fields
			.filter(f -> f.isFinal() == false || f.getInitValue().isPresent() == false)
			.filter(f -> f.isStatic() == false)
			.filter(f -> f.isNullable() == false && f.getDefaultValue().isPresent() == false);
	}

	private JClass addBuilderClass() {
		PList<JField> reqFields = getBuilderRequiredFields();
		String clsGenerics = reqFields.zipWithIndex().map(t-> "_T" + (t._1+1)).toString(", ");
		String clsName = reqFields.isEmpty() ? "Builder" : "Builder<" + clsGenerics + ">";
		JClass bcls = new JClass(packageName,clsName).asStatic();
		for(JField fld : fields.filter(f -> f.isStatic() == false)){
			bcls = bcls.addField(fld.notFinal().noGetter().noWith());

			String resultType = reqFields.isEmpty()
					? "Builder"
					: "Builder<" + reqFields.zipWithIndex().map(t->
					t._2.getName().equals(fld.getName()) ? "SET": "_T" + (t._1+1))
					.toString(", ") + ">";

			JMethod m = new JMethod("set" + UString.firstUpperCase(fld.getName()),resultType)
					.addArg(fld.asArgument())
					.withCode(out -> {
						out.println("this." + fld.getName() + "\t=\t" + fld.getName());
						out.println("return (" + resultType + ")this;");
					});
					//.addAnnotation("@SuppressWarnings(\"unchecked\")");
			bcls = bcls.addMethod(m);

		}
		return addInternalClass(bcls);
	}
	private JClass addBuilderMethods() {
		JClass res = this;
		JMethod m = new JMethod("build",className).asStatic();
		PList<JField> reqFields = getBuilderRequiredFields();

		JArgument setterArg = new JArgument(
			"Function<Builder<" + reqFields.map(f -> "NOT").toString(",")
				+ ">, Builder<" + reqFields.map(f -> "SET").toString(", ") + ">>","setter"
		).addImport(JImport.forClass(Function.class));

		JMethod updated = new JMethod("updated",className)
			.addArg("Function<Builder<>,Builder<>>","updater",false)
			.withCode(out -> {
				out.println("Builder b = new Builder();");
				for(JField f : fields.filter(f -> f.isStatic() == false)){
					out.println("b.set" + UString.firstUpperCase(f.getName()) + "(this." + f.getName() + ");");
				}
				out.println("b = updated.apply(b);");
				out.println("return new " + className + "(" + getConstructorFields().map(f -> "b." + f.getName()).toString(", ") + ");");
			});
		res = res.addMethod(updated);
		JMethod build = new JMethod("build",className).asStatic()
				.addArg(setterArg)
			.withCode(out -> {
				out.println("Builder b = setter.apply(new Builder<>());");
				out.println("return new " + className + "(" + getConstructorFields().map(f -> "b." + f.getName()).toString(", ") + ");");
			});

		res = res.addMethod(build);
		return res;
	}

	public JClass addBuilder() {
		return addBuilderClass().addBuilderMethods();
	}

	static public void main(String...args){
		JClass cls = new JClass("be.schaubroeck.be","PersoonData")
			   	.addField(new JField("id",int.class))
				.addField(new JField("rrn",String.class))
				.addField(new JField("enabled",boolean.class).defaultValue("true"))
				.addField(new JField("inschrijving",Integer.class).asNullable())
				.addGettersAndSetters()
				.addMainConstructor(AccessLevel.Public)
				.addBuilder()
			;
		System.out.println(cls.printClassFile().printToString());
	}
}
