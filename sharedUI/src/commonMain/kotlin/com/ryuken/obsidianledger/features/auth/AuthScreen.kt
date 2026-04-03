package com.ryuken.obsidianledger.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryuken.obsidianledger.core.ui.theme.LedgerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = LedgerTheme.colors

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.AuthSuccess -> onSuccess()
                is AuthEffect.Error    -> { /* error shown in state */ }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .drawBehind {
                // Grid texture background — subtle dots
                val spacing = 24.dp.toPx()
                val dotRadius = 1.dp.toPx()
                val dotColor = colors.ghostBorder
                for (x in 0..(size.width / spacing).toInt()) {
                    for (y in 0..(size.height / spacing).toInt()) {
                        drawCircle(
                            color  = dotColor,
                            radius = dotRadius,
                            center = Offset(x * spacing, y * spacing)
                        )
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // ── App branding ────────────────────────────────────
            Text(
                text = "OBSIDIAN",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    letterSpacing = 4.sp,
                    color = colors.accentStart
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "LEDGER",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = colors.onSurfacePrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(48.dp))

            // ── Tab Switcher ────────────────────────────────────
            AuthTabSwitcher(
                activeTab = state.activeTab,
                onTabChanged = { viewModel.onIntent(AuthIntent.TabChanged(it)) }
            )

            Spacer(Modifier.height(32.dp))

            // ── Display Name (Create Account only) ──────────────
            AnimatedVisibility(
                visible = state.activeTab == AuthTab.CREATE_ACCOUNT,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut()
            ) {
                Column {
                    UnderlineTextField(
                        value = state.displayName,
                        onValueChange = { viewModel.onIntent(AuthIntent.DisplayNameChanged(it)) },
                        label = "Display Name",
                        accentColor = colors.accentStart,
                        textColor = colors.onSurfacePrimary,
                        hintColor = colors.onSurfaceSecondary
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── Email ───────────────────────────────────────────
            UnderlineTextField(
                value = state.email,
                onValueChange = { viewModel.onIntent(AuthIntent.EmailChanged(it)) },
                label = "Email",
                keyboardType = KeyboardType.Email,
                accentColor = colors.accentStart,
                textColor = colors.onSurfacePrimary,
                hintColor = colors.onSurfaceSecondary
            )

            Spacer(Modifier.height(20.dp))

            // ── Password ────────────────────────────────────────
            UnderlineTextField(
                value = state.password,
                onValueChange = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
                label = "Password",
                isPassword = true,
                imeAction = ImeAction.Done,
                onDone = { viewModel.onIntent(AuthIntent.SubmitClick) },
                accentColor = colors.accentStart,
                textColor = colors.onSurfacePrimary,
                hintColor = colors.onSurfaceSecondary
            )

            Spacer(Modifier.height(12.dp))

            // ── Error ───────────────────────────────────────────
            AnimatedVisibility(visible = state.error != null) {
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Submit Button ───────────────────────────────────
            Button(
                onClick  = { viewModel.onIntent(AuthIntent.SubmitClick) },
                enabled  = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = colors.accentStart,
                    contentColor   = Color.White
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text  = if (state.activeTab == AuthTab.SIGN_IN) "SIGN IN" else "CREATE ACCOUNT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Auth Tab Switcher
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AuthTabSwitcher(
    activeTab: AuthTab,
    onTabChanged: (AuthTab) -> Unit
) {
    val colors = LedgerTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceLow),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AuthTab.entries.forEach { tab ->
            val isActive = tab == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabChanged(tab) }
                    .background(
                        if (isActive) colors.surfaceContainer
                        else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = if (tab == AuthTab.SIGN_IN) "Sign In" else "Create Account",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isActive) colors.accentStart else colors.onSurfaceSecondary,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Underline-only text field
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    accentColor: Color,
    textColor: Color,
    hintColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = hintColor,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        TextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            textStyle     = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            singleLine    = true,
            visualTransformation = if (isPassword)
                PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction    = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor    = Color.Transparent,
                unfocusedContainerColor  = Color.Transparent,
                disabledContainerColor   = Color.Transparent,
                focusedIndicatorColor    = accentColor,
                unfocusedIndicatorColor  = hintColor.copy(alpha = 0.3f),
                cursorColor              = accentColor
            )
        )
    }
}
