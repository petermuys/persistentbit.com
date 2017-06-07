package com.persistentbit.core.javacodegen;

import com.persistentbit.core.Nullable;
import com.persistentbit.core.collections.PList;
import com.persistentbit.core.printing.PrintableText;
import com.persistentbit.core.utils.BaseValueClass;
import com.persistentbit.core.utils.UString;

import java.util.Optional;

/**
 * TODOC
 *
 * @author petermuys
 * @since 28/05/17
 */
public class JField extends BaseValueClass{
	private final String name;
	private final String definition;
	private final boolean isStatic ;
	private final boolean isFinal;
	private final AccessLevel accessLevel = AccessLevel.Private;
	private final           boolean genGetter;
	private final           boolean genWith;
	@Nullable
	private final           String  doc;
	private final           boolean includeInHash;
	private final           boolean isNullable;
	@Nullable private final String  defaultValue;
	@Nullable private final String initValue;
	@Nullable private final Class primitiveType;

	public JField(String name, String definition, boolean isStatic, boolean isFinal, boolean genGetter,
				 boolean genWith,
				 String doc,
				 boolean includeInHash,
				 boolean isNullable,
				 String defaultValue,
				 String initValue,
				 Class primitiveType
	) {
		this.name = name;
		this.definition = definition;
		this.isStatic = isStatic;
		this.isFinal = isFinal;
		this.genGetter = genGetter;
		this.genWith = genWith;
		this.doc = doc;
		this.includeInHash = includeInHash;
		this.isNullable = isNullable;
		this.defaultValue = null;
		this.initValue = initValue;
		this.primitiveType = primitiveType;
	}

	public JField(String name, String definition,Class primitiveType){
		this(name,definition,
			 false,
			 true,
			 true,
			 true,
			 null,
			 true,
			 false,
			 null,
			 null,
			 primitiveType
		);
	}
	public JField(String name, String definition){
		this(name,definition,null);
	}

	public JField(String name, Class type){
		this(name,type.getSimpleName(), type.isPrimitive() ? type : null);
	}


	public JField primitive(Class cls){
		return copyWith("primitiveType",cls);
	}
	public JField asStatic(){
		return copyWith("isStatic",true);
	}
	public JField asNullable() {
		return copyWith("isNullable",true);
	}
	public JField defaultValue(String defaultValue){
		return copyWith("defaultValue",defaultValue);
	}

	public JField initValue(String initValue){
		return copyWith("initValue",initValue);
	}

	public JField notFinal() {
		return copyWith("isFinal",false);
	}
	public JField noGetter() {
		return copyWith("genGetter", false);
	}
	public JField noWith() {
		return copyWith("genWith",false);
	}
	public JField javaDoc(String doc){
		return copyWith("doc",doc);
	}
	public JField excludeHash(){
		return copyWith("includeInHash",false);
	}

	public String getName() {
		return name;
	}

	public String getDefinition() {
		return definition;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public AccessLevel getAccessLevel() {
		return accessLevel;
	}

	public boolean isGenGetter() {
		return genGetter;
	}

	public boolean isGenWith() {
		return genWith;
	}

	public String getDoc() {
		return doc;
	}

	public boolean isIncludeInHash() {
		return includeInHash;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public Optional<String> getDefaultValue() {
		return Optional.ofNullable(defaultValue);
	}

	public Optional<String> getInitValue() {
		return Optional.ofNullable(initValue);
	}

	public PrintableText printDef() {
		return out -> {
			String res = accessLevel.label();
			res = res.isEmpty()? res : res + " ";
			res = isStatic ? res + " static" : res;
			res = isFinal ? res + " final" : res;
			res = res + "\t" + (isNullable && defaultValue == null ? getNullableDefinition() : definition);
			res = res + "\t" + name;
			res = initValue != null ? "\t=\t" + initValue : res;
			res += ";";
			out.println(res);
		};
	}

	public PrintableText printConstructAssign(){
		return out -> {
			String res = "this." + name + " = ";
			if(isNullable){
				if(defaultValue != null){
					res += name + " == null ? " + defaultValue + " : " + defaultValue;
				} else {
					res += "Objects.requireNotNull(" + name + ", \"" + name + " can not be null\"";
				}

			} else {
				res += name;
			}
			out.println(res + ";");
		};
	}

	public String getNullableDefinition(){
		if(primitiveType == null){
			return definition;
		}
		switch (primitiveType.getSimpleName()){
			case "int" : return "Integer";
			case "byte" : return "Byte";
			case "short" : return "Short";
			case "long" : return "Long";
			case "float" : return "Float";
			case "double" : return "Double";
			default: throw new RuntimeException("Unknown: " + primitiveType);
		}

	}

	public PrintableText printGetter() {
		return out -> {
			if(genGetter == false){
				return;
			}
			String res = "public ";
			res += isStatic ? "static " : "";
			if(isNullable && defaultValue == null){
				res += "Optional<" + getNullableDefinition() + "> ";
			} else {
				res += definition + " ";
			}
			res += "get" + UString.firstUpperCase(name) + "() { return ";
			res += isNullable && defaultValue == null
					? "Optional.ofNullable(" + (isStatic? "" : "this.") + name + "); }"
					: (isStatic ? "" : "this.") + name + "; }";
			out.println(res);
		};
	}

	public JArgument asArgument(){
		return new JArgument(definition,name,isNullable, PList.empty());
	}
}