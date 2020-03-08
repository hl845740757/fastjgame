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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.utils.concurrent.Promise;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解表示该方法是一个Rpc调用。
 *
 * <h3>返回值</h3>
 * 1. 只要方法参数中存在{@link Promise}参数，则表示方法无法立即返回结果，注解处理器会捕获泛型参数作为返回值类型。
 * 2. 当方法返回值类型不为void时，表示方法可以立即返回结果，其返回值类型就是代理方法的返回值类型。
 * 3. 当返回值为void时，表明方法没有返回值，代理方法的返回值类型为通配符。
 *
 * <h3>限制</h3>
 * 1. {{@link Session}和{@link Promise}不会出现在客户端的代理方法的中，因此必须避免出现相同签名的代理方法。
 * 2. 方法不能是private - 至少是包级访问权限。
 * 3. methodId必须在[0,9999]区间段。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/17
 * github - https://github.com/hl845740757
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RpcMethod {

    /**
     * 该方法在该类中的唯一id。
     * 注意：取值范围为闭区间[0, 9999]。
     *
     * @return 由该id和serviceId构成唯一索引。
     */
    short methodId();
}
