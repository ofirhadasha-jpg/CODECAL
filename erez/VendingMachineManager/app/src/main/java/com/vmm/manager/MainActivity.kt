package com.vmm.manager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vmm.manager.ui.AppNavigation
import com.vmm.manager.ui.products.ProductViewModel
import com.vmm.manager.ui.theme.VmmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val productViewModel: ProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        setContent {
            VmmTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Handles images shared from WhatsApp (or any other app).
     * User shares photo → select VMM as target → shows AddProduct dialog
     * pre-filled with the image.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            uri?.let {
                productViewModel.updateForm { copy(imageUri = it) }
                // Navigation to AddProduct handled in AppNavigation via state
            }
        }
    }
}
