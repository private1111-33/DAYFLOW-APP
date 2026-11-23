package com.dayflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.dayflow.app.ui.theme.*

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08080A), Color(0xFF160B12), Color(0xFF09090B))))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
fun DayflowCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val colors = CardDefaults.cardColors(containerColor = Color(0xFF171317))
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
    }
    val modifier = Modifier.fillMaxWidth().border(
        width = 1.dp,
        brush = Brush.linearGradient(listOf(Color(0xFF653047), Color(0xFF30232B), Color(0xFF241B21))),
        shape = shape
    )
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, content = inner)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, content = inner)
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun PlaceholderNote(text: String) {
    Text(text, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = TextSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = BurgundyLight) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
