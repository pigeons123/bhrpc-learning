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
package io.binghe.rpc.consumer.yaml.factory;

import com.alibaba.fastjson.JSON;
import io.binghe.rpc.common.exception.RpcException;
import io.binghe.rpc.constants.RpcConstants;
import io.binghe.rpc.consumer.yaml.config.ConsumerYamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description Yml工具类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class ConsumerYamlConfigFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerYamlConfigFactory.class);

    private static Map<String, Object> propMap;

    static {
        InputStream resourceAsStream = null;
        try {
            ClassLoader classLoader = ConsumerYamlConfigFactory.class.getClassLoader();
            URL resource = classLoader.getResource(RpcConstants.CONSUMER_YML_FILE_NAME);
            if (resource == null){
                throw new RpcException(RpcConstants.CONSUMER_YML_FILE_NAME + " file is not found in class path.");
            }
            Yaml yaml = new Yaml();
            resourceAsStream = classLoader.getResourceAsStream(RpcConstants.CONSUMER_YML_FILE_NAME);
            Map<String, Map<String, Map<String, Map<String, Object>>>> map = yaml.loadAs(resourceAsStream, HashMap.class);
            propMap = map.get(RpcConstants.BHRPC).get(RpcConstants.BINGHE).get(RpcConstants.CONSUMER);
        } catch (Exception e) {
            LOGGER.error("读取yml文件异常: {}", e.getMessage());
            propMap = null;
        }finally {
            if (resourceAsStream != null){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    LOGGER.error("关闭yml文件流异常: {}", e.getMessage());
                }
            }
        }
    }

    public static ConsumerYamlConfig getConcumerYamlConfig(){
        if (propMap == null){
            return new ConsumerYamlConfig();
        }
        return JSON.parseObject(JSON.toJSONString(propMap), ConsumerYamlConfig.class);
    }
}
