package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.ui.theme.FrostedGlassBgDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBgLight
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderLight
import com.yourname.simplenotes.ui.theme.SakuraBorderSoft
import com.yourname.simplenotes.ui.theme.SakuraPink
import com.yourname.simplenotes.ui.theme.isAppInDarkTheme

/**
 * PIN entry dialog for unlocking or setting a lock on a note.
 * Styled with Frosted Glass ("Mờ sương") theme.
 */
@Composable
fun PinEntryDialog(
    title: String,
    onConfirm: (pin: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null
) {
    var pin by remember { mutableStateOf("") }
    val isDark = isAppInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(
            BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
            RoundedCornerShape(28.dp)
        ),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SakuraPink.copy(alpha = 0.12f))
                    .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SakuraPink,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = SakuraPink
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                    label = { Text("Mã PIN (chữ số)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
            ) {
                Text("Xác nhận", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else SakuraBorderSoft)
            ) {
                Text("Hủy")
            }
        }
    )
}
