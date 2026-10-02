package com.fyp.rebarcountingapp

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // 1) Holds the list of up to 5 most recent rebar counts
    var recentRebarCounts by mutableStateOf<List<RebarCount>>(emptyList())
        private set

    // 2) Total number of rebar count entries
    var totalCounts by mutableIntStateOf(0)
        private set

    // 3) User's display name
    var userDisplayName by mutableStateOf("")
        private set

    init {
        loadDashboardData()
    }

    /**
     * Loads all necessary data for the Dashboard:
     * - Recent rebar counts
     * - Total rebar count
     * - Weekly distribution of counts (for a bar chart)
     */
    fun loadDashboardData() {
        loadRecentRebarCounts()
        loadTotalCounts()
        loadUserName()
    }

    fun clearDashboardData() {
        recentRebarCounts = emptyList()
        totalCounts = 0
        userDisplayName = ""
    }

    /**
     *  Fetch the most recent rebar counts from Firestore, ordered by timestamp descending.
     */
    private fun loadRecentRebarCounts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        firestore.collection("rebarCounts")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val items = querySnapshot.documents.map { doc ->
                    val rebarCount = doc.toObject(RebarCount::class.java) ?: RebarCount()
                    rebarCount.copy()
                }
                recentRebarCounts = items
            }
            .addOnFailureListener { e ->
                Log.e("DashboardViewModel", "Error fetching rebar counts", e)
            }
    }

    /**
     *  Load the total number of rebar counts by simply counting all documents in "rebarCounts".
     */
    private fun loadTotalCounts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        firestore.collection("rebarCounts")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                totalCounts = querySnapshot.size()
            }
            .addOnFailureListener { e ->
                Log.e("DashboardViewModel", "Error fetching total counts", e)
            }
    }

    /**
     *  Load the user's display name from Firestore.
     */
    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val firstName = doc.getString("firstname") ?: ""
                    val lastName = doc.getString("lastname") ?: ""
                    userDisplayName = "$firstName $lastName"
                }
            }
            .addOnFailureListener {
                // handle error or set a default
                userDisplayName = ""
            }
    }
}