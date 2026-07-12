package ao.consuma.aqui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import ao.consuma.aqui.core.navigation.ConsumaAquiNavHost
import ao.consuma.aqui.ui.theme.ConsumaAquiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsumaAquiTheme {
                val navController = rememberNavController()
                ConsumaAquiNavHost(navController = navController)
            }
        }
    }
}