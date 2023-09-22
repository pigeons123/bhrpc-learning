/**
 * Copyright 2020-9999 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.binghe.rpc.consumer.spring;

import io.binghe.rpc.annotation.RpcReference;
import io.binghe.rpc.common.utils.StringUtils;
import io.binghe.rpc.constants.RpcConstants;
import io.binghe.rpc.consumer.spring.context.RpcConsumerSpringContext;
import io.binghe.rpc.consumer.yaml.config.ConsumerYamlConfig;
import io.binghe.rpc.consumer.yaml.factory.ConsumerYamlConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author binghe(公众号：冰河技术)
 * @version 1.0.0
 * @description RpcConsumerPostProcessor
 */
@Component
public class RpcConsumerPostProcessor implements ApplicationContextAware, BeanClassLoaderAware, BeanFactoryPostProcessor {
    private final Logger logger = LoggerFactory.getLogger(RpcConsumerPostProcessor.class);
    private ApplicationContext context;
    private ClassLoader classLoader;

    private final Map<String, BeanDefinition> rpcRefBeanDefinitions = new LinkedHashMap<>();

    //获取配置文件的数据
    private ConsumerYamlConfig consumerYamlConfig = ConsumerYamlConfigFactory.getConcumerYamlConfig();

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        RpcConsumerSpringContext.getInstance().setContext(applicationContext);
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName != null) {
                Class<?> clazz = ClassUtils.resolveClassName(beanClassName, this.classLoader);
                ReflectionUtils.doWithFields(clazz, this::parseRpcReference);
            }
        }

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        this.rpcRefBeanDefinitions.forEach((beanName, beanDefinition) -> {
            if (context.containsBean(beanName)) {
                throw new IllegalArgumentException("spring context already has a bean named " + beanName);
            }
            registry.registerBeanDefinition(beanName, rpcRefBeanDefinitions.get(beanName));
            logger.info("registered RpcReferenceBean {} success.", beanName);
        });
    }

    private void parseRpcReference(Field field) {
        RpcReference annotation = AnnotationUtils.getAnnotation(field, RpcReference.class);
        if (annotation != null) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RpcReferenceBean.class);
            builder.setInitMethodName(RpcConstants.INIT_METHOD_NAME);
            builder.addPropertyValue("interfaceClass", field.getType());
            builder.addPropertyValue("version", this.getVersion(annotation.version()));
            builder.addPropertyValue("registryType", this.getRegistryType(annotation.registryType()));
            builder.addPropertyValue("registryAddress", this.getRegistryAddress(annotation.registryAddress()));
            builder.addPropertyValue("loadBalanceType", this.getLoadBalanceType(annotation.loadBalanceType()));
            builder.addPropertyValue("serializationType", this.getSerializationType(annotation.serializationType()));
            builder.addPropertyValue("timeout", this.getTimeout(annotation.timeout()));
            builder.addPropertyValue("async", this.getAsync(annotation.async()));
            builder.addPropertyValue("oneway", this.getOneway(annotation.oneway()));
            builder.addPropertyValue("proxy", this.getProxy(annotation.proxy()));
            builder.addPropertyValue("group", this.getGroup(annotation.group()));
            builder.addPropertyValue("scanNotActiveChannelInterval", this.getScanNotActiveChannelInterval(annotation.scanNotActiveChannelInterval()));
            builder.addPropertyValue("heartbeatInterval", this.getHeartbeatInterval(annotation.heartbeatInterval()));
            builder.addPropertyValue("retryInterval", this.getRetryInterval(annotation.retryInterval()));
            builder.addPropertyValue("retryTimes", this.getRetryTimes(annotation.retryTimes()));
            builder.addPropertyValue("enableResultCache", this.getEnableResultCache(annotation.enableResultCache()));
            builder.addPropertyValue("resultCacheExpire", this.getResultCacheExpire(annotation.resultCacheExpire()));
            builder.addPropertyValue("enableDirectServer", this.getEnableDirectServer(annotation.enableDirectServer()));
            builder.addPropertyValue("directServerUrl", this.getDirectServerUrl(annotation.directServerUrl()));
            builder.addPropertyValue("enableDelayConnection", this.getEnableDelayConnection(annotation.enableDelayConnection()));
            builder.addPropertyValue("corePoolSize", this.getCorePoolSize(annotation.corePoolSize()));
            builder.addPropertyValue("maximumPoolSize", this.getMaximumPoolSize(annotation.maximumPoolSize()));
            builder.addPropertyValue("flowType", this.getFlowType(annotation.flowType()));
            builder.addPropertyValue("enableBuffer", this.getEnableBuffer(annotation.enableBuffer()));
            builder.addPropertyValue("bufferSize", this.getBufferSize(annotation.bufferSize()));
            builder.addPropertyValue("reflectType", this.getReflectType(annotation.reflectType()));
            builder.addPropertyValue("fallbackClass", annotation.fallbackClass());
            builder.addPropertyValue("fallbackClassName", this.getFallbackClassName(annotation.fallbackClassName()));
            builder.addPropertyValue("enableRateLimiter", this.getEnableRateLimiter(annotation.enableRateLimiter()));
            builder.addPropertyValue("rateLimiterType", this.getRateLimiterType(annotation.rateLimiterType()));
            builder.addPropertyValue("permits", this.getPermits(annotation.permits()));
            builder.addPropertyValue("milliSeconds", this.getMilliSeconds(annotation.milliSeconds()));
            builder.addPropertyValue("rateLimiterFailStrategy", this.getRateLimiterFailStrategy(annotation.rateLimiterFailStrategy()));
            builder.addPropertyValue("enableFusing", this.getEnableFusing(annotation.enableFusing()));
            builder.addPropertyValue("fusingType", this.getFusingType(annotation.fusingType()));
            builder.addPropertyValue("totalFailure", this.getTotalFailure(annotation.totalFailure()));
            builder.addPropertyValue("fusingMilliSeconds", this.getFusingMilliSeconds(annotation.fusingMilliSeconds()));
            builder.addPropertyValue("exceptionPostProcessorType", this.getExceptionPostProcessorType(annotation.exceptionPostProcessorType()));

            BeanDefinition beanDefinition = builder.getBeanDefinition();
            rpcRefBeanDefinitions.put(field.getName(), beanDefinition);
        }
    }

    private String getExceptionPostProcessorType(String exceptionPostProcessorType){
        if (StringUtils.isEmpty(exceptionPostProcessorType)
                || (RpcConstants.EXCEPTION_POST_PROCESSOR_PRINT.equals(exceptionPostProcessorType) && !StringUtils.isEmpty(consumerYamlConfig.getExceptionPostProcessorType()))){
            exceptionPostProcessorType = consumerYamlConfig.getExceptionPostProcessorType();
        }
        return exceptionPostProcessorType;
    }

    private int getFusingMilliSeconds(int fusingMilliSeconds){
        if (fusingMilliSeconds <= 0
                || (RpcConstants.DEFAULT_FUSING_MILLI_SECONDS == fusingMilliSeconds && consumerYamlConfig.getFusingMilliSeconds() > 0)){
            fusingMilliSeconds = consumerYamlConfig.getFusingMilliSeconds();
        }
        return fusingMilliSeconds;
    }

    private double getTotalFailure(double totalFailure){
        if (totalFailure<= 0
                || (RpcConstants.DEFAULT_FUSING_TOTAL_FAILURE == totalFailure && consumerYamlConfig.getTotalFailure() > 0 )){
            totalFailure = consumerYamlConfig.getTotalFailure();
        }
        return totalFailure;
    }

    private String getFusingType(String fusingType){
        if (StringUtils.isEmpty(fusingType)
                || (RpcConstants.DEFAULT_FUSING_INVOKER.equals(fusingType) && !StringUtils.isEmpty(consumerYamlConfig.getFusingType()))){
           fusingType = consumerYamlConfig.getFusingType();
        }
        return fusingType;
    }

    private boolean getEnableFusing(boolean enableFusing){
        if (!enableFusing){
            enableFusing = consumerYamlConfig.getEnableFusing();
        }
        return enableFusing;
    }

    private String getRateLimiterFailStrategy(String rateLimiterFailStrategy){
        if (StringUtils.isEmpty(rateLimiterFailStrategy)
                || (RpcConstants.RATE_LIMILTER_FAIL_STRATEGY_DIRECT.equals(rateLimiterFailStrategy) && !StringUtils.isEmpty(consumerYamlConfig.getRateLimiterFailStrategy()))){
            rateLimiterFailStrategy = consumerYamlConfig.getRateLimiterFailStrategy();
        }
        return rateLimiterFailStrategy;
    }

    private int getMilliSeconds(int milliSeconds){
        if (milliSeconds <= 0
                || (RpcConstants.DEFAULT_RATELIMITER_MILLI_SECONDS == milliSeconds && consumerYamlConfig.getMilliSeconds() > 0)){
            milliSeconds = consumerYamlConfig.getMilliSeconds();
        }
        return milliSeconds;
    }

    private int getPermits(int permits){
        if (permits <= 0
                || (RpcConstants.DEFAULT_RATELIMITER_PERMITS == permits && consumerYamlConfig.getPermits() > 0)){
            permits = consumerYamlConfig.getPermits();
        }
        return permits;
    }

    private String getRateLimiterType(String rateLimiterType){
        if (StringUtils.isEmpty(rateLimiterType)
                || (RpcConstants.DEFAULT_RATELIMITER_INVOKER.equals(rateLimiterType) && !StringUtils.isEmpty(consumerYamlConfig.getRateLimiterType()))){
            rateLimiterType = consumerYamlConfig.getRateLimiterType();
        }
        return rateLimiterType;
    }

    private boolean getEnableRateLimiter(boolean enableRateLimiter){
        if (!enableRateLimiter){
            enableRateLimiter = consumerYamlConfig.getEnableRateLimiter();
        }
        return enableRateLimiter;
    }

    private String getFallbackClassName(String fallbackClassName){
        if (StringUtils.isEmpty(fallbackClassName)
                || (RpcConstants.DEFAULT_FALLBACK_CLASS_NAME.equals(fallbackClassName) && !StringUtils.isEmpty(consumerYamlConfig.getFallbackClassName()))){
            fallbackClassName = consumerYamlConfig.getFallbackClassName();
        }
        return fallbackClassName;
    }

    private String getReflectType(String reflectType){
        if (StringUtils.isEmpty(reflectType)
                || (RpcConstants.DEFAULT_REFLECT_TYPE.equals(reflectType) && !StringUtils.isEmpty(consumerYamlConfig.getReflectType()))){
            reflectType = consumerYamlConfig.getReflectType();
        }
        return reflectType;
    }

    private int getBufferSize(int bufferSize){
        if (bufferSize <= 0
                || (RpcConstants.DEFAULT_BUFFER_SIZE == bufferSize && consumerYamlConfig.getBufferSize() > 0)){
            bufferSize = consumerYamlConfig.getBufferSize();
        }
        return bufferSize;
    }

    private boolean getEnableBuffer(boolean enableBuffer){
        if (!enableBuffer){
            enableBuffer = consumerYamlConfig.getEnableBuffer();
        }
        return enableBuffer;
    }

    private String getFlowType(String flowType){
        if (StringUtils.isEmpty(flowType)
                || (RpcConstants.FLOW_POST_PROCESSOR_PRINT.equals(flowType) && !StringUtils.isEmpty(consumerYamlConfig.getFlowType()))){
            flowType = consumerYamlConfig.getFlowType();
        }
        return flowType;
    }

    private int getMaximumPoolSize(int maximumPoolSize){
        if (maximumPoolSize <= 0
                || (RpcConstants.DEFAULT_MAXI_NUM_POOL_SIZE == maximumPoolSize && consumerYamlConfig.getMaximumPoolSize() > 0)){
            maximumPoolSize = consumerYamlConfig.getMaximumPoolSize();
        }
        return maximumPoolSize;
    }

    private int getCorePoolSize(int corePoolSize){
        if (corePoolSize <= 0
                || (RpcConstants.DEFAULT_CORE_POOL_SIZE == corePoolSize && consumerYamlConfig.getCorePoolSize() > 0)){
            corePoolSize = consumerYamlConfig.getCorePoolSize();
        }
        return corePoolSize;
    }

    private boolean getEnableDelayConnection(boolean enableDelayConnection){
        if (!enableDelayConnection){
            enableDelayConnection = consumerYamlConfig.getEnableDelayConnection();
        }
        return enableDelayConnection;
    }

    private String getDirectServerUrl(String directServerUrl){
        if (StringUtils.isEmpty(directServerUrl)
                || (RpcConstants.RPC_COMMON_DEFAULT_DIRECT_SERVER.equals(directServerUrl) && !StringUtils.isEmpty(consumerYamlConfig.getDirectServerUrl()))){
            directServerUrl = consumerYamlConfig.getDirectServerUrl();
        }
        return directServerUrl;
    }

    private boolean getEnableDirectServer(boolean enableDirectServer){
        if (!enableDirectServer){
            enableDirectServer = consumerYamlConfig.getEnableDirectServer();
        }
        return enableDirectServer;
    }

    private int getResultCacheExpire(int resultCacheExpire){
        if (resultCacheExpire <= 0
                || (RpcConstants.RPC_SCAN_RESULT_CACHE_EXPIRE == resultCacheExpire && consumerYamlConfig.getResultCacheExpire() > 0)){
            resultCacheExpire = consumerYamlConfig.getResultCacheExpire();
        }
        return resultCacheExpire;
    }

    private boolean getEnableResultCache(boolean enableResultCache){
        if (!enableResultCache){
            enableResultCache = consumerYamlConfig.getEnableResultCache();
        }
        return enableResultCache;
    }

    private int getRetryTimes(int retryTimes){
        if (retryTimes <= 0 || (RpcConstants.RPC_REFERENCE_DEFAULT_RETRYTIMES == retryTimes && consumerYamlConfig.getRetryTimes() > 0)){
            retryTimes = consumerYamlConfig.getRetryTimes();
        }
        return retryTimes;
    }

    private int getRetryInterval(int retryInterval){
        if (retryInterval <= 0
                || (RpcConstants.RPC_REFERENCE_DEFAULT_RETRYINTERVAL == retryInterval && consumerYamlConfig.getRetryInterval() > 0)){
            retryInterval = consumerYamlConfig.getRetryInterval();
        }
        return retryInterval;
    }

    private int getHeartbeatInterval(int heartbeatInterval){
        if (heartbeatInterval <= 0
                || (RpcConstants.RPC_COMMON_DEFAULT_HEARTBEATINTERVAL == heartbeatInterval && consumerYamlConfig.getHeartbeatInterval() > 0 )){
            heartbeatInterval = consumerYamlConfig.getHeartbeatInterval();
        }
        return heartbeatInterval;
    }

    private int getScanNotActiveChannelInterval(int scanNotActiveChannelInterval){
        if (scanNotActiveChannelInterval <= 0
                || (RpcConstants.RPC_COMMON_DEFAULT_SCANNOTACTIVECHANNELINTERVAL == scanNotActiveChannelInterval && consumerYamlConfig.getScanNotActiveChannelInterval() > 0)){
            scanNotActiveChannelInterval = consumerYamlConfig.getScanNotActiveChannelInterval();
        }
        return scanNotActiveChannelInterval;
    }

    private String getGroup(String group){
        if (StringUtils.isEmpty(group)
                || (RpcConstants.RPC_COMMON_DEFAULT_GROUP.equals(group) && !StringUtils.isEmpty(consumerYamlConfig.getGroup()))){
            group = consumerYamlConfig.getGroup();
        }
        return group;
    }

    private String getProxy(String proxy){
        if (StringUtils.isEmpty(proxy)
                || (RpcConstants.RPC_REFERENCE_DEFAULT_PROXY.equals(proxy) && !StringUtils.isEmpty(consumerYamlConfig.getProxy()))){
            proxy = consumerYamlConfig.getProxy();
        }
        return proxy;
    }

    private String getVersion(String version){
        if (StringUtils.isEmpty(version) || (RpcConstants.RPC_COMMON_DEFAULT_VERSION.equals(version) && !StringUtils.isEmpty(consumerYamlConfig.getVersion()))){
            version = consumerYamlConfig.getVersion();
        }
        return version;
    }

    private String getRegistryType(String registryType){
        if (StringUtils.isEmpty(registryType) || (RpcConstants.RPC_REFERENCE_DEFAULT_REGISTRYTYPE.equals(registryType) && !StringUtils.isEmpty(consumerYamlConfig.getRegistryType()))){
            registryType = consumerYamlConfig.getRegistryType();
        }
        return registryType;
    }

    private String getRegistryAddress(String registryAddress){
        if (StringUtils.isEmpty(registryAddress) || (RpcConstants.RPC_REFERENCE_DEFAULT_REGISTRYADDRESS.equals(registryAddress) && !StringUtils.isEmpty(consumerYamlConfig.getRegistryAddress()))){
            registryAddress = consumerYamlConfig.getRegistryAddress();
        }
        return registryAddress;
    }

    private String getLoadBalanceType(String loadBalanceType){
        if (StringUtils.isEmpty(loadBalanceType) || (RpcConstants.RPC_REFERENCE_DEFAULT_LOADBALANCETYPE.equals(loadBalanceType) && !StringUtils.isEmpty(consumerYamlConfig.getLoadBalanceType()))){
            loadBalanceType = consumerYamlConfig.getLoadBalanceType();
        }
        return loadBalanceType;
    }

    private String getSerializationType(String serializationType){
        if (StringUtils.isEmpty(serializationType) || (RpcConstants.RPC_REFERENCE_DEFAULT_SERIALIZATIONTYPE.equals(serializationType) && !StringUtils.isEmpty(consumerYamlConfig.getSerializationType()))){
            serializationType = consumerYamlConfig.getSerializationType();
        }
        return serializationType;
    }

    private long getTimeout(long timeout){
        if (timeout <= 0  || (RpcConstants.RPC_REFERENCE_DEFAULT_TIMEOUT == timeout && consumerYamlConfig.getTimeout() > 0)){
            timeout = consumerYamlConfig.getTimeout();
        }
        return timeout;
    }

    private boolean getAsync(boolean async){
        if (!async){
            async = consumerYamlConfig.getAsync();
        }
        return async;
    }

    private boolean getOneway(boolean oneway){
        if (!oneway){
            oneway = consumerYamlConfig.getOneway();
        }
        return oneway;
    }
}
