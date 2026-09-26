# Android 逐应用广告拦截 — 工程规则

## 0. 项目元信息与产品定位

### 0.1 归属与来源

| 项目 | 值 |
| --- | --- |
| 应用名称 | 哪吒反广（NezhaCyberADSBlocker） |
| 包名 / Bundle ID | `xin.ctkqiang.nezha_cyber.ads_block` |
| 作者 | 哪吒网络安全的钟智强 |
| 联系方式 | johnmelodymel@qq.com |
| 源码仓库 | https://github.com/ctkqiang/NezhaCyberADSBlocker |
| 开源性质 | 开源项目（open source） |
| 服务对象 | 为人民而做，由人民共建, 谁把程序去删了，谁就是汉奸。（for the people, by the people） |

### 0.2 视觉基调

```text
app name    = 哪吒反广
theme color = red
style       = minimal
```

* 主色采用红色（red），用于主操作、状态强调与品牌标识。
* 整体风格保持极简（minimal），避免装饰性元素、渐变堆叠与视觉噪音。
* 本节约束第 5 节（UI 需求）：技术型外观不等于视觉复杂，极简是硬性要求。

### 0.3 数据立场

```text
本应用不收集任何用户数据。
```

本应用不采集、不上报、不出售任何用户数据，也不包含遥测（telemetry）、埋点或统计分析 SDK。

允许持久化的内容仅限本地设备上的运行所需数据，即第 20 节（隐私）与第 24 节（统计）所定义的元数据；这些数据不得离开用户设备。

### 0.4 本节约束项

1. 不得以任何形式引入数据采集、遥测、埋点或第三方分析 SDK。
2. 不得将本地统计或日志上传到任何远程服务。
3. 所有统计与日志默认仅存于本机，且可由用户清除。
4. UI 必须保持极简风格，主色为红色。
5. 任何新增依赖都必须符合开源性质，且不得破坏第 0.3 节的数据立场。
6. 仓库遵循开源项目规范：作者与来源信息应保留在 `README`、`LICENSE` 及关于页面中。

---

## 1. 项目目标

使用 **Kotlin + Jetpack Compose** 构建一个原生 Android 应用，提供**逐应用（per-application）的广告与追踪器拦截**能力。

应用必须允许用户：

1. 发现已安装的 Android 应用。
2. 选择需要对哪些应用进行保护。
3. 启动本地 Android `VpnService`。
4. 拦截被选中应用产生的网络流量。
5. 检查 DNS／网络元数据。
6. 将域名与广告／追踪器规则引擎进行匹配。
7. 阻断匹配到的目标地址。
8. 允许非目标应用绕过过滤层。
9. 展示拦截统计数据。
10. 维护用户可配置的白名单与黑名单。
11. 分析 APK，发现候选域名与网络端点。
12. 将 APK 静态分析结果与运行时观测到的网络域名进行关联。

本项目是一个**网络过滤／安全类应用**，默认不是一个 HTTPS MITM 代理。

首个实现版本必须优先考虑：

* 稳定性（Stability）
* 正确的 Android 网络行为
* 逐应用隔离（Per-app isolation）
* 低电量消耗
* 低延迟
* 确定性的过滤行为
* 不对检测到的广告做任何虚假断言
* 不做不必要的 TLS 拦截

---

# 2. 核心产品概念

本产品是：

> **逐应用网络防火墙 + 广告／追踪器规则引擎**

它**不是**单纯的 APK 字符串扫描器。

系统应当使用两个互补的信息来源：

```text
APK 静态分析
        +
运行时网络观测
        ↓
候选域名情报
        ↓
规则引擎
        ↓
逐应用过滤
```

静态分析负责识别潜在端点。

运行时观测负责判定应用实际与哪些域名通信。

任何单一来源都不应自动把某个域名归类为广告。

---

# 3. 技术栈

使用：

* Kotlin
* Jetpack Compose
* Android SDK
* Android `VpnService`
* Kotlin Coroutines
* Kotlin Flow / StateFlow
* Room（在需要持久化结构化数据时）
* DataStore（用于轻量级应用设置）
* Android PackageManager API
* Android 网络 API
* Gradle Kotlin DSL

避免引入不必要的第三方依赖。

在可行的情况下，优先使用 Android 平台 API 与 Kotlin 标准库能力。

---

# 4. UI 架构

使用 Jetpack Compose。

在以下层次之间保持清晰分离：

```text
ui/
domain/
data/
network/
vpn/
dns/
filtering/
apk/
apps/
```

不要在 Compose UI 代码中放置网络、VPN、数据包解析或 APK 分析逻辑。

Compose 只应消费应用状态，并派发用户操作。

示例：

```text
Compose UI
    ↓
ViewModel
    ↓
Use Case / Domain Layer
    ↓
Repository
    ↓
VPN / APK / Rule Engine
```

---

# 5. UI 需求

UI 应具备现代化安全工具的外观。

倾向于干净的技术型界面，而不是通用的 Material 演示类应用。

主要页面：

```text
首页（Home）
应用列表（Applications）
应用详情（Application Detail）
APK 分析（APK Analysis）
黑名单（Blocklist）
白名单（Allowlist）
网络活动（Network Activity）
统计（Statistics）
设置（Settings）
```

应用列表应能展示：

```text
应用名称
包名
图标
保护状态
已观测域名
已拦截请求数
```

示例：

```text
YouTube
com.example.youtube
Protection: ON
Blocked: 1,248
Observed domains: 31
```

---

# 6. 逐应用过滤

应用必须支持选择单个应用。

示例：

```text
受保护应用

[ON] Application A
[ON] Application B
[OFF] Application C
[OFF] Chrome
[OFF] WhatsApp
```

VPN 层必须能够区分：

```text
受保护应用（Protected applications）
```

与：

```text
被绕过应用（Bypassed applications）
```

当用户明确配置了逐应用过滤时，不要实现全局过滤。

在适当场景下使用 Android `VpnService.Builder` 的应用路由能力，包括：

```kotlin
addAllowedApplication()
addDisallowedApplication()
```

确切的路由策略必须在代码中留下文档说明。

