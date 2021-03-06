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

/**
 * 常量工厂
 *
 * @author wjybxx
 * date - 2020/11/25
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface ConstantFactory<T> {

    /**
     * @param id   常量的数字id
     * @param name 常量的名字
     * @return 具体的常量对象
     */
    @Nonnull
    T newConstant(int id, String name);

}
