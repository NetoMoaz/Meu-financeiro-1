package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.UserProfileAvatar
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GreenDarkPrimary
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.IncomeGreen
import com.example.viewmodel.FinanceViewModel
import java.io.File

@Composable
fun ProfileEditDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val currentName by viewModel.userName.collectAsStateWithLifecycle()
    val currentPhotoPath by viewModel.userPhotoPath.collectAsStateWithLifecycle()

    var tempName by remember { mutableStateOf(currentName) }
    var tempPhotoPath by remember { mutableStateOf(currentPhotoPath) }
    var newlyPickedPhotoPath by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = viewModel.savePhotoFromUri(uri)
            if (savedPath != null) {
                tempPhotoPath = savedPath
                newlyPickedPhotoPath = savedPath
            }
        }
    }

    val previewInitials = remember(tempName) {
        viewModel.getUserInitials(tempName)
    }

    Dialog(
        onDismissRequest = {
            if (newlyPickedPhotoPath != null && newlyPickedPhotoPath != currentPhotoPath) {
                try {
                    File(newlyPickedPhotoPath!!).delete()
                } catch (_: Exception) {}
            }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("profile_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header com Título e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Meu Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = {
                            if (newlyPickedPhotoPath != null && newlyPickedPhotoPath != currentPhotoPath) {
                                try {
                                    File(newlyPickedPhotoPath!!).delete()
                                } catch (_: Exception) {}
                            }
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Foto de Perfil Central com Badge de Câmera
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(104.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    UserProfileAvatar(
                        photoPath = tempPhotoPath,
                        initials = "MF",
                        size = 100.dp,
                        fontSize = 32.sp,
                        borderColor = GreenDarkPrimary.copy(alpha = 0.5f),
                        backgroundColor = GreenDarkPrimary.copy(alpha = 0.12f),
                        textColor = GreenDarkPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GreenDarkPrimary)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Alterar foto",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botões de Ação para Foto (Escolher / Remover)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("button_choose_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = GreenDarkPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tempPhotoPath != null) "Alterar foto" else "Escolher foto",
                            color = GreenDarkPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    if (tempPhotoPath != null) {
                        TextButton(
                            onClick = { tempPhotoPath = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("button_remove_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remover",
                                color = ExpenseRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Campo de Nome do Usuário
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Seu Nome") },
                    placeholder = { Text("Ex: Alexandre, Maria...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GreenDarkPrimary
                        )
                    },
                    supportingText = {
                        Text(
                            text = "Deixe em branco para usar \"Meu Financeiro\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenDarkPrimary,
                        focusedLabelColor = GreenDarkPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_name")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botões Salvar e Cancelar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (newlyPickedPhotoPath != null && newlyPickedPhotoPath != currentPhotoPath) {
                                try {
                                    File(newlyPickedPhotoPath!!).delete()
                                } catch (_: Exception) {}
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_cancel_profile")
                    ) {
                        Text(text = "Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            viewModel.saveProfile(tempName, tempPhotoPath)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDarkPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_save_profile")
                    ) {
                        Text(
                            text = "Salvar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
