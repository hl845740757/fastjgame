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

/**
 * 常量抽象，主要参考Netty实现，但又有所不同。
 * <p>
 * Q: 常量的含义？
 * A: 常量类似于枚举，仅仅使用==判断相等性，一般由{@link ConstantPool}创建。
 *
 * <p>
 * Q: 使用常量时需要注意的地方？
 * A: 1. 一般由{@link ConstantPool}创建。
 * 2. 其使用方式与{@link ThreadLocal}非常相似，优先定义静态属性，只有有足够理由的时候才定义非静态属性。
 *
 * <p>
 * Q: 类型参数{@link T}的作用？
 * A: 类型参数用作类型令牌，它好过{@link Class}对象，因为{@link Class}是不能带类型参数的，
 * 即你不能传递一个{@code Class<Map<Integer,String>>}给另一个方法，但你可以传递一个{@code Constant<Map<Integer,String>>}给另一个方法。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/7/31
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    /**
     * 注意：
     * 1. 该id仅仅在其所属的{@link ConstantPool}下唯一。
     * 2. 如果常量的创建存在竞争，那么其id可能并不稳定，也就不能保证连续。
     * 3. 如果常量的创建是无竞争的，那么常量之间的id应是连续的。
     *
     * @return 常量的数字id。
     */
    int id();

    /**
     * 注意：即使名字相同，也不代表是同一个同一个常量，只有同一个引用时才一定相等。
     *
     * @return 常量的名字。
     */
    String name();

}
