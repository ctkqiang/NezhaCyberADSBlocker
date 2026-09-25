package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 「名称 —— 值」的堆叠信息行。
 *
 * 与 [NezhaMetricRow] 的横向排布分工不同：那一行服务的是短读数，标签与数值分居两端，追求一眼扫过；
 * 这一行要容纳仓库地址、整句说明这类长值，横排会把标签挤成竖排、或让数值折行后与标签错位，
 * 因此标签在上、值在下，长值可以自由换行。标签用次要面色、值用正文色，长段落读起来才不会挤成一团。
 */
@Composable
fun NezhaInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = label,
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        BasicText(
            text = value,
            style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
        )
    }
}
