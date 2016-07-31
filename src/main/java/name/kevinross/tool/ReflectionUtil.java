package name.kevinross.tool;

/*
 * sourced from https://gist.github.com/josefbetancourt/3ffcb3044e558fc1b3e8
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection Invoker using Fluent Builder.
 *
 * ReflectionUtil is a singleton utility class that contains
 * a facade to a fluent builder pattern.
 *
 * @author jbetancourt
 * @since 20130201T2233-5
 *
 * @author kevinross
 * @since 20160629T2133-5
 */
public final class ReflectionUtil {

    private static final ReflectionUtil instance = new ReflectionUtil();

    private ReflectionUtil() {
        // it's a singleton utility class.
    }

    /**
     * Get a class given a fully-qualified class name
     *
     * @param classLoader the loader to find the class in
     * @param name fully qualified class name
     * @return the found class
     * @throws ClassNotFoundException
     */
    public static <T> Class<T> getClassByName(ClassLoader classLoader, String name) {
        try {
            return (Class<T>)classLoader.loadClass(name);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("couldn't get class");
        }
    }

    /**
     * Get an inner class of a given class by name (excluding "EnclosingClass$"
     * @param cls enclosing class
     * @param name inner class name
     * @return null|Class
     */
    public static Class getInnerClass(Class cls, String name) {
        for (Class c : cls.getDeclaredClasses()) {
            if (c.getSimpleName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Return an array of types corresponding to the array of objects passed in
     * @param args
     * @return
     */
    public static Class[] paramsToTypes(Object[] args) {
        if (args == null) {
            return null;
        }
        List<Class> params = new ArrayList<>();
        for (Object o : args) {
            params.add(o.getClass());
        }
        return params.toArray(new Class[params.size()]);
    }

    /**
     * Get the building started
     * @return builder
     */
    public static IInvoker invokes() {
        return new Invoker();
    }

    /**
     * ReflectionUtil use:
     * invokes().name("hello").on(innerTest).using("Hello world!").of(String.class).invoke();
     *
     */
    static class Invoker implements IInvoker {
        protected Invoker invoker;
        private String name;
        private Object object;
        private Class<? extends Object> clazz;
        private Class<?>[] classes;
        private Object[] params;
        private Method method;
        private Constructor constructor;
        private Field field;

        private Invoker() {

        }

        /**
         * Specialized invokers allow for altering the logic while keeping the building going
         * @param parent
         */
        private Invoker(Invoker parent) {
            invoker = parent;
        }

        /**
         * Field/method name
         *
         * @param name
         * @return
         */
        public Invoker name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Object to invoke on
         * @param obj
         * @return
         */
        public Invoker on(final Object obj) {
            object = obj;
            return this;
        }

        /**
         * Class to invoke on
         * @param clz
         * @return
         */
        public Invoker on(final Class<?> clz) {
            clazz = clz;
            return this;
        }

        /**
         * Parameter types for method/constructor
         * @param classes
         * @return
         */
        public Invoker of(final Class<?>... classes) {
            this.classes = classes;
            return this;
        }

        /**
         * Parameter values for method/constructor
         * @param params
         * @return
         */
        public Invoker using(final Object... params) {
            this.params = params;
            return this;
        }

        /**
         * Pass in the actual method instead of finding it
         * @param method
         * @return
         */
        public Invoker method(final Method method) {
            this.method = method;
            return this;
        }

        /**
         * Pass in the actual constructor instead of inferring it from parameter types
         * @param constructor
         * @return
         */
        public Invoker constructor(final Constructor constructor) {
            this.constructor = constructor;
            return this;
        }

        /**
         * Pass in the actual field instead of finding it
         * @param field
         * @return
         */
        public Invoker field(final Field field) {
            this.field = field;
            return this;
        }

        /**
         * Call the constructor
         * @param <T> returning an object of type T
         * @return instance of the class
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         * @throws InstantiationException
         */
        public <T> T getNewInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            return (T)getConstructor().newInstance(params);
        }

        /**
         * Get the value of a field
         * @param <T> returning an object of type T
         * @return value
         * @throws NoSuchFieldException
         * @throws SecurityException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public <T> T get() throws NoSuchFieldException, SecurityException, IllegalAccessException, InvocationTargetException {
            return (T)getField().get(object);
        }

        /**
         * Set a value on the object
         * @throws NoSuchFieldException
         * @throws SecurityException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public void set() throws NoSuchFieldException, SecurityException, IllegalAccessException, InvocationTargetException {
            getField().set(object, params[0]);
        }

        /**
         * Invoke a method on the object
         * @param <T> returning an object of type T
         * @return Void|T-type object
         * @throws NoSuchMethodException
         * @throws SecurityException
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         * @throws InvocationTargetException
         */
        public <T> T invoke() throws NoSuchMethodException, SecurityException,
                IllegalAccessException, IllegalArgumentException,
                InvocationTargetException {

            boolean paramsButNoTypes = (method == null)
                    && ((params != null) && (classes == null));

            if (paramsButNoTypes) {
                throw new IllegalStateException(String.format(
                        "params:%s,classes:%s", params, classes));
            }

            return (T)getMethod().invoke(object, params);

        }


        /**
         * Extend the invoker to allow accessing non-public members
         * @return
         */
        public IInvoker.PrivateInterface nosy() {
            return new InvokerPrivate(this);
        }

        /**
         * Extend the invoker to swallow reflection-related exceptions
         * @return
         */
        public IInvoker.SwallowInterface swallow() {
            return new InvokerNoChecked(this);
        }

        // accessor methods for subclasses
        protected String getName() {
            if (name == null) {
                if (field != null) {
                    return field.getName();
                } else if (method != null) {
                    return method.getName();
                } else {
                    throw new IllegalStateException("nothing defined so no name can be found");
                }
            }
            return name;
        }

        protected Object getObject() {
            return object;
        }

        protected Class<? extends Object> getClazz() {
            if (clazz == null && object != null) {
                return object.getClass();
            }
            return clazz;
        }

        protected Class<?>[] getClasses() {
            return classes;
        }

        protected Object[] getParams() {
            return params;
        }

        protected Constructor getConstructor() throws NoSuchMethodException {
            if (constructor == null) {
                return findConstructor(getClazz(), getClasses());
            }
            return constructor;
        }

        protected Method getMethod() throws NoSuchMethodException {
            if (method == null && name != null) {
                return findMethod(getClazz(), name, getClasses());
            }
            return method;
        }

        protected Field getField() throws NoSuchFieldException {
            if (field == null && name != null) {
                return findField(getClazz(), name);
            }
            return field;
        }

        /**
         * Find a field in the class hierarchy
         * @param clazz which class to start the search in
         * @param name the name of the field
         * @return the field object
         * @throws NoSuchFieldException
         */
        protected static Field findField(Class clazz, String name) throws NoSuchFieldException {
            Class thisClass = clazz;
            NoSuchFieldException ex = null;
            while (thisClass != Object.class) {
                try {
                    return thisClass.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    ex = e;
                    thisClass = thisClass.getSuperclass();
                }
            }
            throw ex;
        }
        /**
         * Find a method in the class hierarchy
         * @param clazz which class to start the search in
         * @param name the name of the method
         * @param pTypes the parameter types of the method
         * @return the method object
         * @throws NoSuchMethodException
         */
        protected static Method findMethod(Class clazz, String name, Class[] pTypes) throws NoSuchMethodException {
            Class thisClass = clazz;
            NoSuchMethodException ex = null;
            while (thisClass != Object.class) {
                try {
                    return thisClass.getDeclaredMethod(name, pTypes);
                } catch (NoSuchMethodException e) {
                    ex = e;
                    thisClass = thisClass.getSuperclass();
                }
            }
            throw ex;
        }
        /**
         * Find a constructor in the class hierarchy
         * @param clazz which class to start the search in
         * @param pTypes the parameter types of the constructor
         * @return the constructor object
         * @throws NoSuchMethodException
         */
        protected static Constructor findConstructor(Class clazz, Class[] pTypes) throws NoSuchMethodException {
            Class thisClass = clazz;
            NoSuchMethodException ex = null;
            while (thisClass != Object.class) {
                try {
                    if (pTypes != null && pTypes.length == 0) {
                        return thisClass.getDeclaredConstructor();
                    }
                    return thisClass.getDeclaredConstructor(pTypes);
                } catch (NoSuchMethodException e) {
                    ex = e;
                    thisClass = thisClass.getSuperclass();
                }
            }
            throw ex;
        }

    } // end class Invoker

    /**
     * An extension of Invoker that calls setAccessible on components before using them
     */
    static class InvokerPrivate extends Invoker implements IInvoker.PrivateInterface {
        private InvokerPrivate(Invoker parent) {
            super(parent);
            invoker = parent;
        }

        /**
         * {@inheritDoc}
         */
        public <T> T get() {
            try {
                final Object objF = getObject();
                final Field fieldF = getField() == null ? findField(objF.getClass(), getName()) : getField();

                return (T) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    Object result;
                    @Override
                    public Object run() throws Exception {
                        if (!fieldF.isAccessible()) {
                            fieldF.setAccessible(true);
                        }
                        return invoker.field(fieldF).get();
//                        result = fieldF.get(objF);
//                        return result;
                    }
                });
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot set field,'" + getName()
                        + "' accessible.", ex);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void set() {
            try {
                //final Object objF = getObject();
                final Field fieldF = getField(); // == null ? findField(objF.getClass(), getName()) : getField();
                //final Object valF = getParams()[0];

                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        if (!fieldF.isAccessible()) {
                            fieldF.setAccessible(true);
                        }
                        invoker.field(fieldF).set();
                        //fieldF.set(objF, valF);
                        //return null;
                        return null;
                    }
                });
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot set field,'" + getName()
                        + "' accessible.", ex);
            }
        }

        /**
         * {@inheritDoc}
         */
        public <T> T getNewInstance() throws NoSuchMethodException {
            try {
                final Constructor ctor = getClazz().getDeclaredConstructor(getClasses());
                //final Object[] paramsF = getParams();
                return (T)AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    Object result;
                    @Override
                    public Object run() throws Exception {
                        if (!ctor.isAccessible()) {
                            ctor.setAccessible(true);
                        }
                        return invoker.constructor(ctor).getNewInstance();
                        //return InvokerPrivate.super.getNewInstance();
                        //result = ctor.newInstance(paramsF);
                        //return result;
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IllegalStateException("Couldn't invoke ctor");
            }
        }

        /**
         * {@inheritDoc}
         */
        public <T> T invoke() {
            try {
                //final Object objF = getObject();
                final Method methodF = getMethod();
                //final Object[] paramsF = getParams();

                return (T)AccessController
                        .doPrivileged(new PrivilegedExceptionAction<Object>() {
                            Object result;

                            @Override
                            public Object run() throws Exception {
                                if (!methodF.isAccessible()) {
                                    methodF.setAccessible(true);
                                }
                                return invoker.method(methodF).invoke();
//                                result = methodF.invoke(objF, paramsF);
//                                return result;
                            }

                        });
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot set method,'" + getName()
                        + "' accessible.", ex);
            }
        }

        @Override
        protected String getName() {
            return invoker.getName();
        }

        @Override
        protected Object getObject() {
            return invoker.getObject();
        }

        @Override
        protected Class<? extends Object> getClazz() {
            return invoker.getClazz();
        }

        @Override
        protected Class<?>[] getClasses() {
            return invoker.getClasses();
        }

        @Override
        protected Object[] getParams() {
            return invoker.getParams();
        }

        @Override
        protected Method getMethod() throws NoSuchMethodException {
            return invoker.getMethod();
        }

        @Override
        protected Field getField() throws NoSuchFieldException {
            return invoker.getField();
        }
    }

    /**
     * An extension of Invoker that swallows all reflection-related exceptions
     */
    static class InvokerNoChecked extends Invoker implements IInvoker.SwallowInterface {
        private Invoker invoker;
        private InvokerNoChecked(Invoker parent) {
            super(parent);
            invoker = parent;
        }

        /**
         * {@inheritDoc}
         */
        public <T> T getNewInstance() {
            try {
                return invoker.getNewInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("couldn't get new instance");
            }
        }

        /**
         * {@inheritDoc}
         */
        public <T> T invoke() {
            try {
                return invoker.invoke();
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("couldn't invoke");
            }
        }

        /**
         * {@inheritDoc}
         */
        public <T> T get() {
            try {
                return invoker.get();
            } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("couldn't get");
            }
        }

        /**
         * {@inheritDoc}
         */
        public void set() {
            try {
                invoker.set();
            } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("couldn't set");
            }
        }

        @Override
        protected String getName() {
            return invoker.getName();
        }

        @Override
        protected Object getObject() {
            return invoker.getObject();
        }

        @Override
        protected Class<? extends Object> getClazz() {
            return invoker.getClazz();
        }

        @Override
        protected Class<?>[] getClasses() {
            return invoker.getClasses();
        }

        @Override
        protected Object[] getParams() {
            return invoker.getParams();
        }

        @Override
        protected Method getMethod() throws NoSuchMethodException {
            return invoker.getMethod();
        }

        @Override
        protected Field getField() throws NoSuchFieldException {
            return invoker.getField();
        }
    }

} // end class ReflectionUtil