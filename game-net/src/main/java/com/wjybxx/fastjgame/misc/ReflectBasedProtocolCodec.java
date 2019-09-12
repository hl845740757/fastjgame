/*
 *    Copyright 2019 wjybxx
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wjybxx.fastjgame.misc;

import com.google.protobuf.*;
import com.wjybxx.fastjgame.annotation.SerializableClass;
import com.wjybxx.fastjgame.annotation.SerializableField;
import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.net.ProtocolCodec;
import com.wjybxx.fastjgame.utils.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 基于反射的协议编解码工具，它基于protoBuf自定义编码格式。
 * 相对于{@link JsonBasedProtocolCodec}传输的数据量要少得多(1/3 - 1/2)，更少的数据量当然带来更快编码速度，
 * 加上网络传输的影响，这个差距会被放大。
 * <p>
 * 建议能单例就单例，能减少内存占用。
 * <p>
 * 后期可能会开发生成代码的工具，为每一个类生成对应的编解码代码，对性能和空间都会有一定帮助。
 * <p>
 * 反射的好处：
 * 1. 代码量少，功能强大。
 * 2. 可扩展性好。
 * 3. 可理解性更好。
 * 4. 支持多态，支持泛型等高级特性，如果是静态编码，所有的传输对象声明类型必须是最终的编码类型，代码复用能力差，而且维护成本高。
 * <p>
 * 反射的缺点：
 * 1. 反射比正常方法调用性能差一点，也没有差的太多。
 * 2. 大量的拆装箱操作。
 * <p>
 * 其实我心里还是支持用反射，尤其是在现在的架构下，解码操作可以在Netty的IO线程，也可以在NetEventLoop线程（NetEventLoop线程是非常清闲的），
 * 对应用层的性能降低很有限。
 * <p>
 * 自定义协议，不要使用原生带Tag的方法！
 * 突然发现...计算大小的方法不需要了..............
 * <p>
 * 编码格式：
 * <pre>
 *     	基本数据类型		tag + value
 *     	String          tag + length + bytes
 * 		List/Set 		tag + size + element,element....              element 编解码 -- 递归
 * 		Map      		tag + size + key,value,key,value.....         key/Value 编解码 -- 递归
 * 		Message  		tag + messageId + length + bytes
 * 	    Serializable	tag + messageId + field,field.... endTag       field 构成: tag + number + data
 * 	    枚举				tag + messageId + number
 * 	    基本类型数组      tag + size + value,value......
 * </pre>
 * 其中messageId用于确定一个唯一的类！也就是{@link MessageMapper}的重要作用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/19
 * github - https://github.com/hl845740757
 */
@Immutable
@ThreadSafe
public class ReflectBasedProtocolCodec implements ProtocolCodec {

    private final MessageMapper messageMapper;
    /**
     * proto message 解析方法
     */
    private final Map<Class<?>, Parser<?>> parserMap;
    /**
     * 普通类的解析信息
     */
    private final Map<Class<?>, ClassDescriptor> descriptorMap;
    /**
     * 枚举解析方法
     */
    private final Map<Class<?>, EnumDescriptor> enumDescriptorMap;
    /**
     * 所有codec映射
     */
    private final NumberEnumMapper<Codec<?>> codecMapper;

    private ReflectBasedProtocolCodec(MessageMapper messageMapper,
                                      Map<Class<?>, Parser<?>> parserMap,
                                      Map<Class<?>, ClassDescriptor> descriptorMap,
                                      Map<Class<?>, EnumDescriptor> enumDescriptorMap) {
        this.messageMapper = messageMapper;
        this.parserMap = parserMap;
        this.descriptorMap = descriptorMap;
        this.enumDescriptorMap = enumDescriptorMap;

        codecMapper = EnumUtils.indexNumberEnum(initValues());
    }

    private Codec<?>[] initValues() {
        // 预估出现的频率排个序
        return new Codec[]{
                new IntegerCodec(),
                new LongCodec(),
                new FloatCodec(),
                new DoubleCodec(),

                new StringCodec(),
                new MessageCodec(),
                new ReferenceCodec(),

                new ListCodec(),
                new MapCodec(),
                new SetCodec(),

                new ProtoEnumCodec(),
                new NumberEnumCodec(),

                // 字节数组比较常见
                new ByteArrayCodec(),

                new BoolCodec(),
                new ByteCodec(),
                new ShortCodec(),

                // 其它数组使用较少
                new IntArrayCodec(),
                new LongArrayCodec(),
                new FloatArrayCodec(),
                new DoubleArrayCodec(),
                new CharArrayCodec(),
                // short数组我工作至今都没用过几次。。。
                new ShortArrayCodec(),

                // 正常情况很少使用char
                new CharCodec(),
        };
    }

