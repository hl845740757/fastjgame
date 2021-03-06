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

package com.wjybxx.fastjgame.util.misc;

import com.wjybxx.fastjgame.net.binary.ObjectReader;
import com.wjybxx.fastjgame.net.binary.ObjectWriter;
import com.wjybxx.fastjgame.net.binary.PojoCodecImpl;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * int值封装。
 * 主要在lambda表达式中使用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/6 20:58
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class IntHolder {

    private int value;

    public IntHolder() {
        this(0);
    }

    public IntHolder(int value) {
        this.value = value;
    }

    /**
     * 获取当前值
     */
    public int get() {
        return value;
    }

    /**
     * 设置为指定值
     */
    public void set(int value) {
        this.value = value;
    }

    /**
     * 返回之后+1
     */
    public int getAndInc() {
        return value++;
    }

    /**
     * +1之后返回
     */
    public int incAndGet() {
        return ++value;
    }

    /**
     * 返回之后-1
     */
    public int getAndDec() {
        return value--;
    }

    /**
     * -1之后返回
     */
    public int decAndGet() {
        return --value;
    }

    /**
     * 加上指定增量并返回
     *
     * @param delta the value to add
     * @return the updated value
     */
    public int addAndGet(int delta) {
        this.value += delta;
        return this.value;
    }

    /**
     * 返回之后加上指定增量
     *
     * @param delta the value to add
     * @return the previous value
     */
    public int getAndAdd(int delta) {
        int result = this.value;
        this.value += delta;
        return result;
    }

    /**
     * 修改当前值，并返回之前的值
     *
     * @return old value
     */
    public int getAndSet(int value) {
        int result = this.value;
        this.value = value;
        return result;
    }

    @Override
    public String toString() {
        return "IntHolder{" +
                "value=" + value +
                '}';
    }

    @SuppressWarnings("unused")
    private static class Codec implements PojoCodecImpl<IntHolder> {

        @Override
        public Class<IntHolder> getEncoderClass() {
            return IntHolder.class;
        }

        @Override
        public IntHolder readObject(ObjectReader reader) throws Exception {
            return new IntHolder(reader.readInt());
        }

        @Override
        public void writeObject(IntHolder instance, ObjectWriter writer) throws Exception {
            writer.writeInt(instance.value);
        }
    }
}
