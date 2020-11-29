#### 表格设计
表格整体分为两种，普通表格和param表（参数表）。  
+ 普通表格表头：

|  行数   |  简述 | 解释  | 
|  ----  | ----  |  ----  |  
|  第一行   |  cs   | c表示客户端需要该字段，s表示服务器需要该字；  另外为了支持扩展，该行使用类似linux命令行的格式，支持option参数  | 
|  第二行   | 字段类型  | 支持的类型：int32/int64/float/double/bool/string/数组/json；  其中json用于支持复杂的数据结构，不可大面积使用，引号对excel不友好 |
|  第三行   | 属性名字 | 用于定义该列的属性名，使用小驼峰法命名，属性名不可以重复。 |
|  第四行   | 属性描述  | 描述，向策划（或自己）解释该列的含义 |

配置示例：

|  cs    |  cs   |   c    |    c  |
|  ----  | ----  |  ----  |  ---  |
|    int32      |    int32     |    bool    |    string     |
|   skillId     |   actionId   |   shockScreen |  animationId  |
|    技能id     |     动作id    |    是否震屏    |    动画id     |
|     10001     |    20001     |    false      |   ani_30001   |
|     10002     |    20001     |     true      |   ani_30002   |

+ param表（参数表）表头：

|  行数   |  简述 | 解释  | 
|  ----  | ----  |  ----  |  
|  第一行   |  cs   |  与普通表格一致，一般可能三列都标cs  | 
|  第二行   | name/type/value  | 必须包含这三列，其中name列的值不可以重复，  type列与普通表支持的类型一致。|

注意！param表的前两行是固定的，
配置示例：

|  cs    |  cs   |   cs   |       |
|  ----  | ----  |  ----  |  ---  |
|  name  |  type |  value |  des  |
|  MAX_LEVEL  |  int32  |   100   | 玩家最大等级 |
|  NPC_NAME   |  string |  wjybxx | 默认NPC名字 |

#### 数组
在基本类型后添加"[]"表示数组类型，最多支持二维数组，如："int32[]"、"int32[][]"；  
在配置时使用"{}"表示数组（可理解为初始化块），使用**英文逗号**分隔，二维数组示例："{{1,2,3} , {4,5,6}}"；  
在内容存在与逗号相似的符号时，如果觉得不易区分，使用{}表达为二维数组，如： {{1.2, 1.3, 1.4}, {1.5, 1.6, 1.7}}。

#### 类型与解析方式扩展
可以通过自定义**CellValueParser**实现。