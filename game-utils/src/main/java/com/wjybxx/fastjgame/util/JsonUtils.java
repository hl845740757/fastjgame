/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Json工具类（字符集UTF-8）。
 * <p>
 * <p>
 * 最终还是选择了Jackson。
 * 建议：Gson也可以一用，但是最好不要使用FastJson，fastJson生成的json格式并不标准，有兼容性问题，此外代码质量不好。
 * jackson的代码质量真的很不错，扩展性很好。
 * <p>
 * )_( jackson也有坑的一点，主要在final字段上。
 * <p>
 * 这些方法真的很强大易用：
 * {@link TypeFactory#constructMapType(Class, Class, Class)}
 * {@link TypeFactory#constructArrayType(Class)}
 * {@link TypeFactory#constructCollectionType(Class, Class)}
 * <p>
 * 简单运用：
 * {@link #readMapFromJson(String, Class, Class, Class)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/12 23:05
 * github - https://github.com/hl845740757
 */
public class JsonUtils {

    /**
     * ObjectMapper自称自己是线程安全的，但是好像还是有点bug
     */
    private static final ThreadLocal<ObjectMapper> MAPPER_THREAD_LOCAL = ThreadLocal.withInitial(ObjectMapper::new);

    /**
     * 一定要私有，不可以暴露给外层，否则会导致应用程序对{@code Jackson}产生强依赖
     */
    private static ObjectMapper getMapper() {
        return MAPPER_THREAD_LOCAL.get();
    }

    // ---------------------------------- 基本支持 ---------------------------

    /**
     * 将bean转换为json字符串
     */
    public static String writeAsJson(@Nonnull Object bean) {
        try {
            return getMapper().writeValueAsString(bean);
        } catch (JsonProcessingException e) {
            // 之所以捕获，是因为，出现异常的地方应该是非常少的
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 解析json字符串为java对象。
     */
    public static <T> T readFromJson(@Nonnull String json, @Nonnull Class<T> clazz) {
        try {
            return getMapper().readValue(json, clazz);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 将bean转换为json对应的字节数组
     */
    public static byte[] writeAsJsonBytes(@Nonnull Object bean) {
        try {
            return getMapper().writeValueAsBytes(bean);
        } catch (JsonProcessingException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 解析json字符串对应的字节数组为java对象。
     */
    public static <T> T readFromJsonBytes(@Nonnull byte[] jsonBytes, @Nonnull Class<T> clazz) {
        try {
            return getMapper().readValue(jsonBytes, clazz);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // ---------------------------------- Map支持 ---------------------------

    /**
     * 解析json字符串为map对象。
     */
    public static <M extends Map> M readMapFromJson(@Nonnull String json,
                                                    @Nonnull Class<M> mapClass,
                                                    @Nonnull Class<?> keyClass,
                                                    @Nonnull Class<?> valueClass) {
        ObjectMapper mapper = getMapper();
        MapType mapType = mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
        try {
            return mapper.readValue(json, mapType);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 解析json字符串的字节数组为map对象。
     */
    public static <M extends Map<?, ?>> M readMapFromJsonBytes(@Nonnull byte[] jsonBytes,
                                                               @Nonnull Class<M> mapClass,
                                                               @Nonnull Class<?> keyClass,
                                                               @Nonnull Class<?> valueClass) {
        ObjectMapper mapper = getMapper();
        MapType mapType = mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
        try {
            return mapper.readValue(jsonBytes, mapType);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // ---------------------------------------- 输入输出流支持 -------------------------------

    public static void writeToOutputStream(OutputStream out, Object value) {
        try {
            getMapper().writeValue(out, value);
        } catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
    }

    public static <T> T readFromInputStream(InputStream in, Class<T> clazz) {
        try {
            return getMapper().readValue(in, clazz);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static void main(String[] args) {
        Int2IntMap data = new Int2IntOpenHashMap();
        data.put(1, 5);
        data.put(6, 7);

        String json = writeAsJson(data);
        System.out.println("json = " + json);

        Int2IntMap rData = readMapFromJson(json, Int2IntOpenHashMap.class, Integer.class, Integer.class);
        System.out.println("map = " + rData);

        System.out.println("equals = " + data.equals(rData));
    }
}
