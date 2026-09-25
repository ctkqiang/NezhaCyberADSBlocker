package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 自绘单行输入框。
 *
 * 不使用 Material 的组件：本项目没有引入 Material，输入框的底色、描边、光标色都属于视觉令牌，
 * 交给外部实现会带进一套独立配色。
 *
 * 键盘动作固定为「完成」并支持提交：输入域名后不必收起键盘去点按钮，这是这一页的主要交互路径。
 */
@Composable
fun NezhaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
) {
    val palette = NezhaTheme.palette
    val shape = RoundedCornerShape(NezhaDimens.textFieldCornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NezhaDimens.textFieldMinHeight)
            .clip(shape)
            .background(palette.surface)
            .border(NezhaDimens.hairline, palette.outline, shape)
            .padding(horizontal = NezhaDimens.textFieldHorizontalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            BasicText(
                text = placeholder,
                style = NezhaTheme.typography.body.copy(color = palette.textSecondary),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = NezhaTheme.typography.body.copy(color = palette.textPrimary),
            cursorBrush = SolidColor(palette.brand),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = if (onSubmit == null) ImeAction.Default else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        )
    }
}
