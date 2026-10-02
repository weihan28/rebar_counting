package com.fyp.rebarcountingapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val description: String,
    val imageRes: Int
)

@Composable
fun OnboardingScreen1(
    onStartTour: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo or branding image
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(300.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartTour,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start the tour")
        }
    }
}

@Composable
fun OnboardingScreen2(
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pages = listOf(
        OnboardingPageData(
            title = "Capture the Rebar Image",
            description = "Snap a photo or upload an image from your gallery to count rebars.",
            imageRes = R.drawable.ic_rebar_capture
        ),
        OnboardingPageData(
            title = "Computes the Rebar Count",
            description = "Our AI model processes the image and computes the number of rebars instantly.",
            imageRes = R.drawable.ic_rebar_ai
        ),
        OnboardingPageData(
            title = "Statistical History of Rebar Count",
            description = "View past counts and track progress over time with easy-to-read charts.",
            imageRes = R.drawable.ic_rebar_stats
        )
    )

    val pageData = pages[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1) Top Content: image, title, description
        Column(
            modifier = Modifier
                .weight(1f)  // occupy remaining space
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(pageData.imageRes),
                contentDescription = pageData.title,
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = pageData.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = pageData.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        // 2) Bottom Content: dots indicator + next/finish button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.forEachIndexed { index, _ ->
                val color = if (index == currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(color = color, shape = CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next or Finish button
        if (currentPage < pages.size - 1) {
            Button(
                onClick = { currentPage++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        setOnboardingCompleted(context)
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Let's go!")
            }
        }
    }
}
