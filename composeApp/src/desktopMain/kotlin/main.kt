import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.schultetable.app.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "舒尔特表 - Schulte Table",
        state = rememberWindowState(width = 600.dp, height = 800.dp)
    ) {
        App()
    }
}
