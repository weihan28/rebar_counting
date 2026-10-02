package com.fyp.rebarcountingapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/* ────────────────────────────────────────────────────────── */
/*  View History with chart + filters + compact cards        */
/* ────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewHistoryScreen(
    onBack: () -> Unit,
    onRecordClick: (RebarCount) -> Unit
) {
    /* ─── Firestore load ─── */
    val firestore  = remember { FirebaseFirestore.getInstance() }
    var history    by remember { mutableStateOf<List<RebarCount>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val snap = firestore.collection("rebarCounts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            history = snap.documents.map { d ->
                RebarCount(
                    count            = d.getLong("count")?.toInt() ?: 0,
                    imageUrl         = d.getString("imageUrl") ?: "",
                    location         = d.getGeoPoint("location"),
                    timestamp        = d.getTimestamp("timestamp"),
                    userId           = d.getString("userId") ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoading = false
    }

    /* ─── UI state ─── */
    var chartGroupBy by remember { mutableStateOf("Month") }
    var cardGroupBy  by remember { mutableStateOf("Month") }
    var filterDate   by remember { mutableStateOf<String?>(null) }
    var ddExpanded   by remember { mutableStateOf(false) }

    /* ─── Scaffold ─── */
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rebar Count History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            /* loading */
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            /* ─── Quick stats ─── */
            Text("Quick Stats", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Group by", fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                SegmentedControl(
                    options = listOf("Month", "Day"),
                    selectedOption = chartGroupBy,
                    onOptionSelected = { chartGroupBy = it }
                )
            }

            Spacer(Modifier.height(8.dp))
            LineGraph(records = history, groupBy = chartGroupBy)
            Spacer(Modifier.height(16.dp))

            /* ─── Filters ─── */
            Text("Filter by", fontWeight = FontWeight.Medium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                ->
                Spacer(Modifier.width(8.dp))
                SegmentedControl(
                    options = listOf("Month", "Day"),
                    selectedOption = cardGroupBy
                ) {
                    cardGroupBy = it
                    filterDate  = null
                }

                Spacer(Modifier.width(8.dp))

                /* dropdown values */
                val fmt         = if (cardGroupBy == "Month") "MMM yyyy" else "dd MMM yyyy"
                val dateOptions = history
                    .map { SimpleDateFormat(fmt, Locale.getDefault()).format(it.timestamp?.toDate() ?: Date()) }
                    .distinct()
                    .sorted()

                Box( modifier = Modifier.weight(1f) ) {
                    ExposedDropdownMenuBox(
                        expanded = ddExpanded,
                        onExpandedChange = { ddExpanded = !ddExpanded }
                    ) {
                        TextField(
                            value = filterDate ?: "All ${cardGroupBy}s",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(ddExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )

                        ExposedDropdownMenu(
                            expanded = ddExpanded,
                            onDismissRequest = { ddExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All ${cardGroupBy}s") },
                                onClick = {
                                    filterDate = null
                                    ddExpanded = false
                                }
                            )
                            dateOptions.forEach { dt ->
                                DropdownMenuItem(
                                    text = { Text(dt) },
                                    onClick = {
                                        filterDate = dt
                                        ddExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            /* ─── Compact list ─── */
            val fmt = if (cardGroupBy == "Month") "MMM yyyy" else "dd MMM yyyy"
            val sdfList = SimpleDateFormat(fmt, Locale.getDefault())
            val visible = history.filter { rec ->
                filterDate == null || sdfList.format(rec.timestamp?.toDate() ?: Date()) == filterDate
            }.sortedByDescending { it.timestamp?.toDate() }

            LazyColumn {
                items(visible) { rec ->
                    HistoryCompactCard(rec, onRecordClick)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────── */
/*  Compact card                                              */
/* ────────────────────────────────────────────────────────── */
@Composable
private fun HistoryCompactCard(
    rec: RebarCount,
    onClick: (RebarCount) -> Unit
) {
    val dateStr = rec.timestamp?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
    } ?: "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(rec) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(dateStr)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Count", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(rec.count.toString())
            }
        }
    }
}

/* ────────────────────────────────────────────────────────── */
/*  Simple segmented control                                  */
/* ────────────────────────────────────────────────────────── */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selectedOption,
                onClick = { onOptionSelected(opt) },
                label = { Text(opt) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}