package com.example.postureapp.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.postureapp.core.designsystem.DestructiveRed

@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = DestructiveRed,
        textAlign = textAlign
    )
}

