package org.dpdns.bnai.anime.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import org.dpdns.bnai.anime.ui.testing.TestTags

object StudioTokens {
    val Primary = Color(0xFF10A37F)
    val Secondary = Color(0xFF3B82F6)
    val DarkBackground = Color(0xFF0F1115)
    val LightBackground = Color(0xFFF7F7F8)
    val DarkSurface = Color(0xFF181B22)
    val LightSurface = Color(0xFFFFFFFF)
    val Error = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val Success = Color(0xFF10B981)
    val Neutral = Color(0xFF6B7280)

    val TouchTargetMin: Dp = 48.dp
    val CardRadius: Dp = 12.dp
    val DialogRadius: Dp = 16.dp
    val SheetRadius: Dp = 20.dp
    val ControlRadius: Dp = 8.dp
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        if (trailingContent != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailingContent
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
        if (!badgeText.isNullOrBlank()) {
            StatusChip(label = badgeText)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .heightIn(min = StudioTokens.TouchTargetMin)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(StudioTokens.ControlRadius),
        colors = ButtonDefaults.buttonColors(containerColor = StudioTokens.Primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = StudioTokens.TouchTargetMin)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(StudioTokens.ControlRadius)
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = StudioTokens.TouchTargetMin)
            .semantics { contentDescription = text },
        shape = RoundedCornerShape(StudioTokens.ControlRadius),
        colors = ButtonDefaults.buttonColors(containerColor = StudioTokens.Error)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(StudioTokens.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color = StudioTokens.Primary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(StudioTokens.CardRadius)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusChip(label: String, modifier: Modifier = Modifier) {
    val upper = label.uppercase().trim()
    val chipColor = when {
        upper in listOf("SUCCEEDED", "ACTIVE", "COMMITTED", "PASSED", "APPROVED", "ONLINE", "COMPLETED", "HEALTHY", "UP", "ALIVE", "LOCKED", "RESOLVED", "VERIFIED_N8N_API") -> StudioTokens.Success
        upper in listOf("QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RECONCILING", "RUNNING", "DRAFT", "WARNING", "PENDING", "BUSY", "RENDERING...", "WAITING IN QUEUE...", "GENERATING...", "MANUAL_REVIEW") -> StudioTokens.Warning
        upper in listOf("FAILED", "ERROR", "BLOCKED", "REJECTED", "OFFLINE", "DOWN", "DEAD", "CRITICAL", "ISSUES_FOUND", "CANCELLED", "EXPIRED") -> StudioTokens.Error
        upper in listOf("UNSET", "UNCHECKED", "UNKNOWN", "UNAVAILABLE", "IDLE", "SKIPPED", "COMMAND_ACTIVITY_ONLY") -> StudioTokens.Neutral
        else -> StudioTokens.Secondary
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(chipColor.copy(alpha = 0.15f))
            .border(1.dp, chipColor.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(chipColor)
        )
        Text(
            text = label,
            color = chipColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(StudioTokens.ControlRadius),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    confirmTestTag: String = TestTags.Dialog.CONFIRM,
    dismissTestTag: String = TestTags.Dialog.CANCEL,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(StudioTokens.DialogRadius),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onClick@{
                    onConfirm()
                },
                modifier = Modifier.testTag(confirmTestTag),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) StudioTokens.Error else StudioTokens.Primary
                )
            ) {
                Text(confirmText, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(dismissTestTag)
            ) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Displays a dialog when unsaved changes exist on the current screen.
 * Provides options to discard changes, keep editing, or save before leaving.
 *
 * @param onDiscard Called when user chooses to discard changes and leave.
 * @param onKeepEditing Called when user chooses to keep editing and stay on screen.
 * @param onSave Called when user chooses to save changes before leaving. Optional;
 *               if null, the "Save" button is not shown and the dialog has two options.
 */
@Composable
fun UnsavedChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    onSave: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        shape = RoundedCornerShape(StudioTokens.DialogRadius),
        title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (onSave != null) {
                    "You have unsaved edits. Save before leaving?"
                } else {
                    "You have unsaved edits on this screen. Leaving now will discard your changes."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onDiscard,
                modifier = Modifier.testTag(TestTags.Dialog.UNSAVED_DISCARD),
                colors = ButtonDefaults.buttonColors(containerColor = StudioTokens.Error)
            ) {
                Text("Discard", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onKeepEditing,
                    modifier = Modifier.testTag(TestTags.Dialog.UNSAVED_KEEP_EDITING)
                ) {
                    Text("Keep Editing")
                }
                if (onSave != null) {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioTokens.Warning)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    )
}

/**
 * BUG-015 & BUG-030: Standardized AsyncMediaPreview with Loading, Success, Broken & Empty states.
 * Never renders a raw URL string as a fake preview.
 */
@Composable
fun AsyncMediaPreview(
    mediaUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    emptyLabel: String = "No cover or media asset configured"
) {
    val cleanUrl = mediaUrl?.trim().orEmpty()
    val isValidHttp = cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(StudioTokens.CardRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!isValidHttp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = emptyLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SubcomposeAsyncImage(
                model = cleanUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Broken media preview",
                            tint = StudioTokens.Error
                        )
                        Text(
                            text = "Media asset unavailable or broken URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTokens.Error
                        )
                    }
                }
            )
        }
    }
}

