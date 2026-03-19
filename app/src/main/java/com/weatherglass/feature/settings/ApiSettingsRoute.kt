package com.weatherglass.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weatherglass.ui.theme.SunnyEnd
import com.weatherglass.ui.theme.SunnyStart

@Composable
fun ApiSettingsRoute(
    onBack: () -> Unit,
    viewModel: ApiSettingsViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state.savedToast) {
        state.savedToast?.let {
            snack.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SunnyStart, SunnyEnd, Color(0xFFF2AA7D))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = Color.White)
                    }
                    Text("API 设置", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x3309111E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("和风天气 Key", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        KeyInput(value = state.qWeatherKey, onValueChange = viewModel::updateQWeather, hint = "输入 QWeather API Key")

                        Text("OpenWeather Key", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        KeyInput(value = state.openWeatherKey, onValueChange = viewModel::updateOpenWeather, hint = "输入 OpenWeather API Key")

                        Button(
                            onClick = viewModel::save,
                            shape = RoundedCornerShape(30.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C72FF), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("保存并应用")
                        }
                    }
                }
            }

            SnackbarHost(snack, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun KeyInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        color = Color(0x281E232D),
        shape = RoundedCornerShape(12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = Color.White)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(hint, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                }
                innerTextField()
            }
        )
    }
}
