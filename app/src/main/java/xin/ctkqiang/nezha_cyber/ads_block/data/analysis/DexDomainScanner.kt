package xin.ctkqiang.nezha_cyber.ads_block.data.analysis

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.DomainCandidate
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.HostNormalizer

private const val DEX_MAGIC = "dex\n"

private const val DEX_ENTRY_PREFIX = "classes"

private const val DEX_ENTRY_SUFFIX = ".dex"

/** DEX 头部中 `string_ids_size` 与 `string_ids_off` 的偏移。 */
private const val STRING_IDS_SIZE_OFFSET = 56

private const val STRING_IDS_OFFSET = 60

/** 每个 `string_id_item` 是一个 4 字节的文件偏移。 */
private const val STRING_ID_ITEM_SIZE = 4

private const val INT_SIZE = 4

private const val BYTE_MASK = 0xFF

private const val BITS_PER_BYTE = 8

private const val ULEB128_CONTINUATION = 0x80

private const val ULEB128_MAX_BYTES = 5

private const val COPY_BUFFER_SIZE = 64 * 1024

private const val INITIAL_BUFFER_SIZE = 64 * 1024

private const val INITIAL_STRING_CAPACITY = 4096

private const val MAX_STRING_LENGTH = 256

private const val MAX_DEX_BYTES = 32 * 1024 * 1024

private const val MAX_STRINGS_PER_DEX = 400_000

private const val MAX_CANDIDATES = 400

/**
 * 域名形态的字符串。
 *
 * 不做嵌套量词，且输入长度已被限制在 [MAX_STRING_LENGTH] 以内，因此不存在灾难性回溯
 * （工程规则第 21 节要求防 ReDoS）。
 */
private val HOST_PATTERN = Regex("[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)+")

/**
 * DEX 字符串池扫描器。
 *
 * 不做「正则地毯式搜索整个文件」，而是按 DEX 格式读字符串池：从头部取 `string_ids` 表，
 * 逐条跳到 `string_data_item`，跳过 uleb128 长度前缀后读到 0 终止符。这样得到的字符串
 * 边界是确定的，不会把二进制指令流里碰巧出现的字节序列当成域名（工程规则第 13 节：
 * 生产实现应支持结构化解析，而不是只依赖字符串提取）。
 *
 * 所有读取都做边界检查：APK 是不可信输入，一个被篡改的偏移量就能让朴素实现越界崩溃（第 21 节）。
 *
 * 三个上限参数用于把最坏情况的工作量钉住：一个精心构造的 APK 不应该让扫描跑到内存耗尽。
 */
internal class DexDomainScanner(
    private val maxDexBytes: Int = MAX_DEX_BYTES,
    private val maxStringsPerDex: Int = MAX_STRINGS_PER_DEX,
    private val maxCandidates: Int = MAX_CANDIDATES,
) {
    fun scan(archive: ZipFile): DomainScanOutcome {
        val occurrences = HashMap<String, Int>()
        var dexFileCount = 0
        val entries = archive.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDexEntry()) {
                dexFileCount += 1
                collectFromEntry(archive = archive, entry = entry, occurrences = occurrences)
            }
        }
        return buildOutcome(occurrences = occurrences, dexFileCount = dexFileCount)
    }

    /** 收集单个 DEX 条目里的候选；条目不可读或不是 DEX 时什么都不做。 */
    private fun collectFromEntry(archive: ZipFile, entry: ZipEntry, occurrences: MutableMap<String, Int>) {
        val bytes = readEntry(archive, entry)
        if (bytes != null && bytes.hasDexMagic()) {
            collectStrings(bytes).forEach { text -> collectHosts(text, occurrences) }
        }
    }

    /**
     * 读出一个 DEX 条目的内容。
     *
     * 超过上限返回 null 而不是截断：截断后的字节数组里 `string_ids` 的偏移会指向不存在的位置，
     * 继续解析只会得到一堆误报。
     *
     * 这里不捕获 IOException：单条读失败说明 APK 已损坏，应由调用方统一按「无法读取」处理，
     * 扫描器自己吞掉一条本该上报的故障是更糟的选择。
     */
    private fun readEntry(archive: ZipFile, entry: ZipEntry): ByteArray? {
        if (entry.size > maxDexBytes) return null
        return archive.getInputStream(entry).use { stream -> stream.readAtMost(maxDexBytes) }
    }

    private fun collectStrings(dex: ByteArray): List<String> {
        val stringCount = dex.readIntOrNull(STRING_IDS_SIZE_OFFSET) ?: return emptyList()
        val tableOffset = dex.readIntOrNull(STRING_IDS_OFFSET) ?: return emptyList()
        if (stringCount <= 0 || tableOffset <= 0) return emptyList()
        val strings = ArrayList<String>(INITIAL_STRING_CAPACITY)
        for (index in 0 until minOf(stringCount, maxStringsPerDex)) {
            dex.readIntOrNull(tableOffset + index * STRING_ID_ITEM_SIZE)
                ?.let { dataOffset -> dex.readStringAt(dataOffset) }
                ?.let { text -> strings += text }
        }
        return strings
    }

    private fun collectHosts(text: String, occurrences: MutableMap<String, Int>) {
        HOST_PATTERN.findAll(text).forEach { match ->
            // 复用规则层的归一化：它已负责小写、去结尾点、拒绝单标签与非法字符，
            // 这里再写一套校验迟早会与规则侧不一致。
            val host = HostNormalizer.normalizeHost(match.value) ?: return@forEach
            occurrences[host] = (occurrences[host] ?: 0) + 1
        }
    }

    private fun buildOutcome(occurrences: Map<String, Int>, dexFileCount: Int): DomainScanOutcome {
        val sorted = occurrences.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { entry -> entry.value }
                    .thenBy { entry -> entry.key },
            )
        return DomainScanOutcome(
            candidates = sorted.take(maxCandidates).map { entry ->
                DomainCandidate(host = entry.key, occurrences = entry.value)
            },
            dexFileCount = dexFileCount,
            isTruncated = sorted.size > maxCandidates,
        )
    }
}

