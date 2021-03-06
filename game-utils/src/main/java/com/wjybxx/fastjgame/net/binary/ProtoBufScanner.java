/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.net.binary;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ProtocolMessageEnum;
import com.wjybxx.fastjgame.util.ClassScanner;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 扫描包中的protoBuffer消息类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/20
 */
public class ProtoBufScanner {

    private static final Set<String> SCAN_PACKAGES = Set.of("com.wjybxx.fastjgame");

    public static Set<Class<?>> scan() {
        return scan(SCAN_PACKAGES);
    }

    public static Set<Class<?>> scan(Set<String> packages) {
        return packages.stream()
                .map(scanPackage -> ClassScanner.findClasses(scanPackage,
                        name -> StringUtils.countMatches(name, "$") > 0,
                        ProtoBufScanner::isProtoBufferClass))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isProtoBufferClass(Class<?> messageClazz) {
        if (Modifier.isAbstract(messageClazz.getModifiers())) {
            return false;
        }
        return AbstractMessage.class.isAssignableFrom(messageClazz)
                || ProtocolMessageEnum.class.isAssignableFrom(messageClazz);
    }

}