    @Override
    public ByteBuf encodeRpcRequest(ByteBufAllocator bufAllocator, @Nonnull Object request) throws IOException {
        return encode(bufAllocator, request);
    }

    @Override
    public Object decodeRpcRequest(ByteBuf data) throws IOException {
        return decode(data);
    }

    @Override
    public ByteBuf encodeRpcResponse(ByteBufAllocator bufAllocator, @Nonnull Object body) throws IOException {
        return encode(bufAllocator, body);
    }

    @Override
    public Object decodeRpcResponse(ByteBuf data) throws IOException {
        return decode(data);
    }

    @Override
    public ByteBuf encodeMessage(ByteBufAllocator bufAllocator, Object message) throws IOException {
        return encode(bufAllocator, message);
    }

    @Override
    public Object decodeMessage(ByteBuf data) throws IOException {
        return decode(data);
    }

    /**
     * 编码一个对象。
     *
     * @param bufAllocator byteBuf分配器
     * @param object       待序列化的对象
     * @return encodeResult
     * @throws IOException error
     */
    private ByteBuf encode(ByteBufAllocator bufAllocator, Object object) throws IOException {
        final byte[] localBuffer = NetUtils.LOCAL_BUFFER.get();
        // 减少字节数组创建，即使使用输出流构造，其内部还是做了缓存。
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        writeObject(codedOutputStream, object);
        // 真正写入到byteBuf
        final ByteBuf buffer = bufAllocator.buffer(codedOutputStream.getTotalBytesWritten());
        buffer.writeBytes(localBuffer, 0, codedOutputStream.getTotalBytesWritten());
        return buffer;
    }

