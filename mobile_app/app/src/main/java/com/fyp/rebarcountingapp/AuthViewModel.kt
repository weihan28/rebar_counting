package com.fyp.rebarcountingapp

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Profile data (assume these are loaded at sign-in)
    var userFirstName = mutableStateOf("")
    var userLastName = mutableStateOf("")
    var userEmployeeId = mutableStateOf("")
    var userEmail = mutableStateOf("")

    // Called when user taps the "Sign In" button
    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "signInWithEmail: success")
                    loadUserProfile { onSuccess() }
                } else {
                    Log.w("AuthViewModel", "signInWithEmail: failure", task.exception)
                    onError(task.exception)
                }
            }
    }

    // Called when user taps the "Sign Up" button
    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        employeeId: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "createUserWithEmail: success")
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(
                            userId = it.uid,
                            email = email,
                            firstName = firstName,
                            lastName = lastName,
                            employeeId = employeeId,
                            onSuccess = { loadUserProfile { onSuccess() } },
                            onError = onError
                        )
                    }
                } else {
                    Log.w("AuthViewModel", "createUserWithEmail: failure", task.exception)
                    onError(task.exception)
                }
            }
    }

    fun loadUserProfile(onComplete: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userFirstName.value = doc.getString("firstname") ?: ""
                    userLastName.value = doc.getString("lastname") ?: ""
                    userEmployeeId.value = doc.getString("employeeId") ?: ""
                    userEmail.value = doc.getString("email") ?: ""
                }
                onComplete?.invoke()
            }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        employeeId: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "firstname" to firstName,
            "lastname" to lastName,
            "employeeId" to employeeId
        )
        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                // Update local state
                userFirstName.value = firstName
                userLastName.value = lastName
                userEmployeeId.value = employeeId
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    private fun saveUserToFirestore(
        userId: String,
        email: String,
        firstName: String,
        lastName: String,
        employeeId: String,
        onSuccess: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        val userEntry = hashMapOf(
            "email" to email,
            "firstname" to firstName,
            "lastname" to lastName,
            "employeeId" to employeeId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(userId)
            .set(userEntry)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    // Check if a user is already logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Sign out if needed
    fun signOut() {
        auth.signOut()
    }
}