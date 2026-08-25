package com.expensemanager.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.expensemanager.app.ui.navigation.AppNavigation
import com.expensemanager.app.ui.screens.accounts.AccountsViewModel
import com.expensemanager.app.ui.screens.analytics.AnalyticsViewModel
import com.expensemanager.app.ui.screens.dashboard.DashboardViewModel
import com.expensemanager.app.ui.screens.settings.SettingsViewModel
import com.expensemanager.app.ui.screens.transactions.TransactionsViewModel
import com.expensemanager.app.ui.theme.ExpenseManagerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsReadGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val smsReceiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false

        if (smsReadGranted || smsReceiveGranted) {
            dashboardViewModel.syncSms()
        }
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        viewModelFactory {
            val app = application as ExpenseApplication
            DashboardViewModel(app.transactionRepository, app.accountRepository, app.smsRepository)
        }
    }

    private val transactionsViewModel: TransactionsViewModel by viewModels {
        viewModelFactory {
            val app = application as ExpenseApplication
            TransactionsViewModel(app.transactionRepository)
        }
    }

    private val analyticsViewModel: AnalyticsViewModel by viewModels {
        viewModelFactory {
            val app = application as ExpenseApplication
            AnalyticsViewModel(app.transactionRepository)
        }
    }

    private val accountsViewModel: AccountsViewModel by viewModels {
        viewModelFactory {
            val app = application as ExpenseApplication
            AccountsViewModel(app.accountRepository, app.transactionRepository)
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            val app = application as ExpenseApplication
            SettingsViewModel(app.transactionRepository, app.smsRepository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        enableHighRefreshRate()
        checkAndRequestPermissions()

        setContent {
            ExpenseManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        dashboardViewModel = dashboardViewModel,
                        transactionsViewModel = transactionsViewModel,
                        analyticsViewModel = analyticsViewModel,
                        accountsViewModel = accountsViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
    }

    private fun enableHighRefreshRate() {
        try {
            // Android 11+ / 12+ (API 30+/31+) Request 120Hz via AttachedSurfaceControl
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.decorView.post {
                    try {
                        val surfaceControl = window.decorView.rootSurfaceControl
                        if (surfaceControl != null) {
                            val method = surfaceControl.javaClass.getMethod("setFrameRate", Float::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                            method.invoke(surfaceControl, 120.0f, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
                        }
                    } catch (e: Throwable) {
                        // ignore if unsupported on specific OEM layer
                    }
                }
            }

            // Android 6.0+ (API 23+) & Android 8.0+ (API 26+) preferred display mode & refresh rate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }

                val modes = display?.supportedModes ?: emptyArray()
                val bestMode = modes.maxByOrNull { it.refreshRate }

                val params = window.attributes
                if (bestMode != null && bestMode.refreshRate >= 90f) {
                    params.preferredDisplayModeId = bestMode.modeId
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params.preferredRefreshRate = bestMode?.refreshRate ?: 120.0f
                }
                window.attributes = params
            }
        } catch (e: Exception) {
            // fallback gracefully
        }
    }


    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        val hasReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReceiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

        if (!hasReadSms) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        if (!hasReceiveSms) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else if (hasReadSms || hasReceiveSms) {
            dashboardViewModel.syncSms()
        }
    }

    private inline fun <reified T : ViewModel> viewModelFactory(crossinline creator: () -> T): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return creator() as VM
            }
        }
    }
}
