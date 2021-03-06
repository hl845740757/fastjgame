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

package com.wjybxx.fastjgame.net.utils;

import com.google.protobuf.*;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * protocol buffer工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/24
 * github - https://github.com/hl845740757
 */
public class ProtoUtils {

    /**
     * 寻找protoBuf消息的parser对象
     *
     * @param clazz protoBuffer class
     * @return parser
     */
    public static <T extends MessageLite> Parser<T> findParser(@Nonnull Class<T> clazz) {
        Objects.requireNonNull(clazz);
        try {
            // 这个写法兼容2和3
            final Method method = clazz.getDeclaredMethod("getDefaultInstance");
            method.setAccessible(true);
            final Message instance = (Message) method.invoke(null);
            @SuppressWarnings("unchecked") final Parser<T> parserForType = (Parser<T>) instance.getParserForType();
            return parserForType;
        } catch (Exception e) {
            throw new IllegalArgumentException("bad class " + clazz.getName(), e);
        }
    }

    /**
     * 寻找protobuf枚举的映射信息
     *
     * @param clazz protoBuffer enum
     * @return map
     */
    public static <T extends ProtocolMessageEnum> Internal.EnumLiteMap<T> findMapper(@Nonnull Class<T> clazz) {
        Objects.requireNonNull(clazz);
        try {
            final Method method = clazz.getDeclaredMethod("internalGetValueMap");
            method.setAccessible(true);
            @SuppressWarnings("unchecked") final Internal.EnumLiteMap<T> mapper = (Internal.EnumLiteMap<T>) method.invoke(null);
            return mapper;
        } catch (Exception e) {
            throw new IllegalArgumentException("bad class " + clazz.getName(), e);
        }
    }
}
