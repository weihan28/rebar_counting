package com.fyp.rebarcountingapp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    // Load the current profile from the ViewModel (if not loaded already)
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Track whether the user is in "edit" mode or not
    var isEditing by remember { mutableStateOf(false) }

    // Local states for editing the profile
    var firstName by remember { mutableStateOf(viewModel.userFirstName.value) }
    var lastName by remember { mutableStateOf(viewModel.userLastName.value) }
    var employeeId by remember { mutableStateOf(viewModel.userEmployeeId.value) }
    val email = viewModel.userEmail.value  // read-only field

    // Whenever the ViewModel's profile fields change, update local states if not editing
    LaunchedEffect(viewModel.userFirstName.value, viewModel.userLastName.value, viewModel.userEmployeeId.value) {
        if (!isEditing) {
            firstName = viewModel.userFirstName.value
            lastName = viewModel.userLastName.value
            employeeId = viewModel.userEmployeeId.value
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Callout/hint text about this page
            Text(
                text = "Here you can view or update your personal details. Tap 'Edit' to modify your info.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Profile fields
            OutlinedTextField(
                value = firstName,
                onValueChange = { if (isEditing) firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditing
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { if (isEditing) lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditing
            )
            OutlinedTextField(
                value = employeeId,
                onValueChange = { if (isEditing) employeeId = it },
                label = { Text("Employee ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isEditing
            )
            OutlinedTextField(
                value = email,
                onValueChange = { /* read-only */ },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // Always read-only
            )

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            if (!isEditing) {
                // If not editing, show "Edit" button
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }
            } else {
                // If editing, show "Save" and "Cancel"
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            // Save the updated info
                            viewModel.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                employeeId = employeeId,
                                onSuccess = {
                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                    isEditing = false
                                },
                                onError = { e ->
                                    Toast.makeText(context, "Update failed: ${e?.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = {
                            // Cancel editing; revert to ViewModel’s data
                            firstName = viewModel.userFirstName.value
                            lastName = viewModel.userLastName.value
                            employeeId = viewModel.userEmployeeId.value
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

