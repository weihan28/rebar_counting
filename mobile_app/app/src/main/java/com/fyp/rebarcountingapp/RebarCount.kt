package com.fyp.rebarcountingapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class RebarCount(
    val count: Int = 0,
    val imageUrl: String = "",
    val location: GeoPoint? = null,
    val timestamp: Timestamp? = null,
    val userId: String = ""
)
