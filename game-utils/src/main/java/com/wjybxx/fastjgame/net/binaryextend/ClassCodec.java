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

package com.wjybxx.fastjgame.net.binaryextend;

import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link Class}对象序列化工具类
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/2/18
 * github - https://github.com/hl845740757
 */
@SuppressWarnings({"rawtypes"})
public class ClassCodec implements PojoCodecImpl<Class> {

    /**
     * 加载类的缓存 - 寻找类消耗较大，进行缓存.
     * 序列化Class的情况较少，不必每个线程缓存一份。
     */
    private static final ConcurrentMap<String, Class<?>> CACHE = new ConcurrentHashMap<>();

    @Override
    public Class readObject(ObjectReader reader) throws Exception {
        final String className = reader.readString();
        return findClass(className);
    }

    @Override
    public void writeObject(Class instance, ObjectWriter writer) throws Exception {
        writer.writeString(instance.getName());
    }

    @Override
    public Class<Class> getEncoderClass() {
        return Class.class;
    }

    @Nonnull
    public static Class findClass(String className) throws ClassNotFoundException {
        final Class<?> cacheClass = CACHE.get(className);
        if (cacheClass != null) {
            return cacheClass;
        }
        // 必须使用forName，因为某些类是生成的，不能通过类加载器直接加载
        final Class<?> newClass = Class.forName(className);
        CACHE.put(className, newClass);
        return newClass;
    }
}
