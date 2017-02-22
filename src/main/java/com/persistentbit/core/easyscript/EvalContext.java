package com.persistentbit.core.easyscript;

import com.persistentbit.core.collections.PMap;

import java.util.Objects;
import java.util.Optional;

/**
 * TODO: Add comment
 *
 * @author Peter Muys
 * @since 21/02/2017
 */
public abstract class EvalContext {
    enum Type {
        block,functionCall
    }

	public abstract Optional<Object> getValue(String name);

	public abstract Type getType();

	public abstract EvalContext withValue(String name, Object value);

	public abstract boolean hasLocalValue(String name);

	public EvalContext subContext(Type type) {
		return new ContextImpl(this, type, PMap.empty());
	}


	public abstract EvalContext withParentContext(EvalContext context);

	public abstract EvalContext	getLocalContext();

	private static class ContextImpl extends EvalContext{

		private final EvalContext parentContext;
        private final Type type;
		private final PMap<String, Object> valueLookup;


		public ContextImpl(EvalContext parentContext, Type type, PMap<String, Object> valueLookup) {
			this.parentContext = parentContext;
            this.type = Objects.requireNonNull(type);
			this.valueLookup = Objects.requireNonNull(valueLookup);
		}

        @Override
        public Type getType() {
            return type;
        }

        @Override
		public Optional<Object> getValue(String name) {
			Optional<Object> res = valueLookup.getOpt(name);
			if(res.isPresent()){
                return res;
            }
            return parentContext == null
                    ? Optional.empty()
				: parentContext.getValue(name)
				;
        }

		@Override
		public EvalContext withValue(String name, Object value) {
			return new ContextImpl(parentContext, type, valueLookup.put(name, value));
		}

		@Override
		public boolean hasLocalValue(String name) {
			return valueLookup.containsKey(name);
		}

		@Override
		public EvalContext getLocalContext() {
			return new ContextImpl(null,type,valueLookup);
		}

		@Override
		public EvalContext withParentContext(EvalContext context) {
			if(parentContext != null){
				return parentContext.withParentContext(context);
			}
			return new ContextImpl(context,type,valueLookup);
		}
	}

	public static final EvalContext inst = new ContextImpl(null, Type.block, PMap.empty());

/*
    public static String script(String template){
        PList<TemplateBlock> blocks = TemplateBlock.parse(template);
        Source source = blocks.map(tb ->
                tb.getType()== TemplateBlock.Type.string
                        ? Source.asSource(tb.getPos(), "\"" + StringUtils.escapeToJavaString(tb.getContent()) + "\"")
                        : Source.asSource(tb.getPos(),tb.getContent())
        ).fold(Source.asSource("\"\""),left->right->left.plus(Source.asSource("+")).plus(right));
        return source.rest();
    }


    public static void main(String... args) throws Exception {
        String s = "\r\n<<start>>blabla<<var2>>\r\n<<var3>>end de rest<>!";
        String script = script(s);

        System.out.println(script);
    } */
}
