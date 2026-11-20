package com.example.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gesturerecognition.CoreGestureType
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppGuideDialog(onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val isAr = lang == "ar"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = ElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.appGuideTitle,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(glowColor = ElectricBlue) {
                    Text(
                        text = if (isAr) "ما هو تطبيق NEGM GESTURE؟" else "What is NEGM GESTURE?",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAr) {
                            "تطبيق ذكاء اصطناعي يتيح لك التحكم في وظائف هاتفك دون لمس الشاشة عبر كاميرا الهاتف، باستخدام محرك Google MediaPipe لتتبع 21 نقطة مفصلية في اليد محلياً وبسرعة عالية."
                        } else {
                            "An on-device AI gesture controller that lets you operate phone features touch-free via CameraX and Google MediaPipe 21-point hand tracking."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                GlassCard(glowColor = CyberViolet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = CyberViolet)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "كيف تبدأ التحكم؟" else "How to Start",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberViolet
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr) {
                            "1. اضغط على START GESTURE من الشاشة الرئيسية وامنح إذن الكاميرا.\n" +
                                "2. وجّه يدك أمام الكاميرا حتى يظهر الإطار الضوئي ونقاط المفاصل.\n" +
                                "3. ثبّت الإشارة حتى يكتمل مؤشر التثبيت لتنفيذ الأمر مرة واحدة دون تكرار.\n" +
                                "4. خصّص كل إشارة أو أنشئ ملفات تحكم متعددة من تبويب (حركاتي)."
                        } else {
                            "1. Tap START GESTURE on the Dashboard and grant Camera permission.\n" +
                                "2. Hold your hand in frame until the glowing skeleton overlay locks on.\n" +
                                "3. Keep the pose steady until the Hold Lock ring completes.\n" +
                                "4. Remap actions or create multiple profiles in My Gestures."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                GlassCard(glowColor = NeonMint) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = NeonMint)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "الإشارات المدعومة (9 حركات)" else "Supported Gestures (9)",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonMint
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    CoreGestureType.entries.forEach { g ->
                        Text(
                            text = "${g.badgeSymbol} ${if (isAr) g.titleAr else g.titleEn} → ${if (isAr) g.defaultAction.labelAr else g.defaultAction.labelEn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }

                GlassCard(glowColor = ElectricBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "الخصوصية والأمان" else "Privacy & Security",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.signInPrivacyNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = ElectricBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}
