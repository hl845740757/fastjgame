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

package com.wjybxx.fastjgame.net.test;

import com.wjybxx.fastjgame.net.example.BinaryProtoCodecTest;
import com.wjybxx.fastjgame.net.example.ExampleConstants;
import com.wjybxx.fastjgame.net.example.ExampleMessages;
import com.wjybxx.fastjgame.net.serialization.Serializer;
import com.wjybxx.fastjgame.net.utils.NetUtils;

import java.util.Objects;

/**
 * 对象拷贝测试
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/11
 * github - https://github.com/hl845740757
 */
public class ProtocolCloneTest {

    public static void main(String[] args) throws Exception {
        // 触发NetUtils类加载，避免输出干扰
        System.out.println(NetUtils.getOuterIp());

        cloneTest(ExampleConstants.BINARY_SERIALIZER);
        cloneTest(ExampleConstants.JSON_SERIALIZER);
    }

    private static void cloneTest(Serializer serializer) throws Exception {
        System.out.println("\n" + serializer.getClass().getName());
        final ExampleMessages.FullMessage fullMessage = BinaryProtoCodecTest.newFullMessage();
        System.out.println("cloneField " + Objects.equals(serializer.cloneObject(fullMessage), fullMessage));
    }
}
