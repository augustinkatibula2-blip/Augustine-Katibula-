package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.LinkUpLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BrandedLaunchSplash(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyPrimary)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinkUpLogo(darkBackground = true, showTagline = true, iconSize = 48.dp)
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = ElectricBlueBright,
                trackColor = Color.White.copy(alpha = 0.16f)
            )
        }
    }
}

private data class WelcomeSlide(
    val icon: ImageVector,
    val badge: String,
    val title: String,
    val description: String
)

@Composable
fun AppWelcomeScreen(
    onExploreApp: () -> Unit,
    onSignIn: () -> Unit
) {
    val slides = listOf(
        WelcomeSlide(
            icon = Icons.Rounded.Handshake,
            badge = "Co-Founder Matching",
            title = "Find the right partner to build with",
            description = "Swipe or browse builders scored across complementary skills, vision, commitment, equity expectations, and location across Malawi and Africa."
        ),
        WelcomeSlide(
            icon = Icons.Rounded.WorkOutline,
            badge = "Skills-First Careers",
            title = "Get matched on ability, not just titles",
            description = "Discover full-time, hybrid, and remote roles where practical experience and shipped portfolios count alongside formal education."
        ),
        WelcomeSlide(
            icon = Icons.Rounded.RocketLaunch,
            badge = "14-Day Build Together",
            title = "Collaborate before committing long-term",
            description = "Message verified connections privately and run a structured 14-day sprint with shared tasks and reviews before launching a venture."
        )
    )
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinkUpLogo(iconSize = 26.dp)
                TextButton(
                    onClick = onExploreApp,
                    modifier = Modifier.testTag("welcome_skip_button")
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = MaterialTheme.linkUpColors.accentContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(84.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = MaterialTheme.linkUpColors.onAccentContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = slide.badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.linkUpColors.accentText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pager Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(slides.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) ElectricBlue else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pagerState.currentPage < slides.lastIndex) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Continue")
                    }
                } else {
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("welcome_signin_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Sign In or Create Account")
                    }
                }
                OutlinedButton(
                    onClick = onExploreApp,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("welcome_explore_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("Explore Opportunities First")
                }
            }
        }
    }
}
