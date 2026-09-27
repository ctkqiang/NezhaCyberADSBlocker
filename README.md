# 哪吒反广 · Nezha Cyber ADS Blocker
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org) [![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android)](https://developer.android.com) [![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.06-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose) [![Version](https://img.shields.io/badge/Version-0.1.0-red?style=flat-square)](https://github.com/ctkqiang/NezhaCyberADSBlocker/releases) [![开源项目](https://img.shields.io/badge/%E5%BC%80%E6%BA%90-%E9%A1%B9%E7%9B%AE-red?style=flat-square)](https://github.com/ctkqiang/NezhaCyberADSBlocker) [![Made in China](https://img.shields.io/badge/Made%20in%20China-red?style=flat-square)]() [![Last commit](https://img.shields.io/github/last-commit/ctkqiang/NezhaCyberADSBlocker?style=flat-square)](https://github.com/ctkqiang/NezhaCyberADSBlocker/commits/main) [![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](https://github.com/ctkqiang/NezhaCyberADSBlocker/pulls)
**逐应用广告与追踪拦截 | Per-App Ad & Tracker Blocker**
_一个基于 Android `VpnService` 的本地 DNS 过滤应用，把广告与追踪器挡在域名解析这一层_
**哪吒反广**用系统 VPN 能力建一条只接管**域名解析**的隧道，让被选中的应用的 DNS 查询先经过本机规则
引擎再决定转发还是拦截。拦截在解析层完成，因此不碰 TLS、不装证书、不做中间人，也不会把用户的
正常流量绕道。
不收集、不上报、不出售任何用户数据，不含遥测与统计 SDK；所有规则、观测与统计只存在本机，随应用
一同卸载。不是 HTTPS MITM 代理，第一版也不打算是。
<div align="center">
  <img src="assets/logo.png" alt="哪吒反广 · NezhaCyberADSBlocker" width="240"/>
</div>
---
## 目录
- [法律声明](#法律声明)
- [项目定位](#项目定位)
- [与其他方案的区别](#与其他方案的区别)
- [核心功能](#核心功能)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [构建与安装](#构建与安装)
  - [两个构建变体](#两个构建变体)
  - [首次授权](#首次授权)
- [过滤引擎](#过滤引擎)
  - [规则优先级](#规则优先级)
  - [内置域名清单](#内置域名清单)
  - [应用专属清单](#应用专属清单)
  - [拦截应答方式](#拦截应答方式)
- [技术架构](#技术架构)
  - [隧道设计](#隧道设计)
  - [事件溯源](#事件溯源)
  - [分层与目录结构](#分层与目录结构)
- [桌面小组件](#桌面小组件)
- [隐私与安全设计](#隐私与安全设计)
- [主题系统](#主题系统)
- [开发指南](#开发指南)
- [常见问题](#常见问题)
- [贡献](#贡献)
- [许可证](#许可证)
- [支持](#支持)
---
## 法律声明
> **本应用是网络过滤工具，只在本机处理域名解析，不采集、不上报、不出售任何用户数据。** **拦截可能影响个别应用的正常功能（例如其广告与业务共用同一批主机时），请自行评估并善用白名单。** **内置域名清单来源于公开去广告规则集，不保证收录全部广告域名，也不对任何域名作出「这是广告」的断言。** **使用者需自行承担因使用本应用产生的一切后果。**
---
## 项目定位
哪吒反广是一款**逐应用网络防火墙 + 广告／追踪器规则引擎**，面向希望在不改装系统、不获取 root
的前提下减少应用广告与追踪请求的普通用户。

它工作的位置很关键——不在页面里，而在**域名解析**这一层：
```
被选中的应用 → DNS 查询 → 本机隧道 → 规则引擎 → BLOCK / ALLOW → 上游解析
```
拦截发生在应用拿到 IP 地址之前。因此它对 HTTPS、HTTP/2、HTTP/3 一视同仁，也不需要理解任何
加密内容；代价是它**只能按域名拦**，无法区分同一域名下正常的与广告的路径。

它**不是**单纯的 APK 字符串扫描器。系统用两个互补来源判断「谁在联网、去了哪里」：
```
APK 静态分析（候选端点）
        +
运行时网络观测（实际请求）
        ↓
候选域名情报
        ↓
规则引擎
        ↓
逐应用过滤
```
静态分析负责发现潜在端点，运行时观测负责判定应用实际与哪些域名通信。**任何单一来源都不自动把
某个域名归类为广告**——命名里带 `ads`／`tracker` 只是指示信号，不构成证据。
---
## 与其他方案的区别
| 能力 | 哪吒反广 | 系统「私人 DNS」 | hosts + root | 传统全局 VPN 去广告 |
|------|---------|-----------------|-------------|-------------------|
| 逐应用生效 | 是（`addAllowedApplication` 按包名路由） | 否（全局） | 是 | 多数否 |
| 无需 root | 是 | 是 | 否 | 是 |
| 只接管 DNS、不转发全流量 | 是（仅一条自造 DNS 地址的 /32 路由） | 是 | 是 | 否（全流量过隧道） |
| 拦截可解释（命中哪条规则） | 是 | 否 | 否 | 部分 |
| 自身白名单 / 黑名单 | 是（用户规则优先级最高） | 否 | 需手改 hosts | 部分 |
| APK 域名静态分析 | 是 | 否 | 否 | 否 |
| 被拦通知 + 通知拦截 | 是 | 否 | 否 | 部分 |
| 桌面小组件 | 10 种 | 否 | 否 | 否 |
| 数据不出设备 | 是 | 取决于上游解析商 | 是 | 取决于实现 |
| 需要装证书 / MITM | 否 | 否 | 否 | 否 |
---
## 核心功能
### 逐应用保护
- 「应用」页列出设备上全部可联网应用，逐个开关保护状态
- 保护状态改变**隧道 → 规则**双层：路由在建立隧道时由 `addAllowedApplication` 固定
- 应用列表展示应用名、包名、图标、保护状态与运行时权限侵入度；观测与拦截计数来自**最近观测窗口**，界面文案如实标注「最近」，不假装是跨会话累计
- 未受保护的应用完全绕开隧道，其网络行为不受任何影响
### 域名规则引擎
- 决策集中在一个引擎里，过滤逻辑不散落在代码各处的 `if (domain == ...)`
- 支持精确匹配、子域名匹配、`*.` 前缀通配符，大小写不敏感
- 后缀索引（Trie／哈希）做匹配，单次查询耗时与规则条数无关
- 每条决策携带命中规则与来源，供日志、统计与通知解释「为什么被拦」
### 两级内置清单
- **全局清单**：`assets/domains.txt`，随应用打包，命中即对所有应用生效
- **应用专属清单**：`assets/app_ads.txt`，`<包名> <域名>` 格式，只对左侧应用生效
- 两者都是只读资产，不硬编码进 Kotlin；用户规则优先级最高，可覆盖任意一条
### 通知拦截（full 变体）
- 按应用 + 匹配文字建立通知拦截规则，命中的通知会被替换为带标记的替代通知
- 替代通知保留原应用、命中规则与原通知内容，并如实说明「可读取的文本为空」而非编造
- 拦截发生在通知**已发布之后**：平台没有 API 能阻止应用生成通知，只能改变其在通知栏的去向
### APK 静态分析
- 解析 APK 的 Manifest、DEX、资源与 Assets，提取 URL 与类域名字符串
- 处理多 DEX（`classes.dex`／`classes2.dex`／……），不假设只有一个
- 域名规范化、去重、剔除非法与本地路径，产出**候选**端点
- 静态候选与运行时实际观测分别标注，不把候选当作已确认请求
### 统计与网络活动
- **跨会话累计**（保存在本机）：DNS 请求总数、已拦截、已放行、拦截占比
- **本次会话**：拦截域名数（跨会话去重需长期保存全部域名集合，因此重启归零，界面如实标注）
- **规则**：内置清单版本与条数、用户规则数
- **最近观测窗口**：拦截最多的域名、按应用的计数与高频域名；归属到应用需要系统连接归属查询（Android 10+），更低版本一律显示「未知来源」
- 「网络活动」页回答「哪个应用在请求什么」，与本站点列表的答案一致
- 严格区分 observed（已观测）／blocked（已拦截）／allowed（已放行）；**放行 ≠ 用户放行**，绝大多数放行只是没有规则命中，界面不把观测域名称作广告
### 隐私与传感器审计
- 列出**当前已授予**敏感权限的应用，权限名沿用系统标签，与系统设置保持一致
- 一键跳转该系统信息页，由用户自己处置；并如实说明关闭传感器的唯一官方路径
- 不提供任何不能真正生效的开关（原因见「隐私与安全设计」）
### 品牌化通知
- 三条通知（隧道常驻 / 拦截记录 / 通知拦截）共用同一套视觉语言
- `setSmallIcon` 只能用单色剪影，彩色应用 logo 走 `setLargeIcon` 与自定义内容视图
- 拦截记录**逐条**成条、通知栏上限 20 条，超出淘汰最早的，完整流水保留在应用内
---
## 快速开始
### 环境要求
- JDK 17（`sourceCompatibility` / `jvmTarget` 均为 17）
- Android 手机 API 26（Android 8.0）及以上
- Android SDK 36（`compileSdk` / `targetSdk` 均为 36）
- 构建工具由 Gradle Wrapper 自带，无需另装
### 构建与安装
```bash
# 质量门禁（格式与复杂度违规会让构建失败）
./gradlew ktlintCheck detekt

# 单元测试
./gradlew test

# 打包
./gradlew assembleFullDebug       # 产物 app/build/outputs/apk/full/debug/
./gradlew assembleStandardDebug   # 产物 app/build/outputs/apk/standard/debug/

# 安装（installDebug 是 installFullDebug 的别名）
./gradlew installDebug
./gradlew installStandardDebug
```
Release 签名从 `local.properties` 或环境变量读取，绝不硬编码：
```properties
key.store=app/nezha-release.jks
key.storePassword=...
key.alias=...
key.keyPassword=...
```
未配置时 release 构建回退到 debug 签名，便于本地验证；发布前必须配置正式密钥库。
### 两个构建变体
`capability` 维度只有一处差异，且**只由清单决定，不改动任何源码**：

| 变体 | 通知监听服务 | 通知拦截功能 | 互联网来源侧载 |
|------|------------|------------|--------------|
| `standard` | 不声明 | 关闭（导航入口隐藏） | 可安装（不命中 Play Protect 清单） |
| `full` | 声明 | 完整可用 | 需先关闭「Play 保护机制」 |

关键点：Google Play Protect 对互联网来源侧载的 APK 有一份自动拦截清单，声明
`BIND_NOTIFICATION_LISTENER_SERVICE` 即被直接拒绝安装。这份拦截**只看 manifest 声明**，与
APK 内存在哪些类无关，因此把该 service 隔离到 `full` 专属清单，就能得到一个可侧载的 `standard`
包，而过滤引擎、规则资产与十个小组件在两个变体里共用同一份实现。
### 首次授权
1. 安装后打开应用，点「开启保护」
2. 系统弹出 VPN 授权对话框，选择**允许**（应用无法绕过这一步）
3. Android 13+ 会请求通知权限——未授予时前台服务的状态通知不会显示
4. 到「应用」页逐个打开需要保护的应用；未开启的应用不受影响
5. 隧道常驻通知里可随时点「停止」，无需回到应用内
---
## 过滤引擎
### 规则优先级
顺序是确定性的，用户显式规则永不被静默覆盖：
```
用户显式放行（Allow）
        ↓
用户显式阻断（Block）
        ↓
全局内置清单（builtin）
        ↓
应用专属清单（app_ads）
        ↓
关键词兜底（keyword）
        ↓
未知（Unknown）
```
- 决策结果只有三种：`ALLOW` / `BLOCK` / `UNKNOWN`
- 命中内置清单时来源标记为 `builtin`，界面**不因此断言「该域名是广告」**，只说「已拦截 — 内置规则」
- 关键词兜底位于最后一档，只在用户规则与内置清单都没命中时执行
### 内置域名清单
| 项 | 值 |
|----|----|
| 存放位置 | `assets/domains.txt`（唯一来源，运行时只读，76,211 条） |
| 版本文件 | `assets/domains.version`（单行整数，当前 `2`） |
| 上游 | StevenBlack/hosts（MIT 许可），转换自 hosts 格式 |
| 格式 | 每行一个域名，UTF-8 / LF，`#` 注释，通配符仅 `*.` 前缀 |
| 更新语义 | 版本号变化才做增量合并；域名做主键 upsert |
| 用户覆写 | 合并时保留用户放行／阻断／启用停用覆写，禁止用清单覆盖用户显式规则 |
| 移除处理 | 清单里删除的域名置为禁用而非物理删除，以免切断既有观测关联 |
清单开头有一段 **HTTPDNS 拦截**：国内应用普遍绕过系统解析、直接用 HTTPS 换回 IP，这样建立的
连接在域名层完全看不到，因此这一档不是「又一批广告域名」，而是让整个域名规则生效的前置条件。
### 应用专属清单
| 项 | 值 |
|----|----|
| 存放位置 | `assets/app_ads.txt`（只读，286 行，含注释） |
| 格式 | 每行 `<包名> <域名>`，空白分隔 |
| 版本号 | 无需（不导入持久层、无用户覆写，每次启动整体重载） |
| 匹配前提 | 需要包名上下文（隧道层连接归属反查，Android 10 以下取不到即为 null） |
| 失败方向 | 包名未知时**不命中任何应用专属规则**，全局规则照常生效——宁可漏拦，也不套用到身份不明的流量 |
| 证据标注 | 条目按「已确认」／「待确认」分级，标注写在所属小节里 |
覆盖的典型应用包括微博、淘宝、高德地图、百度地图、微信、腾讯地图、应用宝、抖音、懂车帝、
拼多多等。**禁止为凑数收录无法确认用途的域名**——当一个主机同时承载商品图片或正文时，在域名层
拦截会把正常内容一并拦掉，这类域名一条都不收，并在小节里写明「不列的原因」。
### 拦截应答方式
命中规则时回给客户端的本地应答可在设置里选择，三种都在本机合成，都不向上游发起查询：

| 方式 | 语义 | 取舍 |
|------|------|------|
| `NxDomain`（默认） | 域名不存在（RCODE 3） | 最标准；个别客户端可能因此改试备用域名 |
| `Refused` | 本解析器拒绝（RCODE 5） | 不会被误读成域名失效 |
| `ZeroAddress` | 返回 A 记录 `0.0.0.0` | hosts 屏蔽的通用写法，连接立刻失败 |

响应方式只影响被拦截的域名，放行的域名一律原样转发。
---
## 技术架构
### 隧道设计
```
        Android 应用
             │
             ▼
        VpnService（前台服务）
             │
             ▼
     TUN 接口（自造 DNS 地址 /32）
             │
      ┌──────┴──────┐
      ▼             ▼
  受保护应用      未受保护应用
      │             │
      ▼             ▼
  DNS 解析      直接联网（完全绕开）
      │
      ▼
   规则引擎
   ┌──┴──┐
   ▼     ▼
 BLOCK  ALLOW ──→ 受保护 socket 转发到真实上游 DNS
```
三处关键设计：
- **隧道只捕获一个自造的 DNS 地址**（`10.111.222.2/32`），它只存在于隧道内。因此隧道捕获的流量
  在结构上不可能与其它应用重叠，也不会像「路由真实 DNS 服务器」那样把发往那台机器的 TCP 53、
  NTP、门户认证一并吸进隧道再丢掉。
- **上游解析走受保护 socket**：中继绕过隧道，直接问隧道之外那张网的 DNS 服务器。
- **MTU 取 1500**，避免为 DNS 这种小包做分片；超过单个 MTU 的应答直接丢弃由客户端重试，不做分片。
换网时只把新的上游地址推给中继、**不重建隧道**——隧道里唯一被捕获的地址是本应用自造的，与具体
网络无关。隧道停止后不会自动重启（`START_NOT_STICKY`），用户关掉就是关掉。
### 事件溯源
域名观测、过滤决策与用户在本应用内的本地操作按事件溯源建模，事件只追加、不改不删：
- 事件是不可变 `data class`，携带信封：`eventIdentifier` / `sequenceNumber` / `occurredAt` / `eventType` / `aggregateType` / `aggregateIdentifier` / `payload`
- 命令（`BlockDomain`）与事件（`DomainBlocked`）严格区分：命令是意图，事件是事实，UI 只能派发命令
- 归约器是纯函数 `(State, Event) -> State`，投影可从序列零重放完整重建，冲突时以事件日志为准
- 追加事件与推进投影检查点在同一事务内完成
- **不事件溯源**：已安装应用列表（`PackageManager` 是权威）、隧道实时状态（`VpnService` 是权威）、
  规则清单本身（assets 与数据库是权威）、应用图标与标签等平台资源
### 分层与目录结构
依赖方向单向：`feature → domain ← data`。`domain` 是中心，不依赖 Android SDK、Compose、Room、
`VpnService`、`PackageManager`；`data` 实现 `domain` 声明的端口；应用模块是唯一装配所有部件的地方。
UI 采用 MVVM 单向数据流：`Composable → UiIntent → ViewModel → UseCase/Domain → Repository`，
状态以单个 `StateFlow<XxxUiState>` 暴露，一次性动作建模为封闭的 `XxxUiEffect`。
```
NezhaCyberADSBlocker/
├── app/src/main/java/.../ads_block/
│   ├── MainActivity.kt / NezhaApplication.kt   # 入口
│   ├── AppContainer.kt                          # 依赖装配
│   ├── data/                                    # 适配器：实现 domain 的端口
│   │   ├── database/       # Room（DAO、实体、迁移）
│   │   ├── observation/    # 域名观测仓储
│   │   ├── rule/           # 规则仓储与资产解析
│   │   ├── application/    # PackageManager 适配、保护状态、权限来源
│   │   ├── notification/   # 通知拦截规则仓储
│   │   ├── privacy/        # 隐私策略与保留策略
│   │   └── appearance/     # 主题偏好
│   ├── domain/                                  # 纯领域：实体、端口、决策
│   │   ├── rule/           # RuleEngine、后缀索引、HostNormalizer、关键词策略
│   │   ├── analysis/       # APK 分析端口与结果模型
│   │   ├── application/    # 应用来源端口、保护状态端口
│   │   ├── observation/    # 观测模型与端口
│   │   ├── notification/   # 通知拦截模型
│   │   ├── privacy/        # 隐私策略、敏感权限清单
│   │   └── vpn/            # 会话状态、失败原因、控制器端口
│   ├── feature/                                 # 每个页面一个 ViewModel + UiContract
│   │   ├── home/ application/ analysis/ network/
│   │   ├── rule/ notification/ statistic/ setting/ privacy/
│   ├── network/vpn/                             # VpnService、DNS 解析、中继、连接归属
│   ├── notification/platform/                   # 通知监听服务与通知构造
│   ├── ui/                                      # Compose：主题、组件、导航、页面
│   └── widget/                                  # 十个桌面小组件
├── app/src/full/AndroidManifest.xml             # full 变体专属：通知监听服务
├── app/src/main/assets/                         # domains.txt / app_ads.txt / domains.version
├── app/src/test/                                # 单元测试
├── config/detekt/detekt.yml                     # 静态检查配置
└── .githooks/                                   # commit-msg 与 pre-commit 守卫
```
---
## 桌面小组件
十种小组件各回答**一个不同的问题**，而不是把同一个数字换十种样式：

| 小组件 | 回答的问题 |
|--------|-----------|
| 保护开关 | 保护开了吗（可直接切换） |
| 拦截计数 | 一共拦了多少 |
| 拦截占比 | 拦截占总请求的多少 |
| 最近拦截 | 最近拦下了什么 |
| 受保护应用 | 护住了几个应用 |
| 观测请求 | 一共看了多少流量 |
| 规则规模 | 生效规则有多少条 |
| 拦截排行 | 谁最常被拦 |
| 拦截趋势 | 是变多了还是变少了 |
| 保护概览 | 想直接去哪里 |

小组件从本地快照读取（不依赖后台服务常驻），应用内的改动经由定向广播 `WIDGET_REFRESH` 让桌面跟上。
---
## 隐私与安全设计
| 措施 | 实现 |
|------|------|
| 不收集数据 | 无遥测、无埋点、无统计 SDK，不含任何上报通道 |
| 只存元数据 | 观测仅保存包名、域名、动作、命中规则与时间，不保存请求体、Cookie、令牌或消息内容 |
| 记录可关 | 关闭「观测记录」后不再记录，但**过滤与拦截照常工作**（过滤只依赖规则引擎） |
| 保留可调 | 三档：Minimal（50 / 500）、Standard（200 / 2000）、Extended（1000 / 10000） |
| 记录可清 | 「清除统计」是独立动作，与「停止记录」分开，避免只想停止收集时丢掉已有诊断数据 |
| 不可信输入 | DNS 报文、域名、IP、长度、协议字段、APK 内容、导入规则一律校验；畸形包不崩溃、不触发无界分配 |
| 静态分析保持静态 | 只读取 APK，**绝不执行**从中提取的任何代码 |
| 无 MITM | 不装证书、不做 TLS 拦截、不修改响应，因此不存在证书固定与隐私风险 |
| 最小权限 | 只声明 VPN、前台服务、通知、网络状态与包可见性所需权限 |
| 数据仅在本机 | 观测、统计与日志默认只存在本机，且可被用户清除 |
**能力边界必须说清**（对应「隐私与传感器」页）：本应用**不能**关闭陀螺仪、加速度计等运动传感器
（平台根本没有为它们设权限），**不能**修改其它应用的授权状态（仅设备所有者可改，且 Android 12
起对传感器相关权限的撤销本身被系统限制），**不能**调用系统的「传感器已关闭」（背后是
`SensorPrivacyManager`，`@SystemApi`，普通应用无权调用）。因此该页只提供**审计 + 跳转 + 引导**，
绝不提供点了没反应的死开关——「点了没反应」比「没有这个开关」更有害，用户会以为已经被保护了。
---
## 主题系统
主色取自应用图标实测统计，品牌红贯穿全局：

| 令牌 | 浅色 | 深色 |
|------|------|------|
| `brand`（品牌红） | `#CC1F33` | `#FF2640` |
| `onBrand` | `#FFFFFF` | `#FFFFFF` |
| `background` | `#F7F7F9` | `#040507`（与图标底色同值） |
| `surface` | `#FFFFFF` | `#121216` |
| `surfaceElevated` | `#EDEDF1` | `#212128` |
| `textPrimary` | `#17171A` | `#F2F2F4` |
| `textSecondary` | `#5A5A63` | `#A6A6B0` |
| `outline` | `#E2E2E6` | `#2A2A31` |

- 主题三档：跟随系统（默认）／始终浅色／始终深色
- 全部前景／背景组合按 WCAG AA 校验（正文与标签 ≥ 4.5:1，主按钮大字 ≥ 3:1），由单元测试锁住
- 悬浮底栏使用**液态玻璃**：实时取样背景、半透明表面 + 顶部高光 + 边缘内阴影，选中项再叠一层玻璃胶囊
- 颜色一律从主题读取，文本一律从字符串资源读取，不写死（否则构建门禁会失败）
---
## 开发指南
### 技术栈
| 层 | 选型 |
|----|------|
| 语言 | Kotlin 2.4.20（`jvmTarget` 17） |
| UI | Jetpack Compose（BOM 2026.06.01）+ `activity-compose` + `lifecycle-viewmodel-compose` |
| 持久化 | Room 2.8.5（KSP 生成，schema 导出并纳入版本控制） |
| 异步 | Kotlin Coroutines / Flow（Dispatcher 注入，禁止 `GlobalScope`） |
| 网络 | Android `VpnService` + 平台网络 API + `PackageManager` |
| 架构 | MVVM + 单向数据流 + 事件溯源 |
| 构建 | Gradle Kotlin DSL + AGP 9.3.3 |
| 质量门禁 | ktlint + detekt（格式与复杂度违规直接让构建失败） |
### 质量门禁与测试
```bash
./gradlew ktlintCheck detekt   # 格式与复杂度：不是评审意见，而是构建闸门
./gradlew test                 # 单元测试
./gradlew assembleFullDebug    # 完整构建
```
测试覆盖：域名匹配（精确／子域名／通配符／大小写／结尾点／非法）、规则引擎（ALLOW/BLOCK/UNKNOWN、
优先级、多来源）、APK 分析（单 DEX／多 DEX／内嵌 URL／非法 URL／重复／畸形）、DNS（合法／畸形／
被拦／放行）、逐应用行为（受保护生效／未受保护绕过）、主题对比度。
### 代码约定
| 项目 | 要求 |
|------|------|
| 文件粒度 | 一个文件一个顶层公开声明，文件名与声明同名 |
| 属性顺序 | 类体内属性集中在顶部、任何函数之前（含 `private`） |
| 命名 | 包名小写单数领域名；禁止 `Impl` 后缀，用具体技术命名 |
| 空值 | 禁止裸 `!!`；禁止吞异常；领域失败用封闭结果类型建模 |
| 协程 | `suspend` 必须主线程安全；Dispatcher 注入；结构化并发 |
| 禁止 | 禁止 `utils`／`helpers`／`misc` 包、禁止通配符导入、禁止硬编码域名与颜色 |
| 提交粒度 | 一个逻辑改动一个提交，不把格式化改动与行为改动混在一起 |
### 提交信息
由 `.githooks/commit-msg` 强制，形式为 `<类型>(<范围>): <描述>`：

| 项 | 取值 |
|----|------|
| 类型 | `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` `revert` |
| 范围 | `app` `core` `feature` `network` `analysis` `data` `rules` `build` |
| 主题 | 按字节不超过 100；不得以 `.` 或 `。` 收尾 |
| 正文 | 必须存在，写清**为什么改、影响是什么、非显然的取舍**；禁止写成操作步骤或复述 diff |
---
## 常见问题
**Q: 开启保护后某些应用出问题？** A: 把它加入白名单，或在「应用」页关掉它的保护。白名单优先级最高。
**Q: 为什么还是偶尔看到广告？** A: 三处常见原因——应用走 HTTPDNS 绕过系统解析、广告与业务共用同一主机
（域名层拦会伤到正文，因此不拦）、或广告由应用自有平台承载。清单里都写明了这些边界。
**Q: 通知拦截会阻止应用发通知吗？** A: 不会。平台只允许本应用在通知**已发布之后**接管它在通知栏的去向，
无法阻止应用生成通知。展开被拦截的通知可以看到原应用、命中规则与原内容。
**Q: 会看到我的浏览记录吗？** A: 应用只保存域名、动作、命中规则与时间的**元数据**，不保存任何载荷内容；
这些数据只在本机，可随时关闭记录或清除。
**Q: 需要装证书或越狱吗？** A: 都不需要。第一版不做 HTTPS MITM、不依赖 root、不使用无障碍服务。
**Q: standard 和 full 装哪个？** A: 只需要域名过滤用 `standard`（可任意来源侧载）；需要通知拦截用
`full`（从浏览器等来源安装前需先关闭「Play 保护机制」）。
**Q: 支持 IPv6 吗？** A: 当前隧道按 IPv4/UDP:53 处理，IPv6 与 QUIC 相关考量属于后续优化项，尚未覆盖。
---
**如果这个工具帮到了你，请给它一个星标！**
**为人民而做，由人民共建**
---
## 贡献
欢迎提交 Issue 与 Pull Request。动手之前请先读 [`.trae/rules/project_rules.md`](.trae/rules/project_rules.md)
——命名、注释、分层依赖、事件溯源与提交规范都在里面，它与个人偏好冲突时以它为准。

约定：

| 项目 | 要求 |
|------|------|
| 环境 | JDK 17；`compileSdk` / `targetSdk` 36 |
| 静态检查 | 本地先跑 `./gradlew ktlintCheck detekt`，违规会让 CI 失败 |
| 提交信息 | 见上文「提交信息」，由 `.githooks/commit-msg` 强制 |
| 提交粒度 | 一个逻辑改动一个提交，格式化与行为改动分开 |
| 依赖 | 新增依赖前先看 `gradle/libs.versions.toml` 有没有可复用的，并说明取舍 |
| 规则资产 | 修改 `domains.txt` 必须同一变更内递增 `domains.version`；补 `app_ads.txt` 需按证据分级标注 |
| 禁止 | 不提交构建产物、密钥库、`local.properties` 或任何本地配置；不引入遥测与统计 SDK |

提 Issue 请附上应用版本、Android 版本与复现步骤；涉及拦截行为时，请一并给出被拦域名与命中的规则。
---
## 许可证
本项目是**开源项目**，可自由使用、修改与分发；作者与来源信息保留在 `README`、许可证与应用的
「关于开发者」页中。内置域名清单来源于 [StevenBlack/hosts](https://github.com/StevenBlack/hosts)
（MIT 许可），由上游 hosts 格式转换为每行一个域名。
本项目仅供**合法用途**。拦截可能影响个别应用的正常功能，请自行评估并善用白名单——见文首
[法律声明](#法律声明)。
---
<div align="center">
<h2>支持</h2>
<p>如果您觉得本项目对您有帮助，欢迎 Star / Fork，也欢迎请我喝杯咖啡</p>
<p><sub>您的支持是我持续维护和改进的动力</sub></p>

<strong>微信扫码捐赠</strong>

<img src="https://raw.gitcode.com/ctkqiang_sr/ctkqiang_sr/raw/main/mm_reward_qrcode_1778988737577.png"
     alt="微信扫码捐赠"
     width="240"
     style="border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.15);" />
</div>
