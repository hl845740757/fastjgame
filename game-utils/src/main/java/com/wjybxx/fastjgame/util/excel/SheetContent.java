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

package com.wjybxx.fastjgame.util.excel;

/**
 * 表示一个{@link Sheet}中的内容部分。
 *
 * @author wjybxx
 * date - 2020/11/21
 * github - https://github.com/hl845740757
 */
public interface SheetContent {

    /**
     * @return 表格内容的总行数
     */
    int totalRowCount();

    /**
     * @return 表格的有效内容行行数
     */
    int valueRowCount();

    default ParamSheetContent asParamSheetContent() {
        if (this instanceof ParamSheetContent) {
            return (ParamSheetContent) this;
        }
        throw Sheet.notParamSheetException();
    }

    default DefaultSheetContent asDefaultSheetContent() {
        if (this instanceof DefaultSheetContent) {
            return (DefaultSheetContent) this;
        }
        throw Sheet.notDefaultSheetException();
    }

}
