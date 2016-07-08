package name.kevinross.tool;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invoker interface and subinterfaces.
 */
public interface IInvoker {
    IInvoker name(final String name);
    IInvoker on(final Object obj);
    IInvoker on(final Class<?> clz);
    IInvoker of(final Class<?>... classes);
    IInvoker using(final Object... params);
    IInvoker method(final Method method);
    IInvoker field(final Field field);
    <T> T getNewInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException;
    <T> T get() throws NoSuchFieldException, SecurityException, IllegalAccessException, InvocationTargetException;
    void set() throws NoSuchFieldException, SecurityException, IllegalAccessException, InvocationTargetException;
    <T> T invoke() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException;
    IInvoker.PrivateInterface nosy();
    IInvoker.SwallowInterface swallow();
    interface PrivateInterface extends IInvoker {
        <T> T getNewInstance() throws NoSuchMethodException, InvocationTargetException, InstantiationException;
        <T> T get() throws NoSuchFieldException, InvocationTargetException;
        void set() throws NoSuchFieldException, InvocationTargetException;
        <T> T invoke() throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException;
    }
    interface SwallowInterface extends IInvoker {
        <T> T getNewInstance();
        <T> T get();
        void set();
        <T> T invoke();
    }
}
