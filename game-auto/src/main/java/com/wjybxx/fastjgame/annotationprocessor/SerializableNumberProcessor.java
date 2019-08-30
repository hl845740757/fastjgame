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

package com.wjybxx.fastjgame.annotationprocessor;

import com.google.auto.service.AutoService;
import com.wjybxx.fastjgame.utils.AutoUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * 分析{@code SerializableField#number()}是否重复，以及是否在 [0,127之间]
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/23
 * github - https://github.com/hl845740757
 */
@AutoService(Processor.class)
public class SerializableNumberProcessor extends AbstractProcessor {

	// 使用这种方式可以脱离对utils，net包的依赖
	private static final String SERIALIZABLE_CLASS_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.SerializableClass";
	private static final String SERIALIZABLE_FIELD_CANONICAL_NAME = "com.wjybxx.fastjgame.annotation.SerializableField";
	private static final String NUMBER_METHOD_NAME = "number";

	// 工具类
	private Messager messager;
	private Elements elementUtils;
	private Types typeUtils;

	private TypeElement serializableClassElement;
	private DeclaredType serializableFieldType;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		elementUtils = processingEnv.getElementUtils();
		typeUtils = processingEnv.getTypeUtils();
		// 不能再这里初始化别的信息，因为对应的注解可能还未存在
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(SERIALIZABLE_CLASS_CANONICAL_NAME);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	private void ensureInited() {
		if (serializableClassElement != null) {
			return;
		}
		serializableClassElement = elementUtils.getTypeElement(SERIALIZABLE_CLASS_CANONICAL_NAME);
		serializableFieldType = typeUtils.getDeclaredType(elementUtils.getTypeElement(SERIALIZABLE_FIELD_CANONICAL_NAME));
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		ensureInited();
		// 该注解只有类可以使用
		@SuppressWarnings("unchecked")
		Set<TypeElement> typeElementSet = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(serializableClassElement);
		for (TypeElement typeElement:typeElementSet) {
			checkNumber(typeElement);
		}
		return false;
	}

	private void checkNumber(TypeElement typeElement) {
		final IntSet numberSet = new IntOpenHashSet(typeElement.getEnclosedElements().size());
		for (Element element:typeElement.getEnclosedElements()) {
			// 非成员属性
			if (element.getKind() != ElementKind.FIELD) {
				continue;
			}
			// 该注解只有Field可以使用
			VariableElement variableElement = (VariableElement) element;
			// 查找该字段上的注解
			final Optional<? extends AnnotationMirror> first = AutoUtils.findFirstAnnotationNotInheritance(typeUtils, variableElement, serializableFieldType);
			// 该成员属性没有serializableField注解
			if (!first.isPresent()) {
				continue;
			}
			// value中，基本类型会被封装为包装类型，number是int类型
			final int number = (Integer) AutoUtils.getAnnotationValueNotDefault(first.get(), NUMBER_METHOD_NAME);
			// 取值范围检测
			if (number < 0 || number > 127) {
				messager.printMessage(Diagnostic.Kind.ERROR, "number " + number + " must between [0, 127]", variableElement);
			}
			// 重复检测
			if (!numberSet.add(number)) {
				messager.printMessage(Diagnostic.Kind.ERROR, "number " + number + " is duplicate!", variableElement);
			}
		}
	}
}