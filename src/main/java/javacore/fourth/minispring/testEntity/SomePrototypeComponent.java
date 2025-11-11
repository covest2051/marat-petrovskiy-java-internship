package javacore.fourth.minispring.testEntity;

import javacore.fourth.minispring.beans.factory.annotation.Autowired;
import javacore.fourth.minispring.beans.factory.annotation.Scope;
import javacore.fourth.minispring.beans.factory.stereotype.Component;

@Component
@Scope("prototype")
public class SomePrototypeComponent {
    @Autowired
    private BeanToInjectInComponent beanToInjectInComponent;

    public BeanToInjectInComponent getBeanToInjectInComponent() {
        return beanToInjectInComponent;
    }

    public void setBeanToInjectInComponent(BeanToInjectInComponent beanToInjectInComponent) {
        this.beanToInjectInComponent = beanToInjectInComponent;
    }
}