不要假设这两种机制可以同时使用。

---

# 7. VPN 架构

核心网络层必须使用 Android `VpnService`。

高层架构：

```text
                 Android Application
                         │
                         ▼
                   VpnService
                         │
                         ▼
                  TUN Interface
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
       Protected Apps          Bypassed Apps
             │                       │
             ▼                       ▼
       Packet Processing          Internet
             │
             ▼
       DNS / Network Metadata
             │
             ▼
         Rule Engine
             │
       ┌─────┴─────┐
       ▼           ▼
     BLOCK       ALLOW
```

在目标 Android 版本要求时，VPN 服务必须作为恰当的前台服务（foreground service）运行。

需要处理：

* VPN 生命周期
* 用户 VPN 授权
* 服务启动／停止
* 重连
* 网络变化
* IPv4
* IPv6
* DNS 配置
* MTU
* 应用路由
* 资源清理

不得泄漏文件描述符（file descriptor）。

当用户明确关闭 VPN 后，不得让 VPN 保持活动状态。

---

# 8. DNS 过滤

首个过滤实现应主要聚焦于 DNS／域名级别的过滤。

示例：

```text
Application
    ↓
DNS query
    ↓
Rule Engine
    ↓
ads.example.com
    ↓
BLOCK
```

被放行的域名：

```text
api.example.com
    ↓
ALLOW
```

初始实现不要求解密 HTTPS。

DNS 引擎必须支持：

* 精确域名匹配
* 子域名匹配
* 在适当场景下的通配符／域名后缀规则
* 白名单优先级
* 黑名单优先级
* 规则优先级
* 高效查找
* 缓存
* 统计

示例规则：

```text
ads.example.com
tracker.example.com
*.ads.example.com
```

---

# 9. 规则引擎

规则引擎是核心组件。

不要把过滤逻辑写成散落在整个代码库中的：

```kotlin
if (domain == "...") ...
```

所有过滤决策都必须经过一个集中式的规则引擎。

示例接口：

```kotlin
interface RuleEngine {
    fun evaluate(domain: String, packageName: String): FilterDecision
}
```

可能的决策结果：

```kotlin
ALLOW
BLOCK
UNKNOWN
```

返回结果应携带足够元数据，供日志记录与统计使用。

示例：

```kotlin
data class FilterDecision(
    val action: Action,
    val matchedRule: String?,
    val source: RuleSource?,
)
```

---

# 10. 规则优先级

使用确定性的优先级顺序。

推荐的概念顺序：

```text
用户显式放行（Explicit User Allow）
        ↓
用户显式阻断（Explicit User Block）
        ↓
可信／系统规则（Trusted/System Rules）
        ↓
第三方阻断规则（Third-Party Block Rules）
        ↓
未知（Unknown）
```

不要静默覆盖用户的显式规则。

在实现中记录最终的优先级顺序。

---

# 11. 黑名单设计

黑名单必须是数据驱动的。

不要把成千上万个域名硬编码在 Kotlin 源文件里。

使用一种后续可以扩展的规则格式。

可能的来源：

```text
内置规则
用户规则
导入规则
第三方兼容规则
由分析生成的规则
```

规则应带有元数据：

```text
domain
type
source
enabled
createdAt
updatedAt
```

示例：

```text
ads.example.com
type=domain
source=user
enabled=true
```

---

# 12. APK 静态分析

APK 文件是二进制包。

**不要**假设源码可用。

一个 APK 可能包含：

```text
AndroidManifest.xml
classes.dex
classes2.dex
resources.arsc
assets/
lib/
```

静态分析应检查：

* APK 元数据
* 包名
* Manifest
* DEX 文件
* 资源
* Assets
* 在技术条件允许时的原生库
* 内嵌字符串
* URL
* 类域名（domain-like）字符串
* 已知 SDK 标识符

候选提取可能识别出类似如下的字符串：

```text
https://api.example.com
https://ads.example.com
https://tracker.example.com
```

但是：

```text
APK 中发现的字符串
≠
运行时网络请求
≠
广告
```

绝不要仅仅因为域名中出现以下字样，就自动把域名归类为广告：

```text
ads
advert
banner
tracker
analytics
```

这些只是指示信号（indicators）。

---

# 13. DEX 分析

应用可能包含：

```text
classes.dex
classes2.dex
classes3.dex
...
```

静态分析不得假设只有一个 DEX。

在可行的情况下，候选提取应处理所有可用的 DEX 文件。

不要仅依赖：

```bash
strings
```

作为最终架构。

原型阶段可以使用字符串提取，但生产架构应支持结构化解析。

潜在的后续分析：

```text
DEX
 ↓
字符串提取
 ↓
URL 提取
 ↓
域名规范化
 ↓
SDK 识别
 ↓
候选分类
```

---

# 14. 域名提取

对提取出的域名做规范化处理。

需要处理：

```text
https://example.com/path
http://example.com
https://sub.example.com/path?q=1
```

提取出：

```text
example.com
sub.example.com
```

不要把任意字符串当作域名。

使用域名解析器／校验器验证候选。

移除：

* 重复项
* 非法域名
* 本地文件系统路径
* 被错误识别为域名的包名
* 明显非网络的字符串

---

# 15. 运行时网络观测

在判定实际网络行为方面，运行时观测比静态 APK 分析更重要。

概念流程：

```text
启动目标应用
        ↓
观测 DNS／网络元数据
        ↓
收集域名
        ↓
规范化
        ↓
去重
        ↓
与静态候选关联
```

记录：

```text
packageName
timestamp
domain
destination
request count
blocked count
allowed count
```

在没有充分证据的情况下，不要断言某个请求是广告。

---

# 16. 静态与运行时关联

应用最终应提供置信度模型。

示例：

```text
Domain                  Static    Runtime    Classification
-------------------------------------------------------------
api.example.com          YES       YES       Normal
cdn.example.com          YES       YES       Normal
ads.example.com          YES       YES       Candidate
tracker.example.com      YES       YES       Candidate
old.example.com          YES       NO        Unobserved
```

UI 可以展示：

