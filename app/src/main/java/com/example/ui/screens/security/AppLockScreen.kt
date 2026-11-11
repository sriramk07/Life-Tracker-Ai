package com.example.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ElectricBlue

/**
 * ============================================================================
 * APP LOCK SCREEN — BIOMETRIC & PIN SECURITY VAULT
 * ============================================================================
 *
 * Gates entry to personal life metrics, financial ledgers, and journals when
 * security lock is enabled in Settings.
 */
@Composable
fun AppLockScreen(
    canUseBiometrics: Boolean,
    onVerifyPin: (String) -> Boolean,
    onTriggerBiometric: () -> Unit,
    onUnlocked: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050814))
            .padding(24.dp)
            .testTag("app_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphicCard(
            cornerRadius = 28.dp,
            borderColor = ElectricBlue.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Vault Locked",
                        tint = ElectricBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "LifeOS Security Vault",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White
                )

                Text(
                    text = "Enter your 4-digit PIN or verify biometrics to unlock your private metrics & journal.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { value ->
                        if (value.length <= 8) {
                            pinInput = value
                            errorMsg = null
                        }
                    },
                    label = { Text("Enter Vault PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_pin_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = Color(0xFFF43F5E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("vault_pin_error")
                    )
                }

                Button(
                    onClick = {
                        if (onVerifyPin(pinInput)) {
                            onUnlocked()
                        } else {
                            errorMsg = "Incorrect PIN. Please try again."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("vault_unlock_btn")
                ) {
                    Text("Unlock System", fontWeight = FontWeight.Bold)
                }

                if (canUseBiometrics) {
                    OutlinedButton(
                        onClick = onTriggerBiometric,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("vault_biometric_btn")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Fingerprint / Face", color = AccentCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
