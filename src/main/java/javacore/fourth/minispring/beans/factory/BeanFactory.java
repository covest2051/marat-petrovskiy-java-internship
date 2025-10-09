package javacore.fourth.minispring.beans.factory;

import javacore.fourth.minispring.beans.factory.annotation.Autowired;
import javacore.fourth.minispring.beans.factory.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BeanFactory {
    private Map<Class<?>, Object> singletons = new HashMap();
    private final Map<Class<?>, String> beanScopes = new HashMap<>();

    public <T> T getBean(Class<T> beanClass) {
        String scope = beanScopes.getOrDefault(beanClass, "singleton");

        try {
            if (scope.equals("singleton")) {
                return (T) singletons.get(beanClass);
            } else if (scope.equals("prototype")) {
                T instance = beanClass.getDeclaredConstructor().newInstance();
                for (Field field : instance.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        for (Map.Entry<Class<?>, Object> entry : singletons.entrySet()) {
                            Class<?> dependencyClass = entry.getKey();
                            Object dependencyInstance = entry.getValue();

                            if (field.getType().isAssignableFrom(dependencyClass)) {
                                String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                                Method setter = instance.getClass().getMethod(setterName, dependencyClass);
                                setter.invoke(instance, dependencyInstance);
                            }
                        }
                    }
                }
                return instance;
            } else {
                throw new IllegalStateException("Unknown scope: " + scope);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void instantiate(String basePackage) throws IOException, URISyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            File file = new File(resource.toURI());
            for(File classFile : Objects.requireNonNull(file.listFiles())){
                String fileName = classFile.getName();

                if(fileName.endsWith(".class")){
                    String className = fileName.substring(0, fileName.lastIndexOf("."));

                    Class<?> classObject = Class.forName(basePackage + "." + className);

                    if(classObject.isAnnotationPresent(Component.class)){
                        System.out.println("Component: " + classObject);

                        String scope = "singleton";
                        if (classObject.isAnnotationPresent(javacore.fourth.minispring.beans.factory.annotation.Scope.class)) {
                            scope = classObject.getAnnotation(javacore.fourth.minispring.beans.factory.annotation.Scope.class).value();
                        }
                        beanScopes.put(classObject, scope);

                        if (scope.equals("singleton")) {
                            Object instance = classObject.getDeclaredConstructor().newInstance();
                            singletons.put(classObject, instance);
                        }
                    }
                }

            }
        }
    }

    public void populateProperties() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        System.out.println("==populateProperties==");

        for (Object object : singletons.values()) {
            for (Field field : object.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    for (Map.Entry<Class<?>, Object> entry : singletons.entrySet()) {
                        Class<?> dependencyClass = entry.getKey();
                        Object dependencyInstance = entry.getValue();

                        if (field.getType().isAssignableFrom(dependencyClass)) {
                            String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                            Method setter = object.getClass().getMethod(setterName, dependencyClass);
                            setter.invoke(object, dependencyInstance);
                        }
                    }
                }
            }
        }
    }

    public void initializeBeans(){
        for (Object bean : singletons.values()) {
            if(bean instanceof InitializingBean){
                ((InitializingBean) bean).afterPropertiesSet();
            }
        }
    }
}