```text
静态候选（Static candidate）
运行时已观测（Runtime observed）
已知规则（Known rule）
用户阻断（User blocked）
用户放行（User allowed）
未知（Unknown）
```

不要把概率性分类当作绝对事实呈现。

---

# 17. 广告检测

广告检测必须基于证据。

可能的信号：

```text
命中已知黑名单
已知广告 SDK
已知追踪域名
域名信誉
运行时行为
URL／路径指示信号
静态 SDK 引用
用户反馈
```

**不要**使用诸如以下的简单粗暴规则：

```text
if ("ads" in domain) block
```

这会产生误报。

示例：

```text
ads.example.com
```

可能是广告。

但是：

```text
ads-api.example.com
```

可能是应用自身的 API 或内部服务。

因此分类必须保持显式且可解释。

---

# 18. HTTPS

第一个版本不实现 HTTPS MITM。

初始架构**不应**要求：

```text
自定义 CA
TLS 拦截
HTTPS 响应修改
证书替换
```

原因：

* 证书固定（Certificate pinning）
* TLS 复杂度
* HTTP/2
* HTTP/3
* QUIC
* 用户信任要求
* 隐私影响
* 稳定性风险

第一个版本应主要依赖：

```text
DNS
域名
网络元数据
```

---

# 19. QUIC / HTTP3

不要假设所有 HTTPS 流量都走 TCP。

现代应用可能使用：

```text
UDP :443
QUIC
HTTP/3
```

网络架构的设计必须让 QUIC 能被处理，或被明确记录为一项限制。

如果 QUIC 流量未被检查，不要虚假宣称实现了全流量检查。

---

# 20. 隐私

本应用是网络安全工具。

不要不必要地收集用户流量内容。

优先存储元数据，例如：

```text
package
domain
timestamp
action
matched rule
```

避免持久化存储：

```text
HTTP 消息体
密码
令牌（tokens）
Cookie
个人消息
```

除非未来某项功能明确要求，并且已向用户清晰披露。

---

# 21. 安全需求

将所有网络数据视为不可信输入。

需要校验：

* DNS 数据包
* 域名
* IP 地址
* 数据包长度
* 协议字段
* APK 内容
* DEX 字符串
* 导入的规则

防止：

* 缓冲区溢出
* 无界内存分配
* 畸形数据包导致的崩溃
* 正则表达式拒绝服务（ReDoS）
* 规则处理过程中的过度 CPU 占用
* 内存泄漏
* 文件描述符泄漏

不要执行从 APK 中提取出的代码。

静态分析必须保持静态。

---

# 22. 性能

过滤引擎可能处理大量的 DNS／网络事件。

避免：

```text
O(n)
```

在规则集变大后，对每一次 DNS 查询都做全列表扫描。

优先使用：

```text
Trie
后缀树（Suffix tree）
基于哈希的查找
编译后的规则结构
```

具体选择取决于规则语义。

对规则匹配做基准测试。

UI 不得在主线程上执行阻塞性的网络或文件操作。

恰当使用协程。

---

# 23. 日志

使用结构化日志。

生产环境避免过量的调试输出。

每一个拦截决策都应可解释。

示例：

```text
BLOCK
package=com.example.game
domain=ads.example.com
rule=ads.example.com
source=builtin
reason=domain_match
```

不要记录敏感的网络负载内容。

---

# 24. 统计

跟踪：

```text
DNS 请求总数
已拦截请求数
已放行请求数
被拦截域名数
受保护应用数
生效规则数
```

按应用维度：

```text
应用
请求数
已拦截
已放行
高频域名（Top Domains）
```

统计必须区分：

```text
observed（已观测）
blocked（已拦截）
allowed（已放行）
```

不要把每一个观测到的域名都称作广告。

---

# 25. 应用发现

使用 Android 的包／应用 API 发现已安装应用。

展示：

```text
应用标签（label）
包名
应用图标
保护状态
```

不要申请不必要的权限。

遵循目标 Android 版本的包可见性（package visibility）要求。

---

# 26. 数据架构

推荐结构：

```text
data/
├── database/
│   ├── AppDatabase.kt
│   ├── DomainDao.kt
│   ├── RuleDao.kt
│   └── EventDao.kt
│
├── repository/
│   ├── AppRepository.kt
│   ├── RuleRepository.kt
│   └── StatisticsRepository.kt
│
└── preferences/
    └── SettingsRepository.kt
```

使用 Room 存储结构化的持久化数据。

使用 DataStore 存储设置，例如：

```text
VPN enabled
Filtering enabled
Selected applications
Theme
DNS configuration
```

---

# 27. 建议的模块结构

随着项目成长，优先采用模块化架构。

```text
app/
core/
    common/
    model/
    database/
    logging/

feature/
    home/
    applications/
    analysis/
    rules/
    statistics/
    settings/

network/
    vpn/
    dns/
    packet/
    filtering/

analysis/
    apk/
    dex/
    domain/

data/
    repository/
```

在抽象尚未产生真实价值之前，不要创建过多的抽象层。

---

# 28. 编码规范

使用：

* Kotlin 惯用写法（idioms）
* 在可行时使用不可变状态
* 在架构边界处使用显式接口
* 小而聚焦的类
* 一个文件一个主要类
* 清晰的包归属
* 有意义的命名
* 使用协程，而非手工管理线程
* 结构化的错误处理

避免：

* 上帝类（God classes）
* 5000 行以上的文件
* UI 类中包含网络逻辑
* 全局可变状态
* 硬编码的域名列表
* 魔法数字
* 复制粘贴式的过滤逻辑

---

# 29. 错误处理

每一个系统组件都必须显式处理失败情况。

示例：

```text
VPN 授权被拒绝
VPN 初始化失败
DNS 失败
畸形 DNS 数据包
网络不可用
APK 解析失败
无效 APK
不支持的 APK 格式
规则导入失败
数据库失败
```

不要因为某个第三方 APK 畸形而导致应用崩溃。

不要因为某个数据包畸形而导致 VPN 崩溃。

---

# 30. 测试

需要为以下方面创建测试：

### 域名匹配

