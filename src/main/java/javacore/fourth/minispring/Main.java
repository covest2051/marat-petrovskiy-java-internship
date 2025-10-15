package javacore.fourth.minispring;

import javacore.fourth.minispring.beans.factory.BeanFactory;
import javacore.fourth.minispring.testEntity.SomePrototypeComponent;
import javacore.fourth.minispring.testEntity.SomeSingletonComponent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.instantiate("javacore.fourth.minispring.testEntity");
        beanFactory.populateProperties();

        SomeSingletonComponent someSingletonComponentComponent1 = beanFactory.getBean(SomeSingletonComponent.class);
        SomeSingletonComponent someSingletonComponentComponent2 = beanFactory.getBean(SomeSingletonComponent.class);
        SomePrototypeComponent somePrototypeComponent1 = beanFactory.getBean(SomePrototypeComponent.class);
        SomePrototypeComponent somePrototypeComponent2 = beanFactory.getBean(SomePrototypeComponent.class);

        beanFactory.initializeBeans();

        System.out.println(someSingletonComponentComponent1);
        System.out.println(someSingletonComponentComponent1.getBeanToInjectInComponent());

        System.out.println(somePrototypeComponent1);
        System.out.println(somePrototypeComponent1.getBeanToInjectInComponent());

        // Наглядная демонстрация скоупа :)
        System.out.println(someSingletonComponentComponent1 == someSingletonComponentComponent2);
        System.out.println(somePrototypeComponent1 == somePrototypeComponent2);
    }
}