internal data class DomainScanOutcome(
    val candidates: List<DomainCandidate>,
    val dexFileCount: Int,
    val isTruncated: Boolean,
)

private fun ZipEntry.isDexEntry(): Boolean =
    !isDirectory && name.startsWith(DEX_ENTRY_PREFIX) && name.endsWith(DEX_ENTRY_SUFFIX)

private fun ByteArray.hasDexMagic(): Boolean =
    size >= DEX_MAGIC.length && String(this, 0, DEX_MAGIC.length, Charsets.US_ASCII) == DEX_MAGIC

private fun ByteArray.readIntOrNull(offset: Int): Int? {
    if (offset < 0 || offset + INT_SIZE > size) return null
    var value = 0
    for (index in 0 until INT_SIZE) {
        value = value or ((this[offset + index].toInt() and BYTE_MASK) shl (BITS_PER_BYTE * index))
    }
    return value
}

private fun ByteArray.readStringAt(dataOffset: Int): String? {
    val start = skipUleb128(dataOffset) ?: return null
    val end = indexOfTerminator(start) ?: return null
    val length = end - start
    if (length !in 1..MAX_STRING_LENGTH) return null
    // 用 UTF-8 而非严格 MUTF-8 解码：非 ASCII 字符可能被替换成 U+FFFD，
    // 但候选域名只会是 ASCII，替换既不会制造新候选，也不会抛异常。
    return String(this, start, length, Charsets.UTF_8)
}

private fun ByteArray.skipUleb128(offset: Int): Int? {
    var cursor = offset
    var readBytes = 0
    while (readBytes < ULEB128_MAX_BYTES && cursor < size) {
        val value = this[cursor].toInt() and BYTE_MASK
        cursor += 1
        readBytes += 1
        if (value and ULEB128_CONTINUATION == 0) return cursor
    }
    return null
}

private fun ByteArray.indexOfTerminator(from: Int): Int? {
    if (from >= size) return null
    var cursor = from
    while (cursor < size) {
        if (this[cursor] == 0.toByte()) return cursor
        cursor += 1
    }
    return null
}

private fun InputStream.readAtMost(limit: Int): ByteArray {
    val buffer = ByteArrayOutputStream(minOf(limit, INITIAL_BUFFER_SIZE))
    val chunk = ByteArray(COPY_BUFFER_SIZE)
    var total = 0
    while (total < limit) {
        val read = read(chunk, 0, minOf(chunk.size, limit - total))
        if (read < 0) break
        buffer.write(chunk, 0, read)
        total += read
    }
    return buffer.toByteArray()
}