```text
精确匹配
子域名匹配
通配符
大写／小写
结尾点（trailing dot）
非法域名
白名单覆盖
```

### 规则引擎

```text
ALLOW
BLOCK
UNKNOWN
优先级
多个规则来源
```

### APK 分析

```text
单个 DEX
多 DEX
内嵌 URL
非法 URL
重复域名
畸形 APK
```

### DNS

```text
合法查询
畸形查询
被阻断域名
被放行域名
IPv4
IPv6
```

### 逐应用行为

验证：

```text
受保护应用 → 过滤生效
未受保护应用 → 绕过
```

---

# 31. 开发策略

增量式实现。

## 阶段 1

构建：

```text
Compose UI
+
已安装应用发现
+
VPN 授权流程
+
启动／停止 VPN
```

## 阶段 2

实现：

```text
逐应用路由
+
DNS 观测
```

## 阶段 3

实现：

```text
域名规则引擎
+
黑名单
+
白名单
```

## 阶段 4

实现：

```text
统计
+
网络活动 UI
```

## 阶段 5

实现：

```text
APK 静态分析
+
域名提取
```

## 阶段 6

实现：

```text
静态／运行时关联
```

## 阶段 7

优化：

```text
IPv6
QUIC 相关考量
性能
电量
大规模黑名单
```

不要试图同时推进所有阶段。

---

# 32. 重要的工程原则

永远不要声称：

> “这个域名是广告。”

除非有支持该分类的证据。

使用如下术语：

```text
Candidate（候选）
Observed（已观测）
Known tracker（已知追踪器）
Known advertising domain（已知广告域名）
Blocked by user rule（被用户规则阻断）
Blocked by imported rule（被导入规则阻断）
```

系统必须对“为什么某个请求被阻断”保持透明。

---

# 33. 初期不要构建的内容

不要从以下内容起步：

```text
HTTPS MITM
证书注入
TLS 解密
内存修改（Memory hacking）
Root 依赖
基于无障碍服务（Accessibility）的 UI 操纵
应用代码注入
应用修改
```

第一版产品应保持为一个干净的网络过滤系统。

---

# 34. 最终架构

预期的最终架构是：

```text
┌─────────────────────────────────────────────┐
│              Jetpack Compose UI             │
│                                             │
│ Home | Apps | Analysis | Rules | Statistics │
└──────────────────────┬──────────────────────┘
                       │
                       ▼
                ViewModel Layer
                       │
                       ▼
                 Domain Layer
                       │
        ┌──────────────┼───────────────┐
        │              │               │
        ▼              ▼               ▼
   App Manager    Rule Engine      APK Analyzer
        │              │               │
        │              │               ▼
        │              │          Domain Candidates
        │              │               │
        │              └───────┬───────┘
        │                      │
        ▼                      ▼
   PackageManager        Rule Database
                               │
                               ▼
                         Filtering Engine
                               │
                               ▼
                         Android VPNService
                               │
                               ▼
                         TUN Interface
                               │
                       ┌───────┴────────┐
                       ▼                ▼
                   Protected        Bypassed
                    Apps              Apps
                       │
                       ▼
                  DNS / Network
                    Metadata
                       │
                       ▼
                  Rule Evaluation
                  │             │
                  ▼             ▼
                BLOCK          ALLOW
```

---

# 35. Agent 行为规则

在修改本项目时：

1. 在创建新文件之前，先检查既有架构。
2. 不要重写无关组件。
3. 没有正当理由不要引入新依赖。
4. 不要伪造 Android API。
5. 不要发明不存在的 API。
6. 针对已配置的 SDK 验证 Android API 的可用性。
7. 网络代码与 UI 保持分离。
8. 过滤规则保持数据驱动。
9. 永不静默削弱 VPN 安全性。
10. 永不静默启用 HTTPS 拦截。
11. 除非明确要求，永不收集网络负载内容。
12. 永远不要用单一弱启发式规则来判定域名分类。
13. 修改规则引擎行为时，必须补充测试。
14. 在实现新功能时，保留既有功能。
15. 在引入重大基础设施之前，先说明架构上的取舍。

---

# 36. 完成定义（Definition of Done）

功能不是因为“能编译通过”就算完成。

对于网络类功能，需验证：

```text
构建成功
↓
应用可启动
↓
VPN 授权可用
↓
VPN 启动
↓
被选中的应用被正确路由
↓
未被选中的应用不受影响
↓
DNS 请求可被观测
↓
规则被执行评估
↓
匹配的域名被阻断
↓
被放行的域名继续正常工作
↓
统计数据准确
↓
VPN 可干净地停止
```

对于 APK 分析：

```text
APK 已导入
↓
Manifest 已解析
↓
DEX 文件已发现
↓
候选域名已提取
↓
域名已规范化
↓
重复项已移除
↓
结果已展示
↓
候选结果与已确认的运行时流量被清晰区分
```

系统必须优先保证**正确性与可验证的行为**，而不是功能数量。

---

# 37. 代码结构与数据结构（Code Structure & Data Structure）

## 37.1 文件与包

* 一个文件只放一个顶层公开声明；文件名必须与声明同名（`RuleEngine.kt` 里是 `RuleEngine`）。
* 只有构成同一个封闭层次的小型声明才可共用一个文件（例如一组同源的过滤事件类型 `FilteringEvents.kt`）。
* 包名小写、使用单数领域名，并与目录一一对应。根包 `xin.ctkqiang.nezha_cyber.ads_block` 由第 0.1 节固定；其下的子包一律使用小写单数、不含下划线的领域名（`...domain.rule`，而不是 `...domain.rule_engine`）。
* **禁止**创建 `utils`、`helpers`、`misc` 包。第 27 节中的 `core/common/` 只作为模块层级存在，其内部同样不得堆放无语义的通用工具类。
* **禁止**使用通配符导入（wildcard import）。

## 37.2 不可变性与数据结构

* 默认使用 `val`；`var` 只用于作用域局部、且可证明不会外泄的值。
* 跨越边界的集合必须不可变。
* 按语义选择数据结构：

