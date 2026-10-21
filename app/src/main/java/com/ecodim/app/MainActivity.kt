package com.ecodim.app

import android.Manifest
import android.app.StatusBarManager
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    // Bumped on resume so permission/settings checks re-run after visiting system settings.
    private val resumeCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EcoDimTheme {
                MainScreen(resumeCount.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount.intValue++
    }
}

@Composable
private fun MainScreen(resumeCount: Int) {
    val context = LocalContext.current
    val active by Dimmer.active.collectAsState()
    val level by Dimmer.level.collectAsState()
    val warmth by Dimmer.warmth.collectAsState()
    val schedule by Dimmer.schedule.collectAsState()
    val running by Dimmer.serviceRunning.collectAsState()

    val serviceEnabled = remember(resumeCount, running) { Dimmer.isServiceEnabled(context) }
    val notificationsAllowed = remember(resumeCount) { Notifications.canPost(context) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun openAccessibilitySettings() =
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun onPowerTap() {
        if (!serviceEnabled) {
            openAccessibilitySettings()
            return
        }
        if (!active && !notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Dimmer.toggle()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("EcoDim", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                !serviceEnabled -> "One step left: switch on the EcoDim service."
                active -> "Dimming at ${(level * 100).roundToInt()}%"
                else -> "Off"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!serviceEnabled) {
            SetupCard(onOpenSettings = ::openAccessibilitySettings)
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            PowerButton(on = active && serviceEnabled, level = level, onClick = ::onPowerTap)
        }

        Section("Strength") {
            LabeledSlider(
                value = level,
                range = Dimmer.MIN_LEVEL..Dimmer.MAX_LEVEL,
                startLabel = "Lighter",
                endLabel = "Darker",
                valueLabel = "${(level * 100).roundToInt()}%",
                onChange = Dimmer::setLevel,
            )
        }

        Section("Warm tint") {
            LabeledSlider(
                value = warmth,
                range = 0f..1f,
                startLabel = "None",
                endLabel = "Amber",
                valueLabel = if (warmth < 0.01f) "Off" else "${(warmth * 100).roundToInt()}%",
                onChange = Dimmer::setWarmth,
            )
        }

        Section("Schedule") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Turn on and off automatically", Modifier.weight(1f))
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { Dimmer.setSchedule(schedule.copy(enabled = it)) },
                )
            }
            if (schedule.enabled) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeButton("On at", schedule.startMinute, Modifier.weight(1f)) {
                        Dimmer.setSchedule(schedule.copy(startMinute = it))
                    }
                    TimeButton("Off at", schedule.endMinute, Modifier.weight(1f)) {
                        Dimmer.setSchedule(schedule.copy(endMinute = it))
                    }
                }
            }
        }

        Section("Quick access") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(onClick = { requestAddTile(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add Quick Settings tile")
                }
            } else {
                Body("Edit your Quick Settings panel and drag the EcoDim tile in.")
            }
            if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow notification controls")
                }
            }
        }

        Section("If the screen gets too dark") {
            Body("Strength stops at 90% so you can always find a way out:")
            Spacer(Modifier.height(6.dp))
            Body("• Tap the EcoDim tile in Quick Settings")
            Body("• Tap Turn off on the EcoDim notification")
            Body("• Hold both volume keys, after turning on the EcoDim shortcut in Accessibility settings")
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = ::openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Set up volume-key shortcut")
            }
        }
    }
}

@Composable
private fun SetupCard(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    Section("Set up", highlight = true) {
        Body("EcoDim dims through an Accessibility service. That's what lets it darken the status bar and notification shade too. It doesn't read anything on your screen.")
        Spacer(Modifier.height(10.dp))
        Body("Open Accessibility, find EcoDim (often under Installed or Downloaded apps), and switch it on.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open Accessibility settings")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(Modifier.height(14.dp))
            Body("Switch greyed out, or a \"Restricted setting\" message? Android does that for apps installed from an APK. Open App info, tap ⋮ in the corner, choose Allow restricted settings, then try again.")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open App info")
            }
        }
    }
}

@Composable
private fun PowerButton(on: Boolean, level: Float, onClick: () -> Unit) {
    val fill by animateColorAsState(if (on) Amber else Color.Transparent, label = "fill")
    val text by animateColorAsState(
        if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        label = "text",
    )
    Box(
        modifier = Modifier
            .size(176.dp)
            .clip(CircleShape)
            .background(fill)
            .border(BorderStroke(2.dp, if (on) Amber else MaterialTheme.colorScheme.outline), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (on) "ON" else "OFF", color = text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                if (on) "${(level * 100).roundToInt()}% dim" else "Tap to dim",
                color = text.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun Section(title: String, highlight: Boolean = false, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (highlight) Amber else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(18.dp),
            )
            .padding(16.dp),
    ) {
        Text(
            title.uppercase(),
            color = if (highlight) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun LabeledSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    Text(valueLabel, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    Slider(value = value, onValueChange = onChange, valueRange = range)
    Row {
        Text(startLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(endLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun TimeButton(label: String, minute: Int, modifier: Modifier, onPicked: (Int) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = modifier,
        onClick = {
            TimePickerDialog(
                context,
                { _, h, m -> onPicked(h * 60 + m) },
                minute / 60,
                minute % 60,
                DateFormat.is24HourFormat(context),
            ).show()
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMinuteOfDay(context, minute), fontSize = 18.sp)
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), fontSize = 15.sp, lineHeight = 21.sp)
}

private fun requestAddTile(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, DimTileService::class.java),
        context.getString(R.string.tile_label),
        Icon.createWithResource(context, R.drawable.ic_tile),
        context.mainExecutor,
    ) { }
}
