/**
 * Copyright 2022-9999 the original author or authors.
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
package io.binghe.rpc.fusing.counter;

import io.binghe.rpc.constants.RpcConstants;
import io.binghe.rpc.fusing.base.AbstractFusingInvoker;
import io.binghe.rpc.spi.annotation.SPIClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author binghe(公众号 : 冰河技术)
 * @version 1.0.0
 * @description 在一段时间内基于错误数量的熔断策略
 */
@SPIClass
public class CounterFusingInvoker extends AbstractFusingInvoker {

    private final Logger logger = LoggerFactory.getLogger(CounterFusingInvoker.class);

    @Override
    public boolean invokeFusingStrategy() {
        logger.info("execute counter fusing strategy, current fusing status is {}", fusingStatus.get());
        switch (fusingStatus.get()){
            //关闭状态
            case RpcConstants.FUSING_STATUS_CLOSED:
                return this.invokeClosedFusingStrategy();
            //半开启状态
            case RpcConstants.FUSING_STATUS_HALF_OPEN:
                return this.invokeHalfOpenFusingStrategy();
            //开启状态
            case RpcConstants.FUSING_STATUS_OPEN:
                return this.invokeOpenFusingStrategy();
            default:
                return this.invokeClosedFusingStrategy();
        }
    }

    /**
     * 处理开启状态
     */
    private boolean invokeOpenFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //超过一个指定的时间范围，则将状态设置为半开启状态
        if (currentTimeStamp - lastTimeStamp >= milliSeconds){
            fusingStatus.set(RpcConstants.FUSING_STATUS_HALF_OPEN);
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        return true;
    }

    /**
     * 处理半开启状态
     */
    private boolean invokeHalfOpenFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //服务已经恢复
        if (currentFailureCounter.get() <= 0){
            fusingStatus.set(RpcConstants.FUSING_STATUS_CLOSED);
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        //服务未恢复
        fusingStatus.set(RpcConstants.FUSING_STATUS_OPEN);
        lastTimeStamp = currentTimeStamp;
        return true;
    }

    /**
     * 处理关闭状态逻辑
     */
    private boolean invokeClosedFusingStrategy() {
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        //超过一个指定的时间范围
        if (currentTimeStamp - lastTimeStamp >= milliSeconds){
            lastTimeStamp = currentTimeStamp;
            this.resetCount();
            return false;
        }
        //超出配置的错误数量
        if (currentFailureCounter.get() >= totalFailure){
            lastTimeStamp = currentTimeStamp;
            fusingStatus.set(RpcConstants.FUSING_STATUS_OPEN);
            return true;
        }
        return false;
    }
}