| 声明 | 用途 |
| --- | --- |
| `data class` | 承载值（values） |
| `sealed interface` | 封闭层次（closed hierarchies） |
| `enum class` | 封闭标量集合 |
| `value class` | 身份（identity）与单位（units） |

* **禁止**在公开 API 中暴露可变集合。

## 37.3 可空性与错误

* `!!` 仅允许出现在紧邻其前、且已文档化的非空判断之后；其余场景一律禁止。
* **禁止**吞掉异常。要么有意义地处理，要么让它继续向上传播。
* 领域失败必须用封闭的结果类型显式建模。

```kotlin
/**
 * VPN 启动的结果。
 *
 * 用封闭类型而非异常建模：授权被拒绝属于预期内的领域结果，不是缺陷。
 */
sealed interface VpnStartResult {
    data object Started : VpnStartResult
    data object PermissionDenied : VpnStartResult
    data class Failed(val reason: VpnFailureReason) : VpnStartResult
}
```

## 37.4 函数

* 公开函数必须声明显式返回类型。
* 函数必须只做一件事；如果它的概述里需要出现「并且」，就必须拆分。
* 纯粹的单行表达式优先使用表达式函数体。
* 有副作用的函数必须用能揭示副作用的动词命名（`persistObservation`、`startVpnSession`）。
* **禁止**用布尔参数切换行为；改用两个命名清晰的函数，或一个封闭类型。

## 37.5 协程

* `suspend` 函数必须是主线程安全的：若内部存在阻塞操作，由它自己切到注入的 dispatcher 上执行。
* Dispatcher 必须注入，禁止直接引用，测试代码除外。
* **禁止**使用 `GlobalScope`。
* 必须使用结构化并发；被启动的协程必须归属于调用方持有生命周期的 scope。
* 跨边界暴露的 `Flow` 必须是冷的，且不得泄漏上游失败；失败必须映射到领域层的错误模型。

## 37.6 序列化与数据契约

* 序列化只出现在数据边界：规则文件的导入／导出、持久化记录。领域层内部不得出现序列化注解。
* 序列化字段名（`@SerialName`）是规则文件与持久化格式的契约；修改它必须伴随对应的格式版本号或数据库 schema 版本递增。
* **禁止**把 Android 平台类型（`PackageInfo`、`ApplicationInfo`、`Network`、`VpnService.Builder`）直接序列化或写入持久层；必须先在适配器边界映射为领域模型。

## 37.7 类内成员声明顺序

类体内的声明顺序是固定的。属性一律集中在类体顶部，位于任何函数之前 —— 包括 `private` 的成员。

```kotlin
class DomainRuleMatcher(
    private val ruleSource: RuleSource,
) {
    // 全部属性集中在类体顶部，位于任何函数之前
    private val compiledRuleCache = ConcurrentHashMap<String, CompiledRule>()
    private val lookupMutex = Mutex()
    private var lastReloadedAt: Instant = Instant.MIN

    suspend fun evaluate(domain: String, packageName: String): FilterDecision { /* ... */ }

    private fun compile(rule: DomainRule): CompiledRule { /* ... */ }
}
```

```kotlin
// 禁止：属性落在类底部，读函数时看不到对象的完整状态
class DomainRuleMatcher(
    private val ruleSource: RuleSource,
) {
    suspend fun evaluate(domain: String, packageName: String): FilterDecision { /* ... */ }

    private val compiledRuleCache = ConcurrentHashMap<String, CompiledRule>()
    private var lastReloadedAt: Instant = Instant.MIN
}
```

固定顺序：

1. 主构造函数注入的属性
2. 其余属性（含 `private`、`internal`）
3. 初始化块（`init`）
4. 次级构造函数
5. 公开函数
6. 私有函数
7. 嵌套类与 `companion object`

补充约束：

* 属性必须在类体内任何函数之前声明完毕。**禁止**把属性写在类底部，或夹在两个函数之间。
* 顶层（top-level）常量与扩展函数置于文件顶部、类声明之前；`const val` 使用 `SCREAMING_SNAKE_CASE`。
* `companion object` 若确实需要，置于类体末尾，不得出现在类体中部。

理由：属性散落会导致阅读方法时需要在类内来回跳转，对象状态也无法一次看清。

---

# 38. 依赖结构与禁止耦合（Dependency Structure）

## 38.1 依赖方向

```text
userInterface / feature  ──▶  domain  ◀──  data
```

* `domain` 是中心。它**禁止**依赖 Android SDK、Compose、Room、`VpnService`、`PackageManager`，也禁止依赖序列化库的运行时注解。
* `data` 实现 `domain` 声明的接口。
* `feature` 依赖 `domain` 与 `userInterface`。
* 应用模块是唯一允许把所有部件装配起来的地方。

## 38.2 端口与适配器

`domain` 声明端口（接口），`data` 提供适配器。

```kotlin
/**
 * 端口：已安装应用的来源。
 *
 * domain 只依赖这个抽象。PackageManager、包可见性配置与线程调度都属于
 * 适配器的职责。
 */
interface InstalledApplicationSource {
    suspend fun listInstalledApplications(): List<InstalledApplication>
}
```

```kotlin
/**
 * 端口：域名观测记录的存储。
 *
 * 实现负责持久化技术与事务语义；domain 不需要知道底层是 Room 还是内存假实现。
 */
interface DomainObservationStore {
    suspend fun appendObservations(observations: List<DomainObservation>)
    fun observeRecentObservations(limit: Int): Flow<List<DomainObservation>>
}
```

## 38.3 禁止耦合

* `domain` → `data`
* `domain` → `android` / `compose` / `room` / `vpnservice` / `packagemanager`
* 一个 Composable → 一个 Room `@Dao`，或 → 一个 `VpnService` 实例
* 一个 Android 平台类型（`PackageInfo`、`ApplicationInfo`、`Network`）→ 直接进入 `domain` 或持久层（必须先经适配器映射为领域模型）

---

# 39. 内置域名资源（assets/domains.txt）

本节细化第 11 节的黑名单内置规则来源，并约束它随应用分发的形式。该清单是持续增长的清单，不是一次性快照。

