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

package com.wjybxx.fastjgame.util.constant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 可使用外部额外数据创建常量对象的工厂，
 */
public interface ExtConstantFactory<T> {

    /**
     * @param id      常量的数字id
     * @param name    常量的名字
     * @param extInfo 扩展信息。
     *                请注意，如果使用该特性，请确保创建的常量对象仍然是不可变的
     * @return 具体的常量对象
     */
    @Nonnull
    T newConstant(int id, String name, @Nullable Object extInfo);

}