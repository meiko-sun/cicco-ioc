package lol.cicco.ioc.core.module.initialize;

import lol.cicco.ioc.core.CiccoContext;
import lol.cicco.ioc.core.CiccoModule;
import lol.cicco.ioc.core.module.beans.BeanModule;
import lol.cicco.ioc.core.module.beans.BeanRegistry;
import lol.cicco.ioc.core.module.binder.BinderModule;
import lol.cicco.ioc.core.module.inject.InjectModule;
import lol.cicco.ioc.core.module.register.RegisterModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
public class InitializeBeanModule implements CiccoModule<Void> {
    public static final String INIT_MODULE_NAME = "_initializeBeanModule";

    @Override
    public void initModule(CiccoContext context) {
        BeanRegistry beanRegistry = (BeanModule) context.getModule(BeanModule.BEAN_MODULE_NAME).getModuleProcessor();

        Set<String> beanNames = beanRegistry.getRegisterBeans();
        for(String bean : beanNames) {
            beanRegistry.getNullableBean(bean).initialize();
        }
        log.debug("init initialize bean module....");
    }

    @Override
    public String getModuleName() {
        return INIT_MODULE_NAME;
    }

    @Override
    public Void getModuleProcessor() {
        return null;
    }

    @Override
    public List<String> dependModule() {
        return Arrays.asList(BeanModule.BEAN_MODULE_NAME, RegisterModule.REGISTER_MODULE_NAME, BinderModule.BINDER_MODULE_NAME, InjectModule.INJECT_MODULE_NAME);
    }

    @Override
    public List<String> afterModule() {
        return null;
    }
}