## 39.1 唯一来源与存放位置

* 内置域名清单的唯一存放位置是 `assets/domains.txt`，由应用直接引用。
* **禁止**把内置域名硬编码进 Kotlin 源文件（第 11 节、第 28 节）。
* 该文件随应用打包，运行时只读；**禁止**在运行时写回 assets。用户规则与用户的覆写状态只存于第 26 节的持久层。
* 清单纳入版本控制。

## 39.2 文件格式

```text
# 以 # 开始的行是注释
# 每行一个条目，忽略空行与首尾空白

ads.example.com
tracker.example.com
*.ads.example.com
```

| 约束 | 值 |
| --- | --- |
| 编码 | UTF-8 |
| 换行 | LF |
| 条目 | 每行一个域名 |
| 通配符 | 仅允许 `*.` 前缀，语义见第 8 节 |
| 大小写 | 统一小写化 |
| 结尾点 | 解析时剥离 |

* 条目不得包含协议、路径、端口或查询串。
* 非法行**不得**导致崩溃（第 29 节）：跳过该行，并以结构化日志记录行号与原因（第 23 节）。
* 文件缺失或不可读时，必须降级为「仅使用用户规则」，不得阻止应用启动。

## 39.3 版本与更新驱动

清单会持续扩充。没有版本号，已安装的用户将永远拿不到新增域名 —— 只在首次安装导入是不够的。

* 版本号声明在 `assets/domains.version`，内容为单行整数。
* 每次修改 `assets/domains.txt`（新增、删除或修正条目），必须在**同一次变更**中递增 `assets/domains.version`。
* 应用启动时比较「已导入版本」与当前版本：相同则跳过导入，不同则执行增量合并。

## 39.4 导入语义

* 内置条目的身份由 `source = builtin` 与域名共同唯一确定。
* 导入以域名为主键做 upsert。
* 合并时**必须**保留：用户显式放行、用户显式阻断、用户对某条内置规则的启用／禁用覆写、用户自定义规则。**禁止**用清单内容覆盖用户的显式规则（第 10 节）。
* 从清单中移除的域名：将其内置条目置为禁用，而非物理删除，以免切断既有观测记录与统计的关联。

## 39.5 加载与性能

* 读取与解析该资产必须在后台协程中完成，**禁止**在主线程执行（第 22 节、第 37.5 节）。
* 解析结果必须编译为高效查找结构（Trie 或哈希，第 22 节）；**禁止**在单次 DNS 查询中线性扫描清单。
* 清单增长后，单次查询的匹配耗时不得随条目数线性劣化。

## 39.6 与优先级和术语的关系

* 内置域名属于第 10 节中的「可信／系统规则」层，优先级低于用户显式放行与用户显式阻断。
* 命中内置清单时，决策来源标记为 `builtin`。
* **禁止**因为命中内置清单就在 UI 上断言「该域名是广告」；按第 32 节使用可解释措辞（例如「已拦截 — 内置规则」）。

---

# 40. 架构：MVVM

本节细化第 4 节的 UI 架构，并固定各层职责。第 34 节的最终架构图以本节为准。

## 40.1 单向数据流

```text
Composable → UiIntent → ViewModel → UseCase / Domain → Repository
Domain → Event / Result → UiState → Composable
```

* 状态必须不可变，并以单个 `StateFlow<XxxUiState>` 暴露。
* 一次性动作（导航、提示、跳转系统设置）必须建模为封闭的 `XxxUiEffect`，**禁止**塞进 state。
* ViewModel 的方法只接受 `UiIntent`，不接受零散的参数。

## 40.2 命名约定

| 类型 | 命名 | 示例 |
| --- | --- | --- |
| ViewModel | `XxxViewModel` | `ApplicationListViewModel` |
| 状态 | `XxxUiState` | `ApplicationListUiState` |
| 用户意图 | `XxxUiIntent` | `ApplicationListUiIntent` |
| 一次性效果 | `XxxUiEffect` | `ApplicationListUiEffect` |

三者构成同一个封闭层次，可共用一个文件（例如 `ApplicationListUiContract.kt`），这不违反第 37.1 节。

## 40.3 各层职责

| 层 | 职责 | 禁止 |
| --- | --- | --- |
| Composable | 渲染 `UiState`、派发 `UiIntent` | 业务逻辑、I/O、直接构造领域对象 |
| ViewModel | 把领域状态映射为 `UiState`、处理 `UiIntent`、发出 `UiEffect` | 业务规则、I/O、直接调用 Android API |
| UseCase / Domain | 业务规则、过滤决策、流程编排 | 依赖 Android、Compose、Room |
| Repository | 协调数据源、暴露 `Flow` | 持有 ViewModel 或 UI 状态 |

## 40.4 ViewModel 约束

* **禁止**在 ViewModel 中引用 `Activity`、`Context`、`View`、`NavController`。
* 需要平台能力时，通过注入的端口获取，而不是直接读 `Context`（第 38.2 节）。
* ViewModel 内的协程必须运行在 `viewModelScope` 中（第 37.5 节）。
* 领域错误必须映射为 `UiState` 的一部分，**禁止**让异常穿过 ViewModel 边界。

## 40.5 Compose 规则

* Composable 必须尽可能无状态；状态提升到 ViewModel。
* **禁止**硬编码颜色，必须从主题读取（第 0.2 节的红色主色）。
* **禁止**硬编码用户可见文本，必须从字符串资源读取。
* 每个页面必须提供浅色与深色两套配色方案的 `@Preview`。
* `LazyColumn` 的每一项必须提供稳定的 `key`。

---

# 41. 事件溯源规则

事件溯源是本项目观测记录与统计的骨干。本节规则不是风格偏好，违反它们会破坏正确性。

事件溯源的覆盖范围只有三样：**域名观测记录、过滤决策记录、用户在本应用内的本地操作**。第 41.8 节列出的内容一律不得事件溯源。

## 41.1 命令是意图，事件是事实

