package com.jar.app

import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.*
import android.widget.EditText

class RichTextEditor(private val editText: EditText) {

    init {
        // Ensure the EditText uses a typeface that supports bold
        editText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    var isBoldActive = false
        private set
    var isItalicActive = false
        private set
    var isUnderlineActive = false
        private set
    var isStrikethroughActive = false
        private set

    private var isApplyingFormat = false
    private var pendingFormatStart = -1
    private var pendingFormatEnd = -1

    init {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isApplyingFormat && after > 0) {
                    pendingFormatStart = start
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isApplyingFormat && count > 0) {
                    pendingFormatEnd = start + count
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isApplyingFormat) return
                if (pendingFormatStart < 0 || pendingFormatEnd <= pendingFormatStart) {
                    pendingFormatStart = -1
                    pendingFormatEnd = -1
                    return
                }

                val text = s ?: return
                val start = pendingFormatStart
                val end = pendingFormatEnd

                pendingFormatStart = -1
                pendingFormatEnd = -1

                isApplyingFormat = true

                if (isBoldActive && isItalicActive) {
                    text.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (isBoldActive) {
                    text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (isItalicActive) {
                    text.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (isUnderlineActive) {
                    text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (isStrikethroughActive) {
                    text.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                // Force EditText to redraw with the new spans
                if (isBoldActive || isItalicActive) {
                    editText.invalidate()
                }

                isApplyingFormat = false
            }
        })
    }

    fun toggleBold() {
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start < end) {
            applyStyleToSelection(Typeface.BOLD)
        } else {
            isBoldActive = !isBoldActive
        }
    }

    fun toggleItalic() {
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start < end) {
            applyStyleToSelection(Typeface.ITALIC)
        } else {
            isItalicActive = !isItalicActive
        }
    }

    fun toggleUnderline() {
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start < end) {
            applySpanToSelection<UnderlineSpan> { UnderlineSpan() }
        } else {
            isUnderlineActive = !isUnderlineActive
        }
    }

    fun toggleStrikethrough() {
        val start = editText.selectionStart
        val end = editText.selectionEnd

        if (start < end) {
            applySpanToSelection<StrikethroughSpan> { StrikethroughSpan() }
        } else {
            isStrikethroughActive = !isStrikethroughActive
        }
    }

    private fun applyStyleToSelection(style: Int) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text ?: return

        if (start >= end) return

        isApplyingFormat = true

        val existingSpans = text.getSpans(start, end, StyleSpan::class.java)
        val hasStyle = existingSpans.any {
            it.style == style && text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
        }

        if (hasStyle) {
            for (span in existingSpans) {
                if (span.style == style) {
                    val spanStart = text.getSpanStart(span)
                    val spanEnd = text.getSpanEnd(span)
                    text.removeSpan(span)

                    if (spanStart < start) {
                        text.setSpan(StyleSpan(style), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (spanEnd > end) {
                        text.setSpan(StyleSpan(style), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        } else {
            text.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        isApplyingFormat = false
    }

    private inline fun <reified T : CharacterStyle> applySpanToSelection(crossinline createSpan: () -> T) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text ?: return

        if (start >= end) return

        isApplyingFormat = true

        val existingSpans = text.getSpans(start, end, T::class.java)
        val hasSpan = existingSpans.any {
            text.getSpanStart(it) <= start && text.getSpanEnd(it) >= end
        }

        if (hasSpan) {
            for (span in existingSpans) {
                val spanStart = text.getSpanStart(span)
                val spanEnd = text.getSpanEnd(span)
                text.removeSpan(span)

                if (spanStart < start) {
                    text.setSpan(createSpan(), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (spanEnd > end) {
                    text.setSpan(createSpan(), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else {
            text.setSpan(createSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        isApplyingFormat = false
    }

    fun insertBulletPoint() {
        val start = editText.selectionStart
        val text = editText.text ?: return

        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val lineText = text.substring(lineStart, lineEnd)

        isApplyingFormat = true
        when {
            lineText.startsWith("• ") -> text.delete(lineStart, lineStart + 2)
            lineText.startsWith("- ") -> text.delete(lineStart, lineStart + 2)
            else -> text.insert(lineStart, "• ")
        }
        isApplyingFormat = false
    }

    fun insertNumberedItem() {
        val start = editText.selectionStart
        val text = editText.text ?: return

        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val lineText = text.substring(lineStart, lineEnd)

        val existingMatch = Regex("^(\\d+)\\.\\s").find(lineText)

        isApplyingFormat = true
        if (existingMatch != null) {
            text.delete(lineStart, lineStart + existingMatch.value.length)
        } else {
            var number = 1
            var checkPos = lineStart - 1
            while (checkPos > 0) {
                var prevLineStart = checkPos
                while (prevLineStart > 0 && text[prevLineStart - 1] != '\n') {
                    prevLineStart--
                }
                val prevLine = text.substring(prevLineStart, checkPos + 1)
                val match = Regex("^(\\d+)\\.\\s").find(prevLine)
                if (match != null) {
                    number = match.groupValues[1].toInt() + 1
                    break
                } else if (prevLine.isNotBlank()) {
                    break
                }
                checkPos = prevLineStart - 1
            }
            text.insert(lineStart, "$number. ")
        }
        isApplyingFormat = false
    }

    fun insertCheckbox() {
        val start = editText.selectionStart
        val text = editText.text ?: return

        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val lineText = text.substring(lineStart, lineEnd)

        isApplyingFormat = true
        when {
            lineText.startsWith("[ ] ") -> text.replace(lineStart, lineStart + 4, "[x] ")
            lineText.startsWith("[x] ") || lineText.startsWith("[X] ") -> text.delete(lineStart, lineStart + 4)
            else -> text.insert(lineStart, "[ ] ")
        }
        isApplyingFormat = false
    }

    fun increaseIndent() {
        val start = editText.selectionStart
        val text = editText.text ?: return

        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        isApplyingFormat = true
        text.insert(lineStart, "    ")
        isApplyingFormat = false
    }

    fun decreaseIndent() {
        val start = editText.selectionStart
        val text = editText.text ?: return

        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        var removeCount = 0
        if (lineStart < text.length) {
            if (text[lineStart] == '\t') {
                removeCount = 1
            } else {
                while (removeCount < 4 && lineStart + removeCount < text.length && text[lineStart + removeCount] == ' ') {
                    removeCount++
                }
            }
        }

        if (removeCount > 0) {
            isApplyingFormat = true
            text.delete(lineStart, lineStart + removeCount)
            isApplyingFormat = false
        }
    }

    fun clearFormattingState() {
        isBoldActive = false
        isItalicActive = false
        isUnderlineActive = false
        isStrikethroughActive = false
    }

    fun toHtml(): String {
        val text = editText.text ?: return ""
        if (text.isEmpty()) return ""
        return Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    }

    fun fromHtml(html: String) {
        if (html.isBlank()) {
            editText.setText("")
            return
        }
        isApplyingFormat = true
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        editText.setText(spanned)
        isApplyingFormat = false
    }

    fun getPlainText(): String {
        return editText.text?.toString() ?: ""
    }
}
