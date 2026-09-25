package xin.ctkqiang.nezha_cyber.ads_block.data

import java.io.File

/**
 * 应用私有文件的原子写入。
 *
 * 规则文件与统计计数是用户数据的唯一副本，写到一半掉电不应该让它们损坏。
 * 因此一律「先写同目录的临时文件，再改名」——同一文件系统内的改名是原子操作。
 *
 * 放在 data 包内共用而不是各自实现一份：这段逻辑一旦分叉，就会出现某条路径忘记改名的情况，
 * 而那正是数据损坏的来源。
 */
internal fun File.writeTextAtomically(content: String) {
    val temp = File(parentFile, "$name.tmp")
    temp.writeText(content)
    if (!temp.renameTo(this)) {
        temp.copyTo(this, overwrite = true)
        temp.delete()
    }
}

internal fun File.writeLinesAtomically(lines: List<String>) {
    writeTextAtomically(lines.joinToString(separator = "\n", postfix = "\n"))
}

internal fun File.readIntOrZero(): Int = if (exists()) readText().trim().toIntOrNull() ?: 0 else 0