| | 命令（Command） | 事件（Event） |
| --- | --- | --- |
| 时态 | 祈使、现在 | 过去 |
| 含义 | 「执行这个」 | 「这已发生」 |
| 命名 | `BlockDomain` | `DomainBlocked` |
| 产生者 | UI 意图或用户操作 | 执行结果的权威来源 |
| 幂等键 | `OperationIdentifier` | `EventIdentifier` + `sequenceNumber` |

* **禁止**用命令给事件命名，也**禁止**用事件给命令命名。
* **禁止**由 UI 层伪造事件。UI 只能派发命令，事件只能由真正执行命令的那一层产生。

## 41.2 事件必须不可变

每个事件必须是实现 `sealed` 接口的不可变 `data class`。事件只使用 `val`，不暴露可变集合，除派生的只读辅助外不携带任何行为。

```kotlin
sealed interface DomainEvent {
    val eventIdentifier: EventIdentifier
    val sequenceNumber: Long
    val occurredAt: Instant
}
```

## 41.3 事件信封

每个被持久化或跨边界传递的事件必须携带此信封。字段名是持久化格式的契约。

```json
{
  "envelopeVersion": 1,
  "eventIdentifier": "event_01J",
  "sequenceNumber": 5003,
  "occurredAt": 1757660000000,
  "eventType": "domain.blocked",
  "aggregateType": "application",
  "aggregateIdentifier": "com.example.game",
  "payload": {}
}
```

## 41.4 事件存储只追加

* 只提供 `appendEvents` 与读取操作。没有 update，没有 delete。
* `eventIdentifier` 必须唯一。
* `sequenceNumber` 必须在同一来源内唯一且单调递增。
* 追加事件与推进其投影检查点必须在同一个事务内完成，使事件不会脱离投影存在，投影也不会声称自己取得了并未取得的进度。

## 41.5 重复写入与幂等

本机环境同样会产生重复：进程被系统回收、VPN 重启、导入流程重跑。

* 必须按 `eventIdentifier` 与 `sequenceNumber` 去重。
* `sequenceNumber` 出现空洞时必须显式对齐并记录，**禁止**静默跳过。
* 每条命令必须携带 `OperationIdentifier`。执行方必须把命令结果记入操作日志；重放同一条命令时必须返回已记录的结果，**禁止**重复执行破坏性动作（例如清空统计、批量禁用规则）。

## 41.6 归约器与投影

* 归约器必须是纯函数：`(State, Event) -> State`。无 I/O、不读时钟、无随机。
* 投影必须能通过从序列零重放事件日志完整重建。
* 投影**禁止**被视为事实来源。与事件日志冲突时，日志优先，投影重建。
* 每个投影必须持久化自己的检查点（`projectionName`、`lastSequenceNumber`、`updatedAt`）。

## 41.7 重放与快照

* 重放必须是一等公民，且被测试覆盖（第 30 节）。
* 快照是优化，不是设计要求。在测量证明必要之前，**禁止**引入快照。

## 41.8 不得事件溯源的内容

* Android 已安装应用列表 —— `PackageManager` 是权威来源（第 25 节）。
* VPN 隧道与网络栈的实时状态 —— 内核与 `VpnService` 是权威来源（第 7 节）。
* 规则清单本身 —— 第 39 节的 assets 与第 26 节的数据库是权威来源。
* 应用图标、标签等平台资源。

**禁止**试图用事件溯源去重建上述任一状态。它们只能作为事件的输入，不能作为事件的历史。

---

# 42. 可读性与留白

代码库本身是交付物。杂乱不是风格问题，它会直接造成误读。

## 42.1 格式化由构建门禁保证

* 缩进：Kotlin 使用 4 个空格；XML 与 JSON 使用 2 个空格（与第 39.2 节的资产格式一致）。
* 行尾：LF。缩进一律使用空格，**禁止** Tab。
* 单行长度上限统一由 `.editorconfig` 定义，**禁止**在个别文件里放宽。
* `ktlint` 负责格式与 import 排序；`detekt` 负责复杂度与禁止模式。
* 构建必须因违规而失败。格式问题不是评审意见，而是构建门禁。

```bash
./gradlew ktlintCheck detekt
./gradlew test
```

## 42.2 留白

* 类体内成员分组之间必须留一个空行：属性组之后、每个函数之间、嵌套类型之前。
* **禁止**连续出现两个及以上空行。
* **禁止**行尾空白；文件必须以单个换行符结束。
* **禁止**在函数体内用空行把逻辑切成多段 —— 需要分段说明时，说明该函数应当拆分（第 37.4 节）。

## 42.3 可读性

* 一个文件只承载一个概念。文件变长时先考虑拆类，而不是加注释（第 28 节）。
* 嵌套深度上限为 3 层；超过时必须用早返回或提取函数压平。
* 注释只写代码无法表达的原因、约束与陷阱。**禁止**复述代码、**禁止**保留注释掉的代码、**禁止**横幅装饰与分割线。
* 顶层声明的排列顺序：常量 → 类型别名 → 主要类型 → 扩展函数。阅读一个文件应当是一趟向下的行程。

## 42.4 禁止硬编码

* **禁止**硬编码颜色、用户可见文本与魔法数值。颜色来自主题（第 0.2 节）、文本来自字符串资源、可调数值来自具名常量。
* **禁止** `Impl` 后缀的类名；用具体技术命名（`RoomObservationStore`，而不是 `ObservationStoreImpl`）。

---

# 43. 提交信息规则

第 43.1 节由 `.githooks/commit-msg` 强制执行，两者必须同步修改 —— 任何一处单独改动都会让另一处失效。第 43.2 与 43.3 节目前只由评审保证。

## 43.1 形式

```text
<类型>(<范围>): <描述>

<正文>
```

| 项 | 约束 |
| --- | --- |
| 类型 | `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` `revert` |
| 范围 | `app` `core` `feature` `network` `analysis` `data` `rules` `build` |
| 主题长度 | 按字节不超过 100（中文一个字占 3 字节） |
| 主题结尾 | 不得以 `.` 或 `。` 收尾 |
| 正文 | 必须存在 |

* 需要新增范围时，必须同时修改本节与 `.githooks/commit-msg`。
* 正文使用中文；标识符、类型名与命令保持原文。