/**
 * BUG-006 & BUG-028: Complete Compose Markdown Renderer.
 * Supports Headings (#..###), Bold (**), Italic (*), Inline Code (`),
 * Fenced Code Blocks (```), Ordered/Unordered Lists, Blockquotes (>), and Tables (|).
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val lines = markdown.replace("\r\n", "\n").split("\n")
            var i = 0
            while (i < lines.size) {
                val rawLine = lines[i]
                val trimmed = rawLine.trim()

                // 1. Fenced Code Block (```)
                if (trimmed.startsWith("```")) {
                    val lang = trimmed.removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    Surface(
                        color = Color(0xFF111318),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D3340)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (lang.isNotEmpty()) {
                                Text(
                                    text = lang.uppercase(),
                                    fontSize = 10.sp,
                                    color = StudioTokens.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                Text(
                                    text = codeLines.joinToString("\n"),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE5E7EB)
                                )
                            }
                        }
                    }
                    i++
                    continue
                }

                // 2. Markdown Table (| col1 | col2 |)
                if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
                    val tableRows = mutableListOf<List<String>>()
                    while (i < lines.size) {
                        val rowStr = lines[i].trim()
                        if (!rowStr.startsWith("|") || !rowStr.endsWith("|")) break
                        val cells = rowStr.trim('|').split("|").map { it.trim() }
                        // Skip separator row like |---|---|
                        val isSeparator = cells.all { c -> c.all { ch -> ch == '-' || ch == ':' || ch == ' ' } }
                        if (!isSeparator) {
                            tableRows.add(cells)
                        }
                        i++
                    }
                    if (tableRows.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                tableRows.forEachIndexed { rowIndex, rowCells ->
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                if (rowIndex == 0) MaterialTheme.colorScheme.surfaceVariant
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowCells.forEach { cellText ->
                                            Text(
                                                text = parseInlineMarkdown(cellText),
                                                fontSize = 12.sp,
                                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.widthIn(min = 80.dp, max = 220.dp)
                                            )
                                        }
                                    }
                                    if (rowIndex < tableRows.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                    continue
                }

                // 3. Blank line
                if (trimmed.isEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    i++
                    continue
                }

                // 4. Headings (#, ##, ###)
                if (trimmed.startsWith("### ")) {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    i++
                    continue
                }
                if (trimmed.startsWith("## ")) {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    i++
                    continue
                }
                if (trimmed.startsWith("# ")) {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    i++
                    continue
                }

                // 5. Blockquotes (> )
                if (trimmed.startsWith(">")) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(StudioTokens.Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(trimmed.removePrefix(">").trim()),
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp
                        )
                    }
                    i++
                    continue
                }

                // 6. Unordered or Ordered List Items
                val isBullet = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")
                val numberedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
                if (isBullet) {
                    val itemContent = trimmed.drop(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = parseInlineMarkdown(itemContent),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    i++
                    continue
                } else if (numberedMatch != null) {
                    val numStr = numberedMatch.groupValues[1]
                    val itemContent = numberedMatch.groupValues[2]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("$numStr. ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = parseInlineMarkdown(itemContent),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    i++
                    continue
                }

                // 7. Standard Paragraph
                Text(
                    text = parseInlineMarkdown(rawLine),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
                i++
            }
        }
    }
}

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < text.length) {
            // Inline code `...`
            if (text[idx] == '`') {
                val endIdx = text.indexOf('`', idx + 1)
                if (endIdx != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x336B7280),
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(text.substring(idx + 1, endIdx))
                    }
                    idx = endIdx + 1
                    continue
                }
            }
            // Bold **...**
            if (idx + 1 < text.length && text[idx] == '*' && text[idx + 1] == '*') {
                val endIdx = text.indexOf("**", idx + 2)
                if (endIdx != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(idx + 2, endIdx))
                    }
                    idx = endIdx + 2
                    continue
                }
            }
            // Italic *...*
            if (text[idx] == '*') {
                val endIdx = text.indexOf('*', idx + 1)
                if (endIdx != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(idx + 1, endIdx))
                    }
                    idx = endIdx + 1
                    continue
                }
            }
            // Markdown Link [label](url)
            if (text[idx] == '[') {
                val closeBracket = text.indexOf(']', idx + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = text.substring(idx + 1, closeBracket)
                        withStyle(
                            SpanStyle(
                                color = StudioTokens.Secondary,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(label)
                        }
                        idx = closeParen + 1
                        continue
                    }
                }
            }
            append(text[idx])
            idx++
        }
    }
}
