package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

/**
 * 观测记录的保留档位。
 *
 * 做成三档而不是一个任意数字输入框：任意数字需要输入校验、越界处理，还要由界面解释
 * 「多大算合适」，而用户真正要回答的只是「愿不愿意留记录、留多少」。
 *
 * [capacity] 是实时流水保留的条数，[logLineLimit] 是磁盘日志的行数上限。
 * 两者必须一起变：只调其中一个会让内存里的窗口与磁盘上的文件大小脱节，
 * 出现「界面上只有 50 条，磁盘上却躺着 2000 行」这种与用户预期相反的状态。
 */
enum class ObservationRetention(val capacity: Int, val logLineLimit: Int) {
    /** 只留够看清「刚刚发生了什么」的条数。 */
    Minimal(capacity = 50, logLineLimit = 500),
    Standard(capacity = 200, logLineLimit = 2000),
    Extended(capacity = 1000, logLineLimit = 10000),
}