## 43.2 禁止写成操作步骤

**禁止**把提交信息写成操作教程或流水账。以下形式一律不允许：

```text
Step 1: 创建 assets 目录
Step 2: 添加 domains.txt
Step 3: 更新版本号
```

```text
步骤一：先建立规则文档
步骤二：再配置钩子
```

```text
1. 添加文件
2. 修改配置
3. 提交
```

提交历史记录的是**结果与理由**，不是**操作顺序**。

* 操作顺序属于文档与 PR 描述，不属于提交历史。后人回看某次提交时，关心的是「为什么这么改」「影响是什么」，不关心当时按什么顺序敲的。
* 需要分条时，按**主题**（文件、模块或关注点）分条，条目前使用 `-`，不使用数字序号。
* 读起来像教程的提交信息，通常说明这次改动的一句话结论还没被想清楚。先把结论想出来，再写正文。

## 43.3 正文应当写什么

正文交代三件事：

* 改动的目的 —— 为什么改。
* 改动的影响 —— 改变了什么行为，兼容性如何。
* 非显然的实现选择与取舍 —— 为什么是这个方案，而不是看起来更直接的那个。

不写：

* 复述 diff。「把 A 改成 B」这类内容，diff 自己会说。
* 操作步骤（第 43.2 节）。
* 自我评价与客套。「完美解决」「感谢」之类不承载信息。

示例：

```text
fix(network): 修复切换网络后丢失首个 DNS 响应的问题

隧道重建时未重新绑定保护套接字，网络切换后的第一个 DNS 响应会被写入已关闭的
通道并静默丢弃，表现为统计少记一次、规则评估被跳过。

改为在网络回调中显式重建通道，并让关闭路径先完成在途写入。旧行为无法通过
配置开关恢复，因为它记录的是不完整数据。
```

---

# 44. 应用专属广告清单与拦截通知

本节细化第 8、9、11 节，并新增「按应用生效」这一规则维度；它是第 39 节全局内置清单的补充，
不是它的替代。

## 44.1 为什么需要第二个维度

第 39 节的清单是全局的：命中即对所有应用生效。但有一类域名只应该在特定应用里被拦——
把它们放进全局清单会波及别的应用；完全不收又漏掉目标应用自己的广告。
因此引入「包名 + 域名」的规则维度，作用域由发起查询的应用决定。

作用域收窄是**安全取向**的选择：全局误拦会一次性影响整台设备，按应用收窄把影响面限制在
清单里写明的那几个应用上（第 32 节）。

## 44.2 存放位置与格式

* 唯一存放位置是 `assets/app_ads.txt`，随应用打包、运行时只读。**禁止**把条目硬编码进 Kotlin。
* 每行格式为 `<包名> <域名>`，两者之间以空白分隔。域名规则的通配符只允许 `*.` 前缀（第 39.2 节）。
* 编码 UTF-8、换行 LF；忽略空行与 `#` 注释；域名统一小写、解析时剥离结尾点。
* 包名必须至少两段；非法行**不得**导致崩溃，跳过并记录条数即可（第 29 节）。
* 文件缺失或不可读时降级为「该档不生效」并继续启动，不阻断应用启动。

## 44.3 版本与更新

本文件**不需要**版本号，也不导入持久层：它没有用户覆写，每次启动整体重新加载，
因此不存在第 39.3 节那种「已安装用户拿不到新增条目」的问题。需要新增域名时直接追加即可。

## 44.4 优先级

应用专属清单属「可信内置规则」，位于全局内置清单之后、关键词兜底之前：

```text
用户显式放行 → 用户显式阻断 → 全局内置清单 → 应用专属清单 → 关键词兜底
```

* 用户的显式放行仍然最高，因此误拦可以照常纠正（第 10 节）。
* 应用专属清单只提供阻断，与全局清单同样不提供「放行」档。

## 44.5 匹配与失败方向

* 匹配需要包名上下文。包名来自隧道层的连接归属反查；Android 10 以下没有该接口，取不到即为 null。
* **包名未知时不命中任何应用专属规则**，全局规则照常生效。这是刻意选择的失败方向：
  宁可漏拦，也不把某个应用的专属规则套用到身份不明的流量上。
* 匹配仍复用第 22 节的后缀索引，单次查询开销与规则条数无关。

## 44.6 拦截通知

* 命中阻断时发布一条汇总通知，说明「哪个应用请求了哪个域名、命中哪条规则」。
* 通知只陈述事实，**禁止**把域名断言成广告（第 32 节）。
* 通知不逐条推送：固定 id、固定节奏合并更新，避免高频拦截刷屏，也不给 DNS 热路径增加负担。
* 通知的发布与限流在独立协程中完成；热路径上的回调必须立刻返回（第 22 节）。
* 归属不可用时如实显示「未知来源」，不猜一个应用出来。
* 隧道停止时撤下该通知：它是「本次会话」的汇总，会话结束后继续留着只会造成误解。

## 44.7 条目的证据分级

清单条目必须按证据强度标注，标注写在条目所属的小节里。这不是文档礼节，而是防止读者把
「可能生效」当成「已经生效」。

* **已确认**：域名来自广告平台的公开文档或广泛使用的去广告规则集，且该应用确属对应生态。
* **待确认**：域名已确认是广告 SDK 域名，但「该应用是否接入」尚未逐项验证。

「待确认」条目允许收录，前提是它没有副作用：作用域只限左侧应用，该应用没接入该 SDK 时整条
规则不命中，等同于不存在。**禁止**把「待确认」写成「已确认」——那会让使用者以为拦截已经生效。

**禁止**为了凑数收录无法确认用途的域名。典型反例是同时承载商品图片或正文的主机：它上面
确实有广告路径，但在域名层面拦截会把正常内容一并拦掉。这类域名一条都不收，并在小节里
写明「不列的原因」，而不是留空让人以为是遗漏。

平台能力边界同样要在清单里写明，例如广告与核心业务共用同一批主机、或广告由应用自有平台
承载而无法在域名层面区分——写清楚「做不到」比列一条不会命中的规则有用得多。

