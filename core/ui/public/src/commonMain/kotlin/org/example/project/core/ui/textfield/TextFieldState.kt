package org.example.project.core.ui.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange

@Composable
fun rememberKmpTextFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length),
): TextFieldState = rememberSaveable(saver = KmpTextFieldStateSaver) {
    TextFieldState(
        initialText,
        initialSelection
    )
}

// Default TextFieldSaver crashes when restoring TextUndoManager on iOS. Remove save/restore of TextUndoManager to avoid the issue
@Suppress("RedundantNullableReturnType")
private object KmpTextFieldStateSaver : Saver<TextFieldState, Any> {
    override fun SaverScope.save(value: TextFieldState): Any? {
        return listOf(
            value.text.toString(),
            value.selection.start,
            value.selection.end
        )
    }

    override fun restore(value: Any): TextFieldState? {
        val (text, selectionStart, selectionEnd) = value as List<*>
        return TextFieldState(
            initialText = text as String,
            initialSelection =
                TextRange(start = selectionStart as Int, end = selectionEnd as Int)
        )
    }
}