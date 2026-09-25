package xin.ctkqiang.nezha_cyber.ads_block.feature.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.rememberApplicationIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaAppIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTextField
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/** 应用选择列表的最大高度。超过就滚动，而不是把对话框撑到满屏。 */
private val PICKER_MAX_HEIGHT = 320.dp

/**
 * 应用选择器。
 *
 * 单独成一个文件，而不是塞在规则页里：规则页要处理列表、编辑器、权限引导三件事，
 * 「从几百个应用里挑一个」是第四件、且与那三件没有共享状态的事。混在一起会让规则页那个文件
 * 同时容纳四种关注点，而其中三种的修改互不影响。
 *
 * 数据来自已安装应用清单（由 ViewModel 通过应用发现端口提供），这里**不**自己做应用扫描：
 * 另写一份扫描逻辑就会出现两处对「什么算一个应用」的判定，而它们迟早会不一致。
 *
 * 用对话框而不是页面内嵌列表：选应用是一个有明确边界的临时动作，选完即走，不该占据页面空间，
 * 也不该让用户为了一次选择而离开规则页再回来。
 */
@Composable
internal fun NotificationApplicationPicker(
    applications: List<InstalledApplication>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = NezhaTheme.palette
    var query by remember { mutableStateOf("") }
    val visible = remember(applications, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            applications
        } else {
            applications.filter { application -> application.label.contains(trimmed, ignoreCase = true) }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        NezhaSurfaceCard {
            BasicText(
                text = stringResource(R.string.notification_rule_picker_title),
                style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            NezhaTextField(
                value = query,
                onValueChange = { value -> query = value },
                placeholder = stringResource(R.string.notification_rule_picker_search),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            if (visible.isEmpty()) {
                BasicText(
                    text = stringResource(R.string.notification_rule_picker_empty),
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = PICKER_MAX_HEIGHT)) {
                    items(items = visible, key = { application -> application.packageName }) { application ->
                        PickerRow(application = application, onSelect = { onSelect(application.packageName) })
                    }
                }
            }
        }
    }
}

/**
 * 一行应用。
 *
 * 包名与显示名一起给出：同名应用在同一台设备上并不罕见（正式版与测试版），
 * 只显示名字时用户无法分辨自己要选哪一个。
 */
@Composable
private fun PickerRow(application: InstalledApplication, onSelect: () -> Unit) {
    val palette = NezhaTheme.palette
    val icon = rememberApplicationIcon(packageName = application.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(vertical = NezhaDimens.tightGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NezhaAppIcon(icon = icon, label = application.label, size = NezhaDimens.appIconCompactSize)
        Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = application.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
            )
            BasicText(
                text = application.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}
