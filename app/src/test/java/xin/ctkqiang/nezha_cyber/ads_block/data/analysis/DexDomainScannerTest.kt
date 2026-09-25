package xin.ctkqiang.nezha_cyber.ads_block.data.analysis

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEX 字符串池扫描器的测试。
 *
 * 这一段是自写的二进制解析，出错方式很具体：偏移算错会越界、边界没查会崩、字符串边界读偏会
 * 把指令流里的字节当成域名。因此测试直接构造真实的 DEX 结构（头部 + string_ids + string_data），
 * 而不是拿一段假字节蒙混过去。APK 是不可信输入，畸形结构必须表现为「没有结果」而不是崩溃
 * （工程规则第 21、30 节）。
 */
class DexDomainScannerTest {
    private val scanner = DexDomainScanner()

    @Test
    fun `提取字符串池中的候选域名并按出现次数排序`() {
        val archive = archiveOf(
            DEX_ENTRY to buildDex(
                "https://ads.example.com/banner",
                "ads.example.com",
                "com.example.app",
                "这不是域名",
                "localhost",
            ),
        )

        val outcome = archive.use { scanner.scan(it) }

        assertEquals(1, outcome.dexFileCount)
        assertFalse(outcome.isTruncated)
        assertEquals(
            listOf("ads.example.com" to 2, "com.example.app" to 1),
            outcome.candidates.map { candidate -> candidate.host to candidate.occurrences },
        )
    }

    @Test
    fun `多个 DEX 合并统计`() {
        val archive = archiveOf(
            DEX_ENTRY to buildDex("first.example.com"),
            "classes2.dex" to buildDex("second.example.net"),
        )

        val outcome = archive.use { scanner.scan(it) }

        assertEquals(2, outcome.dexFileCount)
        assertEquals(
            listOf("first.example.com", "second.example.net"),
            outcome.candidates.map { candidate -> candidate.host }.sorted(),
        )
    }

    @Test
    fun `忽略非 DEX 条目与缺少魔数的文件`() {
        val archive = archiveOf(
            "resources.arsc" to buildDex("ignored.example.com"),
            DEX_ENTRY to ByteArray(256),
        )

        val outcome = archive.use { scanner.scan(it) }

        assertEquals(1, outcome.dexFileCount)
        assertTrue(outcome.candidates.isEmpty())
    }

    @Test
    fun `字符串偏移越界时不崩溃且返回空结果`() {
        val dex = buildDex("ads.example.com")
        writeLittleEndianInt(dex, STRING_IDS_OFFSET + STRING_IDS_SIZE_OFFSET, dex.size + 4096)

        val outcome = archiveOf(DEX_ENTRY to dex).use { scanner.scan(it) }

        assertTrue(outcome.candidates.isEmpty())
    }

    @Test
    fun `字符串条数超过实际内容时不崩溃`() {
        val dex = buildDex("ads.example.com")
        writeLittleEndianInt(dex, STRING_IDS_SIZE_OFFSET, 10_000)

        val outcome = archiveOf(DEX_ENTRY to dex).use { scanner.scan(it) }

        assertEquals(1, outcome.candidates.size)
    }

    @Test
    fun `候选数量超过上限时截断并标记`() {
        val archive = archiveOf(
            DEX_ENTRY to buildDex(
                "c.example.com",
                "a.example.com",
                "b.example.com",
            ),
        )

        val outcome = archive.use { DexDomainScanner(maxCandidates = 2).scan(it) }

        assertEquals(2, outcome.candidates.size)
        assertTrue(outcome.isTruncated)
        assertEquals("a.example.com", outcome.candidates.first().host)
    }

    @Test
    fun `单标签与非法字符不会被当成域名`() {
        val archive = archiveOf(
            DEX_ENTRY to buildDex("localhost", "12345", "not a domain", "*.ads", "上传.example"),
        )

        val outcome = archive.use { scanner.scan(it) }

        assertTrue(outcome.candidates.isEmpty())
    }

    private fun archiveOf(vararg entries: Pair<String, ByteArray>): ZipFile {
        val file = File.createTempFile("nezha-apk-test", ".apk")
        file.deleteOnExit()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return ZipFile(file)
    }

    /**
     * 构造一个结构真实的最小 DEX：112 字节头部 + string_ids 表 + string_data 区。
     *
     * 只填真正会用到的字段（魔数、string_ids_size、string_ids_off），其余保持 0——
     * 扫描器也只看这几项，测试因此能精确地锁住它读的位置。
     */
    private fun buildDex(vararg strings: String): ByteArray {
        val dataBlocks = strings.map { text -> uleb128(text.length) + text.toByteArray() + TERMINATOR }
        val stringIdsOffset = DEX_HEADER_SIZE
        val dataOffset = stringIdsOffset + strings.size * STRING_ID_ITEM_SIZE
        val totalSize = dataOffset + dataBlocks.sumOf { block -> block.size }
        val dex = ByteArray(totalSize)
        DEX_MAGIC_BYTES.copyInto(dex, 0)
        writeLittleEndianInt(dex, STRING_IDS_SIZE_OFFSET, strings.size)
        writeLittleEndianInt(dex, STRING_IDS_OFFSET, stringIdsOffset)
        var cursor = dataOffset
        dataBlocks.forEachIndexed { index, block ->
            writeLittleEndianInt(dex, stringIdsOffset + index * STRING_ID_ITEM_SIZE, cursor)
            block.copyInto(dex, cursor)
            cursor += block.size
        }
        return dex
    }

    private fun uleb128(value: Int): ByteArray {
        var remaining = value
        val bytes = ArrayList<Byte>()
        do {
            var byte = remaining and ULEB128_PAYLOAD_MASK
            remaining = remaining ushr ULEB128_BITS_PER_BYTE
            if (remaining != 0) byte = byte or ULEB128_CONTINUATION
            bytes += byte.toByte()
        } while (remaining != 0)
        return bytes.toByteArray()
    }

    private fun writeLittleEndianInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value shr 8).toByte()
        target[offset + 2] = (value shr 16).toByte()
        target[offset + 3] = (value shr 24).toByte()
    }

    private companion object {
        const val DEX_ENTRY = "classes.dex"

        const val DEX_HEADER_SIZE = 112

        const val STRING_IDS_SIZE_OFFSET = 56

        const val STRING_IDS_OFFSET = 60

        const val STRING_ID_ITEM_SIZE = 4

        const val ULEB128_CONTINUATION = 0x80

        const val ULEB128_BITS_PER_BYTE = 7

        const val ULEB128_PAYLOAD_MASK = 0x7F

        const val TERMINATOR: Byte = 0

        val DEX_MAGIC_BYTES = byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00)
    }
}
