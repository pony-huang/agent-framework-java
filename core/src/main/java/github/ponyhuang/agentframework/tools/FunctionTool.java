package github.ponyhuang.agentframework.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime tool wrapper around an annotated method.
 */
public class FunctionTool {

    private final String name;
    private final String description;
    private final Method method;
    private final Object instance;
    private final Map<String, ParameterInfo> parameterInfos;
    private final ToolInvoker invoker;
    private final Map<String, Object> schemaOverride;
    private final Map<String, Object> parametersOverride;

    private FunctionTool(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.method = builder.method;
        this.instance = builder.instance;
        this.parameterInfos = builder.parameterInfos;
        this.invoker = builder.invoker;
        this.schemaOverride = builder.schemaOverride;
        this.parametersOverride = builder.parametersOverride;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        if (parametersOverride != null) {
            return parametersOverride;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", getProperties());
        params.put("required", getRequiredParams());
        return params;
    }

    public Object invoke(Map<String, Object> arguments) {
        try {
            if (invoker != null) {
                return invoker.invoke(arguments);
            }
            Object[] args = resolveArguments(arguments);
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new ToolExecutionException("Error invoking tool " + name, e);
        }
    }

    public Map<String, Object> toSchema() {
        if (schemaOverride != null) {
            // Check if it's a full schema (with name and description)
            if (schemaOverride.containsKey("name") && schemaOverride.containsKey("description") && schemaOverride.containsKey("parameters")) {
                return schemaOverride;
            }
            // Check if it's a full schema with nested parameters
            if (schemaOverride.containsKey("name") && schemaOverride.containsKey("description")) {
                return schemaOverride;
            }
            // It's a partial schema (just parameters) - need to wrap properly
            Map<String, Object> wrapped = new HashMap<>();
            wrapped.put("name", name);
            wrapped.put("description", description);
            wrapped.put("parameters", schemaOverride);
            return wrapped;
        }
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", name);
        schema.put("description", description);
        schema.put("parameters", getParameters());
        return schema;
    }

    private Map<String, Object> getProperties() {
        if (parameterInfos == null || parameterInfos.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> properties = new HashMap<>();
        for (ParameterInfo info : parameterInfos.values()) {
            Map<String, Object> param = new HashMap<>();
            param.put("type", info.type);
            param.put("description", info.description);
            if (info.defaultValue != null && !info.defaultValue.isEmpty()) {
                param.put("default", info.defaultValue);
            }
            properties.put(info.name, param);
        }
        return properties;
    }

    private java.util.List<String> getRequiredParams() {
        if (parameterInfos == null || parameterInfos.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> required = new java.util.ArrayList<>();
        for (ParameterInfo info : parameterInfos.values()) {
            if (info.required) {
                required.add(info.name);
            }
        }
        return required;
    }

    private Object[] resolveArguments(Map<String, Object> arguments) {
        if (method == null) {
            return new Object[0];
        }
        Object[] args = new Object[method.getParameterCount()];
        int i = 0;
        for (Parameter param : method.getParameters()) {
            ParameterInfo info = parameterInfos.get(param.getName());
            Object value = arguments.get(param.getName());
            args[i++] = convertValue(value, param.getType());
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return ((Number) value).doubleValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return (Boolean) value;
        }
        if (targetType == Map.class) {
            return (Map<String, Object>) value;
        }
        if (targetType == java.util.List.class) {
            return (java.util.List<Object>) value;
        }
        return value;
    }

    /**
     * Creates a FunctionTool from a method.
     *
     * @param method  the method to wrap
     * @param instance the instance to invoke on
     * @return a new FunctionTool
     */
    public static FunctionTool create(Method method, Object instance) {
        return builder()
                .name(method.getName())
                .method(method)
                .instance(instance)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Invoker interface for custom tool implementations.
     */
    @FunctionalInterface
    public interface ToolInvoker {
        Object invoke(Map<String, Object> arguments);
    }

    /**
         * Parameter information holder.
         */
        private record ParameterInfo(String name, String type, String description, boolean required, String defaultValue) {
    }

    /**
     * Builder for FunctionTool.
     */
    public static class Builder {
        private String name;
        private String description = "";
        private Method method;
        private Object instance;
        private Map<String, ParameterInfo> parameterInfos = new HashMap<>();
        private ToolInvoker invoker;
        private Map<String, Object> schemaOverride;
        private Map<String, Object> parametersOverride;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            if (this.name == null || this.name.isEmpty()) {
                this.name = method.getName();
            }
            extractParameterInfos();
            return this;
        }

        public Builder instance(Object instance) {
            this.instance = instance;
            return this;
        }

        /**
         * Sets a custom invoker for tool execution.
         */
        public Builder invoker(ToolInvoker invoker) {
            this.invoker = invoker;
            return this;
        }

        /**
         * Overrides the tool schema for external tools.
         */
        public Builder schema(Map<String, Object> schemaOverride) {
            this.schemaOverride = schemaOverride;
            return this;
        }

        /**
         * Overrides only the parameters part of the schema.
         * Use this when you want to customize parameters without providing a full schema.
         */
        public Builder parameters(Map<String, Object> parametersOverride) {
            this.parametersOverride = parametersOverride;
            return this;
        }

        private void extractParameterInfos() {
            parameterInfos = new HashMap<>();
            for (Parameter param : method.getParameters()) {
                String paramName = param.getName();
                String paramType = inferType(param.getType());
                String description = "";
                boolean required = true;
                String defaultValue = "";

                ToolParam toolParamAnnotation = param.getAnnotation(ToolParam.class);
                if (toolParamAnnotation != null) {
                    description = toolParamAnnotation.description();
                    required = toolParamAnnotation.required();
                    defaultValue = toolParamAnnotation.defaultValue();
                }

                parameterInfos.put(paramName,
                        new ParameterInfo(paramName, paramType, description, required, defaultValue));
            }
        }

        private String inferType(Class<?> type) {
            if (type == String.class) return "string";
            if (type == int.class || type == Integer.class) return "integer";
            if (type == long.class || type == Long.class) return "integer";
            if (type == double.class || type == Double.class) return "number";
            if (type == boolean.class || type == Boolean.class) return "boolean";
            if (type.isArray()) return "array";
            if (java.util.List.class.isAssignableFrom(type)) return "array";
            if (Map.class.isAssignableFrom(type)) return "object";
            return "string";
        }

        public FunctionTool build() {
            if (invoker == null) {
                Objects.requireNonNull(method, "Method is required");
                Objects.requireNonNull(instance, "Instance is required");
                if (name == null || name.isEmpty()) {
                    name = method.getName();
                }
            } else {
                Objects.requireNonNull(name, "Name is required");
            }
            return new FunctionTool(this);
        }
    }

    /**
     * Exception for tool execution errors.
     */
    public static class ToolExecutionException extends RuntimeException {
        public ToolExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
