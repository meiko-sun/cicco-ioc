package lol.cicco.ioc.core.aop;

import javassist.*;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

final class JavassistEnhance {
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();
    private static CtClass PROCESSOR_CLS;

    private static final String PROCESSOR_FIELD_NAME = "_processor";
    private static final String METHOD_MAP_FIELD_NAME = "_methodMap";

    private static final String JOIN_POINT_CLASS = JoinPointImpl.class.getName();
    private static final String INTERCEPTOR_CLASS = Interceptor.class.getName();

    static {
        try {
            PROCESSOR_CLS = CLASS_POOL.get(AopProcessor.class.getName());
        } catch (NotFoundException ignore) {
        }
    }

    @SneakyThrows
    static Object beanEnhance(String originClsName, Map<Method, List<String>> interceptors) {
        CtClass originCls = CLASS_POOL.get(originClsName);
        // 生成子类
        CtClass targetCls = makeProxyClass(originCls);

        for (Method method : interceptors.keySet()) {
            addProxyMethod(originCls, targetCls, method, interceptors.get(method));
        }
        return makeInstance(targetCls);
    }


    @SneakyThrows
    private static void addProxyMethod(CtClass originCls, CtClass targetCls, Method method, List<String> hasInters) {
        CtMethod originMethod = originCls.getDeclaredMethod(method.getName());
        CtMethod proxyMethod = new CtMethod(originMethod.getReturnType(), method.getName(), originMethod.getParameterTypes(), targetCls);

        // 生成方法体
        StringBuilder methodBody = new StringBuilder("{");

        methodBody.append(JOIN_POINT_CLASS).append(" joinPoint = new ").append(JOIN_POINT_CLASS).append("(").append("$0,").append("(java.lang.reflect.Method)getMethod(\"").append(method.toGenericString()).append("\"),").append("$args").append(");");
        // 调用before()
        for (String s : hasInters) {
            methodBody.append(INTERCEPTOR_CLASS).append(" inter = ").append(PROCESSOR_FIELD_NAME).append(".getInterceptor(\"").append(s).append("\");");
            methodBody.append("inter.before(joinPoint);");
        }
        // 执行
        if(!method.getReturnType().equals(Void.TYPE)) {
            methodBody.append("Object result = super.").append(method.getName()).append("($$);");
        } else {
            methodBody.append("Object result = null;");
            methodBody.append("super.").append(method.getName()).append("($$);");
        }

        methodBody.append("joinPoint.setReturnValue(result);");
        // 调用after()
        for (String s : hasInters) {
            methodBody.append(INTERCEPTOR_CLASS).append(" inter = ").append(PROCESSOR_FIELD_NAME).append(".getInterceptor(\"").append(s).append("\");");
            methodBody.append("inter.after(joinPoint);");
        }
        methodBody.append("return result;");
        methodBody.append("}");

        proxyMethod.setBody(methodBody.toString());
        proxyMethod.setModifiers(originMethod.getModifiers());

        targetCls.addMethod(proxyMethod);
    }

    @SneakyThrows
    private static Object makeInstance(CtClass targetCls) {
        // 生成对应类
        Class<?> target = targetCls.toClass();

        // 从ClassPool移除生成类
        targetCls.detach();
        return target.getConstructor().newInstance();
    }

    @SneakyThrows
    private static CtClass makeProxyClass(CtClass originCls) {

        CtClass targetCls = CLASS_POOL.makeClass(originCls.getName() + "$Child");
        targetCls.setSuperclass(originCls);  // 设置原始类型为父类
        targetCls.addInterface(CLASS_POOL.get(BeanProxy.class.getName()));

        CtField processorField = new CtField(PROCESSOR_CLS, PROCESSOR_FIELD_NAME, targetCls);
        processorField.setModifiers(Modifier.PRIVATE | Modifier.TRANSIENT);
        targetCls.addField(processorField);

        CtField methodField = new CtField(CLASS_POOL.get(Map.class.getName()), METHOD_MAP_FIELD_NAME, targetCls);
        methodField.setModifiers(Modifier.PRIVATE | Modifier.TRANSIENT);
        targetCls.addField(methodField);

        CtMethod setProcessor = new CtMethod(CtClass.voidType, "setProcessor", new CtClass[]{PROCESSOR_CLS}, targetCls);
        // 设置生成类模板
        setProcessor.setBody("{" + PROCESSOR_FIELD_NAME + " = $1;}");
        setProcessor.setModifiers(Modifier.PUBLIC);
        targetCls.addMethod(setProcessor);

        CtMethod putMethod = new CtMethod(CtClass.voidType, "putMethod", new CtClass[]{CLASS_POOL.get(String.class.getName()), CLASS_POOL.get(Method.class.getName())}, targetCls);
        putMethod.setBody("{" + METHOD_MAP_FIELD_NAME + ".put($1,$2);}");
        putMethod.setModifiers(Modifier.PUBLIC);
        targetCls.addMethod(putMethod);

        CtMethod getMethod = new CtMethod(CLASS_POOL.get(Object.class.getName()), "getMethod", new CtClass[]{CLASS_POOL.get(String.class.getName())}, targetCls);
        getMethod.setBody("{return " + METHOD_MAP_FIELD_NAME + ".get($1);}");
        getMethod.setModifiers(Modifier.PUBLIC);
        targetCls.addMethod(getMethod);

        CtConstructor defConst = new CtConstructor(new CtClass[]{}, targetCls);
        defConst.setBody("{"+METHOD_MAP_FIELD_NAME+"=new java.util.HashMap();}");
        targetCls.addConstructor(defConst);
        return targetCls;
    }
}
