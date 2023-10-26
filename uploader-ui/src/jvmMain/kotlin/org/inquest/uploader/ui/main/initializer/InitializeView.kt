package org.inquest.uploader.ui.main.initializer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.inquest.uploader.ui.commons.view.InquestDIView
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import java.io.Serial

/**
 * Initializes the application and components.
 */
class InitializeView(private val onSuccess: () -> Unit): InquestDIView() {
    override fun DI.MainBuilder.initSubDI() {
        bindSingleton { StepsViewModel(instance(), instance()) }
    }

    @Composable
    override fun TheContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header()
            Spacer(Modifier.width(10.dp))
            Navigator(ArcFolderStep(this@InitializeView.onSuccess))
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 2379725234635671728L
    }
}

@Composable
private fun Header() {
    var dots: String by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch {
            while(true) {
                delay(400)
                dots = ".".repeat((dots.length + 1) % 5)
            }
        }
    }

    Text(
        "Totally not lowering your dps numbers$dots",
        fontSize = 35.sp,
        style = MaterialTheme.typography.h6
    )
}