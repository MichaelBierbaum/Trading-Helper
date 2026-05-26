package de.bierbaum.tradinghelper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    // Da das Logo einen festen weißen Hintergrund in der Bilddatei hat,
    // ist die sauberste Lösung für einen nahtlosen Look, den Hintergrund des
    // Splash-Screens ebenfalls auf Weiß zu setzen. 
    // So verschmilzt das Logo perfekt mit dem Bildschirm.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bierbaum),
            contentDescription = "Logo",
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentScale = ContentScale.Fit
        )
    }
}
