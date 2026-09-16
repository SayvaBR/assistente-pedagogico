package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** M0-only proof of boot: deliberately no fake navigation or placeholder features. */
@Composable
fun BootstrapScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(ApColors.Sky).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ApColors.Primary,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp)) {
                    Text("AP", fontSize = 36.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Assistente Pedagógico",
                fontSize = 29.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ApColors.Navy,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "A base de um jeito mais leve de ensinar.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = ApColors.Navy,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = ApColors.White) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                    Text("FUNDAÇÃO DO APP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ApColors.Navy)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Primeira tela técnica — sem dados fictícios ou botões sem função.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = ApColors.Navy,
                    )
                }
            }
        }
    }
}
