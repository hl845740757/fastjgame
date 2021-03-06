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

/**
 * long值对
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/2
 * github - https://github.com/hl845740757
 */
public class LongPair {

    private final long first;
    private final long second;

    public LongPair(long first, long second) {
        this.first = first;
        this.second = second;
    }

    public long getFirst() {
        return first;
    }

    public long getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "LongPair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @SuppressWarnings("unused")
    private static class Codec implements PojoCodecImpl<LongPair> {

        @Override
        public Class<LongPair> getEncoderClass() {
            return LongPair.class;
        }

        @Override
        public LongPair readObject(ObjectReader reader) throws Exception {
            return new LongPair(reader.readLong(), reader.readLong());
        }

        @Override
        public void writeObject(LongPair instance, ObjectWriter writer) throws Exception {
            writer.writeLong(instance.first);
            writer.writeLong(instance.second);
        }
    }
}