    /**
     * 解码一个对象
     *
     * @param data byteBuf的输入流
     * @return value
     * @throws IOException error
     */
    private Object decode(ByteBuf data) throws IOException {
        final byte[] localBuffer = NetUtils.LOCAL_BUFFER.get();
        final int readableBytes = data.readableBytes();
        data.readBytes(localBuffer, 0, readableBytes);
        // 减少字节数组创建，即使使用输入流构造，其内部还是做了缓存。
        CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, readableBytes);
        // 真正读取数据
        return readObject(codedInputStream);
    }

    @SuppressWarnings("unchecked")
    private void writeObject(CodedOutputStream outputStream, @Nullable Object object) throws IOException {
        if (object == null) {
            writeTag(outputStream, WireType.NULL);
            return;
        }
        final Class<?> type = object.getClass();
        for (Codec codec : codecMapper.values()) {
            if (codec.isSupport(type)) {
                writeTag(outputStream, codec.getWireType());
                codec.writeData(outputStream, object, true);
                return;
            }
        }
        throw new UnsupportedOperationException("type " + object.getClass().getName());
    }

    /**
     * 读取一个对象，先读取一个标记，在读取数据
     *
     * @param inputStream 输入流
     * @return obj
     * @throws IOException error
     */
    @Nullable
    private Object readObject(CodedInputStream inputStream) throws IOException {
        final byte wireType = readTag(inputStream);
        if (wireType == WireType.NULL) {
            return null;
        }
        Codec<?> codec = getCodec(wireType);
        return codec.readData(inputStream, true);
    }

    @Nonnull
    private Codec<?> getCodec(int wireType) {
        Codec<?> codec = codecMapper.forNumber(wireType);
        if (null == codec) {
            throw new UnsupportedOperationException("unsupported wireType " + wireType);
        }
        return codec;
    }


    /**
     * 计算对象的大小
     *
     * @param object 对基本类型进行了装箱操作
     * @return size
     */
    @SuppressWarnings("unchecked")
    private int calSerializeSize(@Nullable Object object) throws IOException {
        if (object == null) {
            return calTagSize(WireType.NULL);
        }
        final Class<?> type = object.getClass();
        for (Codec codec : codecMapper.values()) {
            if (codec.isSupport(type)) {
                return calTagSize(codec.getWireType()) + codec.calSerializeDataSize(object, true);
            }
        }
        throw new UnsupportedOperationException("un support type " + object.getClass().getName());
    }

    /**
     * 计算tag大小，也就是类型符号的大小，tag都是整数，使用uint32编解码
     *
     * @param wireType 类型
     * @return 序列化后的大小。
     */
    private static int calTagSize(byte wireType) {
        assert wireType >= 0;
        return 1;
    }

    private static void writeTag(CodedOutputStream outputStream, byte wireType) throws IOException {
        outputStream.writeRawByte(wireType);
    }

    private static byte readTag(CodedInputStream inputStream) throws IOException {
        return inputStream.readRawByte();
    }

    /**
     * 类文件描述符
     */
    private static class ClassDescriptor {
        /**
         * 构造方法
         */
        private final Constructor constructor;
        /**
         * 要序列化的字段的映射
         */
        private final NumberEnumMapper<FieldDescriptor> fieldDescriptorMapper;

        private ClassDescriptor(FieldDescriptor[] serializableFields, Constructor constructor) {
            this.constructor = constructor;
            this.fieldDescriptorMapper = EnumUtils.indexNumberEnum(serializableFields);
        }
    }

    private static class FieldDescriptor implements NumberEnum {
        /**
         * 用于反射赋值
         */
        private final Field field;
        /**
         * 字段对应的number，用于兼容性支持，认为一般不会出现负数
         */
        private final int number;

        /**
         * 如果是数字的话，是否可能负数
         */
        private final boolean mayNegative;
        /**
         * 数据类型(该缓存是为了提高编码速度的)
         */
        private final byte wireType;

        private FieldDescriptor(Field field, int number, boolean mayNegative, byte wireType) {
            this.field = field;
            this.number = number;
            this.mayNegative = mayNegative;
            this.wireType = wireType;
        }

        @Override
        public int getNumber() {
            return number;
        }
    }

    private static class EnumDescriptor {

        /**
         * 解析方法
         */
        private final Method forNumberMethod;

        private EnumDescriptor(Method forNumberMethod) {
            this.forNumberMethod = forNumberMethod;
        }
    }

    private interface Codec<T> extends NumberEnum {

        /**
         * 是否支持编码该对象
         *
         * @param type 数据类型
         * @return 如果支持则返回true
         */
        boolean isSupport(Class<?> type);

        /**
         * 计算序列化后的大小，不包含wireType
         *
         * @param obj         待计算的对象
         * @param mayNegative 是否可能为负数，在没有额外信息的情况下，默认为true，可能为负数
         * @return size
         */
        int calSerializeDataSize(@Nonnull T obj, boolean mayNegative) throws IOException;

        /**
         * 编码协议内容，不包含wireType
         *
         * @param outputStream 输出流
         * @param obj          待编码的对象
         * @param mayNegative  是否可能为负数，在没有额外信息的情况下，默认为true，可能为负数
         * @throws IOException error，
         */
        void writeData(CodedOutputStream outputStream, @Nonnull T obj, boolean mayNegative) throws IOException;

        /**
         * 解码字段协议内容，不包含wireType
         *
         * @param inputStream 输入流
         * @param mayNegative 是否可能为负数，在没有额外信息的情况下，默认为true，可能为负数
         * @return data
         * @throws IOException error
         */
        T readData(CodedInputStream inputStream, boolean mayNegative) throws IOException;

        @Override
        default int getNumber() {
            return getWireType();
        }

        /**
         * 该codec对应的类型
         *
         * @return wireType
         */
        byte getWireType();
    }

    /**
     * 一个字节就占用一个字节 - 不需要使用int32的格式
     */
    private static class ByteCodec implements Codec<Byte> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Byte.class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull Byte obj, boolean mayNegative) throws IOException {
            return 1;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Byte obj, boolean mayNegative) throws IOException {
            outputStream.writeRawByte(obj);
        }

        @Override
        public Byte readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return inputStream.readRawByte();
        }

        @Override
        public byte getWireType() {
            return WireType.BYTE;
        }
    }

    // -------------------------------------------- int32 -----------------------------------------------

    /**
     * 计算int值占用的空间大小
     *
     * @param value       要编码的值
     * @param mayNegative 是否可能为负数？如果可能为负，使用sint32编码，否则使用普通Int32编码(负数固定5字节)
     * @return size
     */
    private static int calInt32Size(int value, boolean mayNegative) {
        if (mayNegative) {
            return CodedOutputStream.computeSInt32SizeNoTag(value);
        } else {
            return CodedOutputStream.computeInt32SizeNoTag(value);
        }
    }

    /**
     * 写入一个整数
     *
     * @param outputStream 输出流
     * @param value        要编码的值
     * @param mayNegative  是否可能为负数？如果可能为负，使用sint32编码，否则使用普通Int32编码(负数固定5字节)
     * @throws IOException error
     */
    private static void writeInt32(CodedOutputStream outputStream, int value, boolean mayNegative) throws IOException {
        if (mayNegative) {
            outputStream.writeSInt32NoTag(value);
        } else {
            outputStream.writeInt32NoTag(value);
        }
    }

    /**
     * 读取一个整数
     *
     * @param inputStream 输入流
     * @param mayNegative 是否可能为负数？如果可能为负，使用sint32解码，否则使用普通Int32解码(负数固定5字节)
     * @return value
     * @throws IOException error
     */
    private static int readInt32(CodedInputStream inputStream, boolean mayNegative) throws IOException {
        if (mayNegative) {
            return inputStream.readSInt32();
        } else {
            return inputStream.readInt32();
        }
    }

    private static abstract class Int32Codec<T extends Number> implements Codec<T> {

        @Override
        public final int calSerializeDataSize(@Nonnull T obj, boolean mayNegative) {
            return calInt32Size(obj.intValue(), mayNegative);
        }

        @Override
        public final void writeData(CodedOutputStream outputStream, @Nonnull T obj, boolean mayNegative) throws IOException {
            writeInt32(outputStream, obj.intValue(), mayNegative);
        }

    }

    private static class IntegerCodec extends Int32Codec<Integer> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Integer.class;
        }

        @Override
        public Integer readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return readInt32(inputStream, mayNegative);
        }

        @Override
        public byte getWireType() {
            return WireType.INT;
        }

    }

    private static class ShortCodec extends Int32Codec<Short> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Short.class;
        }

        @Override
        public Short readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return (short) readInt32(inputStream, mayNegative);
        }

        @Override
        public byte getWireType() {
            return WireType.SHORT;
        }
    }

    private static class CharCodec implements Codec<Character> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Character.class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull Character obj, boolean mayNegative) {
            // char 是无符号整形
            return CodedOutputStream.computeUInt32SizeNoTag(obj);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Character obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj);
        }

        @Override
        public Character readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return (char) inputStream.readUInt32();
        }

        @Override
        public byte getWireType() {
            return WireType.CHAR;
        }
    }

    private static class LongCodec implements Codec<Long> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Long.class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull Long obj, boolean mayNegative) {
            if (mayNegative) {
                return CodedOutputStream.computeSInt64SizeNoTag(obj);
            } else {
                return CodedOutputStream.computeInt64SizeNoTag(obj);
            }
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Long obj, boolean mayNegative) throws IOException {
            if (mayNegative) {
                outputStream.writeSInt64NoTag(obj);
            } else {
                outputStream.writeInt64NoTag(obj);
            }
        }

        @Override
        public Long readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            if (mayNegative) {
                return inputStream.readSInt64();
            } else {
                return inputStream.readInt64();
            }
        }

        @Override
        public byte getWireType() {
            return WireType.LONG;
        }
    }

    private static class FloatCodec implements Codec<Float> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Float.class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull Float obj, boolean mayNegative) {
            return CodedOutputStream.computeFloatSizeNoTag(obj);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Float obj, boolean mayNegative) throws IOException {
            outputStream.writeFloatNoTag(obj);
        }

        @Override
        public Float readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return inputStream.readFloat();
        }

        @Override
        public byte getWireType() {
            return WireType.FLOAT;
        }
    }

    private static class DoubleCodec implements Codec<Double> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Double.class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull Double obj, boolean mayNegative) {
            return CodedOutputStream.computeDoubleSizeNoTag(obj);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Double obj, boolean mayNegative) throws IOException {
            outputStream.writeDoubleNoTag(obj);
        }

        @Override
        public Double readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return inputStream.readDouble();
        }

        @Override
        public byte getWireType() {
            return WireType.DOUBLE;
        }
    }

    private static class BoolCodec implements Codec<Boolean> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == Boolean.class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull Boolean obj, boolean mayNegative) {
            return CodedOutputStream.computeBoolSizeNoTag(obj);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Boolean obj, boolean mayNegative) throws IOException {
            outputStream.writeBoolNoTag(obj);
        }

        @Override
        public Boolean readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return inputStream.readBool();
        }

        @Override
        public byte getWireType() {
            return WireType.BOOLEAN;
        }
    }

    private static class StringCodec implements Codec<String> {

        @Override
        public boolean isSupport(Class<?> type) {
            return String.class == type;
        }

        @Override
        public int calSerializeDataSize(@Nonnull String obj, boolean mayNegative) {
            return CodedOutputStream.computeStringSizeNoTag(obj);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull String obj, boolean mayNegative) throws IOException {
            outputStream.writeStringNoTag(obj);
        }

        @Override
        public String readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            return inputStream.readString();
        }

        @Override
        public byte getWireType() {
            return WireType.STRING;
        }
    }

    private class MessageCodec implements Codec<AbstractMessage> {

        @Override
        public boolean isSupport(Class<?> type) {
            return AbstractMessage.class.isAssignableFrom(type);
        }

        @Override
        public int calSerializeDataSize(@Nonnull AbstractMessage obj, boolean mayNegative) {
            return CodedOutputStream.computeInt32SizeNoTag(messageMapper.getMessageId(obj.getClass())) +
                    CodedOutputStream.computeMessageSizeNoTag(obj);
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull AbstractMessage obj, boolean mayNegative) throws IOException {
            outputStream.writeInt32NoTag(messageMapper.getMessageId(obj.getClass()));
            outputStream.writeMessageNoTag(obj);
        }

        @SuppressWarnings("unchecked")
        @Override
        public AbstractMessage readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int messageId = inputStream.readInt32();
            final Class<?> messageClazz = messageMapper.getMessageClazz(messageId);
            final Parser<AbstractMessage> parser = (Parser<AbstractMessage>) parserMap.get(messageClazz);
            return inputStream.readMessage(parser, ExtensionRegistryLite.getEmptyRegistry());
        }

        @Override
        public byte getWireType() {
            return WireType.MESSAGE;
        }
    }

    private abstract class EnumCodec<T> implements Codec<T> {

        protected abstract int getNumber(T obj);

        @Override
        public int calSerializeDataSize(@Nonnull T obj, boolean mayNegative) {
            return CodedOutputStream.computeInt32SizeNoTag(messageMapper.getMessageId(obj.getClass())) +
                    CodedOutputStream.computeEnumSizeNoTag(getNumber(obj));
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull T obj, boolean mayNegative) throws IOException {
            outputStream.writeInt32NoTag(messageMapper.getMessageId(obj.getClass()));
            outputStream.writeEnumNoTag(getNumber(obj));
        }

        @SuppressWarnings("unchecked")
        @Override
        public T readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            int messageId = inputStream.readInt32();
            int number = inputStream.readEnum();
            Class<?> enumClass = messageMapper.getMessageClazz(messageId);
            try {
                return (T) enumDescriptorMap.get(enumClass).forNumberMethod.invoke(null, number);
            } catch (Exception e) {
                throw new IOException(enumClass.getName(), e);
            }
        }
    }

    private class ProtoEnumCodec extends EnumCodec<ProtocolMessageEnum> {

        @Override
        public boolean isSupport(Class<?> type) {
            return ProtocolMessageEnum.class.isAssignableFrom(type);
        }

        @Override
        public byte getWireType() {
            return WireType.PROTO_ENUM;
        }

        @Override
        protected int getNumber(ProtocolMessageEnum obj) {
            return obj.getNumber();
        }
    }

    private class NumberEnumCodec extends EnumCodec<NumberEnum> {

        @Override
        public boolean isSupport(Class<?> type) {
            return NumberEnum.class.isAssignableFrom(type);
        }

        @Override
        public byte getWireType() {
            return WireType.NUMBER_ENUM;
        }

        @Override
        protected int getNumber(NumberEnum obj) {
            return obj.getNumber();
        }

    }

    // 集合支持
    private abstract class CollectionCodec<T extends Collection> implements Codec<T> {

        @Override
        public int calSerializeDataSize(@Nonnull T obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.size());
            if (obj.size() == 0) {
                return totalSize;
            }
            for (Object element : obj) {
                totalSize += ReflectBasedProtocolCodec.this.calSerializeSize(element);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull T obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.size());
            if (obj.size() == 0) {
                return;
            }
            for (Object element : obj) {
                ReflectBasedProtocolCodec.this.writeObject(outputStream, element);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public T readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            int size = inputStream.readUInt32();
            if (size == 0) {
                return newCollection(0);
            }
            T collection = newCollection(size);
            for (int index = 0; index < size; index++) {
                collection.add(ReflectBasedProtocolCodec.this.readObject(inputStream));
            }
            return collection;
        }

        /**
         * 当size大于0时，尝试返回一个足够容量的集合
         *
         * @param size 需要的大小
         */
        @Nonnull
        protected abstract T newCollection(int size);

    }

    private class ListCodec extends CollectionCodec<List> {

        @Override
        public boolean isSupport(Class<?> type) {
            return List.class.isAssignableFrom(type);
        }

        @Override
        public byte getWireType() {
            return WireType.LIST;
        }

        @Nonnull
        @Override
        protected List newCollection(int size) {
            return new ArrayList(size);
        }
    }

    private class SetCodec extends CollectionCodec<Set> {

        @Override
        public boolean isSupport(Class<?> type) {
            return Set.class.isAssignableFrom(type);
        }

        @Override
        public byte getWireType() {
            return WireType.SET;
        }

        @Nonnull
        @Override
        protected Set newCollection(int size) {
            return new LinkedHashSet(size, 1);
        }
    }

    private class MapCodec implements Codec<Map> {

        @Override
        public boolean isSupport(Class<?> type) {
            return Map.class.isAssignableFrom(type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public int calSerializeDataSize(@Nonnull Map obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.size());
            if (obj.size() == 0) {
                return totalSize;
            }
            for (Map.Entry entry : ((Map<Object, Object>) obj).entrySet()) {
                totalSize += ReflectBasedProtocolCodec.this.calSerializeSize(entry.getKey());
                totalSize += ReflectBasedProtocolCodec.this.calSerializeSize(entry.getValue());
            }
            return totalSize;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Map obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.size());
            if (obj.size() == 0) {
                return;
            }
            for (Map.Entry entry : ((Map<Object, Object>) obj).entrySet()) {
                ReflectBasedProtocolCodec.this.writeObject(outputStream, entry.getKey());
                ReflectBasedProtocolCodec.this.writeObject(outputStream, entry.getValue());
            }
        }

        @Override
        public Map readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            int size = inputStream.readUInt32();
            if (size == 0) {
                return new HashMap();
            }
            Map<Object, Object> map = CollectionUtils.newEnoughCapacityLinkedHashMap(size);
            for (int index = 0; index < size; index++) {
                Object key = ReflectBasedProtocolCodec.this.readObject(inputStream);
                Object value = ReflectBasedProtocolCodec.this.readObject(inputStream);
                map.put(key, value);
            }
            return map;
        }

        @Override
        public byte getWireType() {
            return WireType.MAP;
        }
    }

    /**
     * 带注解的类的编解码器
     */
    private class ReferenceCodec implements Codec<Object> {

        @Override
        public boolean isSupport(Class<?> type) {
            return descriptorMap.containsKey(type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public int calSerializeDataSize(@Nonnull Object obj, boolean ignore) throws IOException {
            ClassDescriptor descriptor = descriptorMap.get(obj.getClass());
            int size = CodedOutputStream.computeInt32SizeNoTag(messageMapper.getMessageId(obj.getClass()));
            try {
                for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptorMapper.values()) {
                    // nullable
                    Object fieldValue = fieldDescriptor.field.get(obj);
                    // null不序列化
                    if (null == fieldValue) {
                        continue;
                    }
                    // 先写tag才能判断是否结束
                    size += calTagSize(fieldDescriptor.wireType);
                    // number,认为一般不会出现负数，但是保留出现负数的可能
                    size += calInt32Size(fieldDescriptor.number, false);
                    Codec codec = getCodec(fieldDescriptor.wireType);
                    size += codec.calSerializeDataSize(fieldValue, fieldDescriptor.mayNegative);
                }
                // 加上一个终止标记
                size += calTagSize(WireType.REFERENCE_END);
            } catch (Exception e) {
                throw new IOException(obj.getClass().getName(), e);
            }
            return size;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull Object obj, boolean ignore) throws IOException {
            ClassDescriptor descriptor = descriptorMap.get(obj.getClass());
            outputStream.writeInt32NoTag(messageMapper.getMessageId(obj.getClass()));
            try {
                for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptorMapper.values()) {
                    // nullable
                    Object fieldValue = fieldDescriptor.field.get(obj);
                    // null不序列化
                    if (null == fieldValue) {
                        continue;
                    }
                    // 先写tag才能判断是否结束
                    writeTag(outputStream, fieldDescriptor.wireType);
                    // number,认为一般不会出现负数，但是保留出现负数的可能
                    writeInt32(outputStream, fieldDescriptor.number, false);
                    // 编码真正的内容
                    Codec codec = getCodec(fieldDescriptor.wireType);
                    codec.writeData(outputStream, fieldValue, fieldDescriptor.mayNegative);
                }
                // 加上一个终止标记
                writeTag(outputStream, WireType.REFERENCE_END);
            } catch (Exception e) {
                throw new IOException(obj.getClass().getName(), e);
            }
        }

        @Override
        public Object readData(CodedInputStream inputStream, boolean ignore) throws IOException {
            int messageId = inputStream.readInt32();
            Class<?> messageClass = messageMapper.getMessageClazz(messageId);
            ClassDescriptor descriptor = descriptorMap.get(messageClass);

            try {
                Object instance = descriptor.constructor.newInstance();
                int wireType;
                while ((wireType = readTag(inputStream)) != WireType.REFERENCE_END) {
                    int number = readInt32(inputStream, false);
                    // nullable
                    FieldDescriptor fieldDescriptor = descriptor.fieldDescriptorMapper.forNumber(number);
                    if (null == fieldDescriptor) {
                        throw new IOException("Incompatible type " + messageClass.getName() + ", number = " + number);
                    }
                    Object fieldValue = getCodec(wireType).readData(inputStream, fieldDescriptor.mayNegative);
                    fieldDescriptor.field.set(instance, fieldValue);
                }
                return instance;
            } catch (Exception e) {
                throw new IOException(messageClass.getName(), e);
            }
        }

        @Override
        public byte getWireType() {
            return WireType.REFERENCE;
        }
    }

    // ------------------------------------------------- 基本数组支持 ---------------------------------------------

    private static class ByteArrayCodec implements Codec<byte[]> {
        @Override
        public boolean isSupport(Class<?> type) {
            return type == byte[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull byte[] obj, boolean mayNegative) throws IOException {
            return CodedOutputStream.computeUInt32SizeNoTag(obj.length) + obj.length;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull byte[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length > 0) {
                outputStream.writeRawBytes(obj);
            }
        }

        @Override
        public byte[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new byte[0];
            }
            return inputStream.readRawBytes(length);
        }

        @Override
        public byte getWireType() {
            return WireType.BYTE_ARRAY;
        }
    }

    private static class IntArrayCodec implements Codec<int[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == int[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull int[] obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.length);
            if (obj.length == 0) {
                return totalSize;
            }
            for (int value : obj) {
                totalSize += CodedOutputStream.computeSInt32SizeNoTag(value);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull int[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (int value : obj) {
                outputStream.writeSInt32NoTag(value);
            }
        }

        @Override
        public int[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new int[0];
            }
            int[] result = new int[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readSInt32();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.INT_ARRAY;
        }
    }

    private static class ShortArrayCodec implements Codec<short[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == short[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull short[] obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.length);
            if (obj.length == 0) {
                return totalSize;
            }
            for (short value : obj) {
                totalSize += CodedOutputStream.computeSInt32SizeNoTag(value);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull short[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (short value : obj) {
                outputStream.writeSInt32NoTag(value);
            }
        }

        @Override
        public short[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new short[0];
            }
            short[] result = new short[length];
            for (int index = 0; index < length; index++) {
                result[index] = (short) inputStream.readSInt32();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.SHORT_ARRAY;
        }
    }

    private static class LongArrayCodec implements Codec<long[]> {
        @Override
        public boolean isSupport(Class<?> type) {
            return type == long[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull long[] obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.length);
            if (obj.length == 0) {
                return totalSize;
            }
            for (long value : obj) {
                totalSize += CodedOutputStream.computeSInt64SizeNoTag(value);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull long[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (long value : obj) {
                outputStream.writeSInt64NoTag(value);
            }
        }

        @Override
        public long[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new long[0];
            }
            long[] result = new long[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readSInt64();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.LONG_ARRAY;
        }
    }

    private static class FloatArrayCodec implements Codec<float[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == float[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull float[] obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.length);
            if (obj.length == 0) {
                return totalSize;
            }
            for (float value : obj) {
                totalSize += CodedOutputStream.computeFloatSizeNoTag(value);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull float[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (float value : obj) {
                outputStream.writeFloatNoTag(value);
            }
        }

        @Override
        public float[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new float[0];
            }
            float[] result = new float[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readFloat();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.FLOAT_ARRAY;
        }
    }

    private static class DoubleArrayCodec implements Codec<double[]> {
        @Override
        public boolean isSupport(Class<?> type) {
            return type == double[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull double[] obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.length);
            if (obj.length == 0) {
                return totalSize;
            }
            for (double value : obj) {
                totalSize += CodedOutputStream.computeDoubleSizeNoTag(value);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull double[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (double value : obj) {
                outputStream.writeDoubleNoTag(value);
            }
        }

        @Override
        public double[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new double[0];
            }
            double[] result = new double[length];
            for (int index = 0; index < length; index++) {
                result[index] = inputStream.readDouble();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.DOUBLE_ARRAY;
        }
    }

    private static class CharArrayCodec implements Codec<char[]> {

        @Override
        public boolean isSupport(Class<?> type) {
            return type == char[].class;
        }

        @Override
        public int calSerializeDataSize(@Nonnull char[] obj, boolean mayNegative) throws IOException {
            int totalSize = CodedOutputStream.computeUInt32SizeNoTag(obj.length);
            if (obj.length == 0) {
                return totalSize;
            }
            for (char value : obj) {
                totalSize += CodedOutputStream.computeUInt32SizeNoTag(value);
            }
            return totalSize;
        }

        @Override
        public void writeData(CodedOutputStream outputStream, @Nonnull char[] obj, boolean mayNegative) throws IOException {
            outputStream.writeUInt32NoTag(obj.length);
            if (obj.length == 0) {
                return;
            }
            for (char value : obj) {
                outputStream.writeUInt32NoTag(value);
            }
        }

        @Override
        public char[] readData(CodedInputStream inputStream, boolean mayNegative) throws IOException {
            final int length = inputStream.readUInt32();
            if (length == 0) {
                return new char[0];
            }
            char[] result = new char[length];
            for (int index = 0; index < length; index++) {
                result[index] = (char) inputStream.readUInt32();
            }
            return result;
        }

        @Override
        public byte getWireType() {
            return WireType.CHAR_ARRAY;
        }
    }

    // -------------------------------------------------  拷贝对象 --------------------------------------------------
    @Override
    public Object cloneRpcRequest(@Nonnull Object request) throws IOException {
        return cloneObject(request);
    }

    @Override
    public Object cloneRpcResponse(@Nonnull Object body) throws IOException {
        return cloneObject(body);
    }

    @Override
    public Object cloneMessage(@Nonnull Object message) throws IOException {
        return cloneObject(message);
    }

    private Object cloneObject(@Nonnull Object obj) throws IOException {
        final byte[] localBuffer = NetUtils.LOCAL_BUFFER.get();
        // 写进入
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(localBuffer);
        writeObject(codedOutputStream, obj);
        // 再读出来
        CodedInputStream codedInputStream = CodedInputStream.newInstance(localBuffer, 0, codedOutputStream.getTotalBytesWritten());
        return readObject(codedInputStream);
    }

    // ------------------------------------------------- 工厂方法 ------------------------------------------------------
    @Nonnull
    public static ReflectBasedProtocolCodec newInstance(MessageMapper messageMapper) {
        final Map<Class<?>, Parser<?>> parserMap = new IdentityHashMap<>();
        final Map<Class<?>, ClassDescriptor> classDescriptorMap = new IdentityHashMap<>();
        final Map<Class<?>, EnumDescriptor> enumDescriptorMap = new IdentityHashMap<>();

        try {
            // 使用反射一定要setAccessible为true，否则会拖慢性能。
            for (Class<?> messageClazz : messageMapper.getAllMessageClasses()) {
                // protoBuf消息
                if (AbstractMessage.class.isAssignableFrom(messageClazz)) {
                    Parser<?> parser = ReflectionUtils.findParser(messageClazz);
                    parserMap.put(messageClazz, parser);
                    continue;
                }
                // protoBufEnum
                if (ProtocolMessageEnum.class.isAssignableFrom(messageClazz)) {
                    indexEnumDescriptor(enumDescriptorMap, messageClazz);
                    continue;
                }
                // NumberEnum
                if (NumberEnum.class.isAssignableFrom(messageClazz)) {
                    if (messageClazz.isAnnotationPresent(SerializableClass.class)) {
                        indexEnumDescriptor(enumDescriptorMap, messageClazz);
                    }
                    continue;
                }
                // 带有注解的普通类 - 缓存所有的字段描述
                if (messageClazz.isAnnotationPresent(SerializableClass.class)) {
                    indexClassDescriptor(classDescriptorMap, messageClazz);
                }
            }
            return new ReflectBasedProtocolCodec(messageMapper, parserMap, classDescriptorMap, enumDescriptorMap);
        } catch (Exception e) {
            ConcurrentUtils.rethrow(e);
            // unreachable
            return null;
        }
    }

    private static void indexClassDescriptor(Map<Class<?>, ClassDescriptor> classDescriptorMap, Class<?> messageClazz) throws NoSuchMethodException {
        FieldDescriptor[] fieldDescriptors = Arrays.stream(messageClazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(SerializableField.class))
                .sorted(Comparator.comparingInt(field -> field.getAnnotation(SerializableField.class).number()))
                .map(field -> {
                    // 取消检测
                    field.setAccessible(true);
                    SerializableField annotation = field.getAnnotation(SerializableField.class);
                    return new FieldDescriptor(field, annotation.number(), annotation.mayNegative(), WireType.findType(field.getType()));
                })
                .toArray(FieldDescriptor[]::new);

        // 必须提供无参构造方法
        Constructor constructor = messageClazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        ClassDescriptor classDescriptor = new ClassDescriptor(fieldDescriptors, constructor);
        classDescriptorMap.put(messageClazz, classDescriptor);
    }

    private static void indexEnumDescriptor(Map<Class<?>, EnumDescriptor> enumDescriptorMap, Class<?> messageClazz) throws NoSuchMethodException {
        Method forNumberMethod = messageClazz.getMethod("forNumber", int.class);
        // 取消检测
        forNumberMethod.setAccessible(true);
        EnumDescriptor enumDescriptor = new EnumDescriptor(forNumberMethod);
        enumDescriptorMap.put(messageClazz, enumDescriptor);
    }

}
