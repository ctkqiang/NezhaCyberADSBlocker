package xin.ctkqiang.nezha_cyber.ads_block.data.database

import android.content.Context
import androidx.room.Room

/** 数据库文件名。它是设备上持久化状态的一部分，改名等同于丢弃全部既有数据。 */
private const val DATABASE_NAME = "nezha.db"

/**
 * 建库入口。
 *
 * 目前只做一件事：按名字打开数据库。之所以单独留一个入口而不是让调用方直接写
 * `Room.databaseBuilder(...)`：迁移、关闭策略与预填充都是「怎么开库」的决策，
 * 它们应当集中在一处，否则将来加迁移时会出现「某个调用点忘了加」的情况。
 *
 * 第一次真正打开数据库的时机由容器决定，这里不做任何预热：打开数据库会读磁盘，
 * 不该发生在应用冷启动的主线程上。
 */
internal object NezhaDatabaseFactory {
    fun create(context: Context): NezhaDatabase = Room.databaseBuilder(
        context.applicationContext,
        NezhaDatabase::class.java,
        DATABASE_NAME,
    ).build()
}
