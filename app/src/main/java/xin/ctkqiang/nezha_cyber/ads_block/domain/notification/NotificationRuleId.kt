package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 通知拦截规则的身份。
 *
 * 用 value class 而不是裸 `Long`：规则的 id、观测记录的 id、应用的 uid 在形态上都是数字，
 * 混用时编译器不会报错，要到运行时才发现拿错了对象。包一层之后这类错误在编译期就消失
 * （工程规则第 37.2 节：`value class` 用于身份与单位）。
 *
 * 值由持久层生成。尚未落库的规则不构造这个类型——创建流程以「包名 + 匹配文字」为输入，
 * 落库后由持久层回填身份，因此不存在「id 未知」的中间状态需要表达。
 */
@JvmInline
value class NotificationRuleId(val value: Long)
