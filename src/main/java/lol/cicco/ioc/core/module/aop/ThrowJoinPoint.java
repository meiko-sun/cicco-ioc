package lol.cicco.ioc.core.module.aop;

/**
 * Interceptor.throwException参数
 */
public interface ThrowJoinPoint extends BeforeJoinPoint{

    /**
     * 当前方法抛出的异常信息
     */
    Throwable getThrowable();
}
