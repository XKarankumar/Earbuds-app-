package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.Exercise3DModelType
import com.example.model.ExerciseItem
import com.example.model.MuscleGroup
import com.example.ui.theme.LocalGlassPalette
import kotlin.math.cos
import kotlin.math.sin

private data class Vec3(val x: Float, val y: Float, val z: Float)
private data class BoneSegment(
    val a: Vec3,
    val b: Vec3,
    val thickness: Float,
    val isTargetMuscle: Boolean = false,
    val isEquipment: Boolean = false
)

/**
 * Interactive 3D perspective-projected biomechanical exercise demonstration renderer.
 * Supports touch 360-degree drag orbit, camera angle presets, play/pause animation,
 * equipment 3D geometry, and volumetric neon target muscle highlighting.
 */
@Composable
fun Exercise3DCanvas(
    exercise: ExerciseItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = LocalGlassPalette.current
    var yawDeg by remember(exercise.id) { mutableFloatStateOf(28f) }
    var pitchDeg by remember(exercise.id) { mutableFloatStateOf(10f) }
    var isPlaying by remember(exercise.id) { mutableStateOf(true) }

    val transition = rememberInfiniteTransition(label = "exercise_3d_anim")
    val animPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rep_cycle"
    )
    val t = if (isPlaying) animPhase else 0.5f

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 170.dp else 280.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06090F), Color(0xFF000000))
                    )
                )
                .border(1.dp, palette.glassBorderTop, RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yawDeg = (yawDeg + dragAmount.x * 0.45f) % 360f
                        pitchDeg = (pitchDeg - dragAmount.y * 0.35f).coerceIn(-55f, 55f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.54f
                val scale = minOf(size.width, size.height) * 0.38f

                // 3D floor perspective grid
                val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
                val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

                fun project(v: Vec3): Pair<Offset, Float> {
                    val x1 = v.x * cos(yawRad) - v.z * sin(yawRad)
                    val z1 = v.x * sin(yawRad) + v.z * cos(yawRad)
                    val y2 = v.y * cos(pitchRad) - z1 * sin(pitchRad)
                    val z2 = v.y * sin(pitchRad) + z1 * cos(pitchRad)
                    val perspective = 3.6f / (3.6f + z2).coerceAtLeast(0.8f)
                    return Offset(cx + x1 * scale * perspective, cy - y2 * scale * perspective) to z2
                }

                // Draw perspective floor ellipse / grid rings
                for (gx in -2..2) {
                    val (p1, _) = project(Vec3(gx * 0.55f, -0.98f, -1.1f))
                    val (p2, _) = project(Vec3(gx * 0.55f, -0.98f, 1.1f))
                    drawLine(Color.White.copy(alpha = 0.08f), p1, p2, strokeWidth = 1.dp.toPx())
                }
                for (gz in -2..2) {
                    val (p1, _) = project(Vec3(-1.1f, -0.98f, gz * 0.55f))
                    val (p2, _) = project(Vec3(1.1f, -0.98f, gz * 0.55f))
                    drawLine(Color.White.copy(alpha = 0.08f), p1, p2, strokeWidth = 1.dp.toPx())
                }

                val segments = buildPoseSegments(exercise.modelType, exercise.muscleGroup, t)
                val projected = segments.map { seg ->
                    val (pa, za) = project(seg.a)
                    val (pb, zb) = project(seg.b)
                    Triple(seg, pa to pb, (za + zb) * 0.5f)
                }.sortedByDescending { it.third } // painter's algorithm far-to-near

                projected.forEach { (seg, pts, depth) ->
                    val (pa, pb) = pts
                    val depthScale = (3.6f / (3.6f + depth).coerceAtLeast(0.8f)).coerceIn(0.65f, 1.4f)
                    val strokeW = seg.thickness * depthScale * (if (compact) 0.7f else 1f)

                    when {
                        seg.isEquipment -> {
                            drawLine(
                                color = Color(0xFF94A3B8),
                                start = pa,
                                end = pb,
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.45f),
                                start = pa,
                                end = pb,
                                strokeWidth = strokeW * 0.25f,
                                cap = StrokeCap.Round
                            )
                        }
                        seg.isTargetMuscle -> {
                            // Outer glowing target muscle aura
                            drawLine(
                                color = Color(0xFF00E5FF).copy(alpha = 0.34f + 0.2f * t),
                                start = pa,
                                end = pb,
                                strokeWidth = strokeW * 1.95f,
                                cap = StrokeCap.Round
                            )
                            // Core highlighted muscle fiber
                            drawLine(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF10B981))
                                ),
                                start = pa,
                                end = pb,
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                        else -> {
                            drawLine(
                                color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                                start = pa,
                                end = pb,
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    if (!seg.isEquipment) {
                        drawCircle(
                            color = if (seg.isTargetMuscle) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.65f),
                            radius = strokeW * 0.34f,
                            center = pa
                        )
                    }
                }
            }

            if (!compact) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ViewInAr,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (t < 0.5f) "3D • Eccentric" else "3D • Concentric",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause 3D animation" else "Play 3D animation",
                        tint = Color.White
                    )
                }
            }
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = yawDeg in -10f..10f,
                    onClick = { yawDeg = 0f; pitchDeg = 6f },
                    label = { Text("Front 3D") }
                )
                FilterChip(
                    selected = yawDeg in 20f..40f,
                    onClick = { yawDeg = 28f; pitchDeg = 10f },
                    label = { Text("3/4 View") }
                )
                FilterChip(
                    selected = yawDeg in 75f..105f,
                    onClick = { yawDeg = 88f; pitchDeg = 6f },
                    label = { Text("Side Profile") }
                )
            }
        }
    }
}

private fun buildPoseSegments(
    model: Exercise3DModelType,
    group: MuscleGroup,
    t: Float
): List<BoneSegment> {
    val list = mutableListOf<BoneSegment>()
    val chestGlow = group == MuscleGroup.CHEST
    val backGlow = group == MuscleGroup.BACK
    val shoulderGlow = group == MuscleGroup.SHOULDERS
    val bicepGlow = group == MuscleGroup.BICEPS
    val tricepGlow = group == MuscleGroup.TRICEPS
    val legGlow = group == MuscleGroup.LEGS
    val absGlow = group == MuscleGroup.ABS
    val forearmGlow = group == MuscleGroup.FOREARMS
    val calfGlow = group == MuscleGroup.CALVES
    val cardioGlow = group == MuscleGroup.CARDIO

    when (model) {
        Exercise3DModelType.BENCH_PRESS,
        Exercise3DModelType.INCLINE_PRESS,
        Exercise3DModelType.SKULL_CRUSHER -> {
            // Bench + Athlete supine + pressing arms & barbell
            val benchAngle = if (model == Exercise3DModelType.INCLINE_PRESS) 0.28f else 0f
            list += BoneSegment(Vec3(-0.75f, -0.42f, 0f), Vec3(0.70f, -0.42f + benchAngle, 0f), 18f, isEquipment = true)
            list += BoneSegment(Vec3(-0.6f, -0.95f, 0f), Vec3(-0.6f, -0.42f, 0f), 10f, isEquipment = true)
            list += BoneSegment(Vec3(0.55f, -0.95f, 0f), Vec3(0.55f, -0.42f + benchAngle, 0f), 10f, isEquipment = true)

            val hip = Vec3(-0.22f, -0.30f, 0f)
            val shoulder = Vec3(0.38f, -0.28f + benchAngle * 0.85f, 0f)
            val head = Vec3(0.58f, -0.22f + benchAngle, 0f)
            list += BoneSegment(hip, shoulder, 24f, isTargetMuscle = chestGlow || absGlow)
            list += BoneSegment(shoulder, head, 18f)

            // Legs planted
            for (side in listOf(-0.18f, 0.18f)) {
                val knee = Vec3(-0.56f, -0.38f, side)
                val foot = Vec3(-0.64f, -0.95f, side)
                list += BoneSegment(hip.copy(z = side * 0.5f), knee, 16f)
                list += BoneSegment(knee, foot, 13f)
            }

            // Pressing barbell vertical range
            val handY = if (model == Exercise3DModelType.SKULL_CRUSHER) 0.25f + t * 0.30f else -0.05f + t * 0.58f
            val handX = if (model == Exercise3DModelType.SKULL_CRUSHER) 0.50f - t * 0.12f else 0.36f
            for (side in listOf(-0.34f, 0.34f)) {
                val sh = shoulder.copy(z = side * 0.55f)
                val elbow = Vec3(handX - 0.10f, (sh.y + handY) * 0.45f, side * 0.95f)
                val hand = Vec3(handX, handY, side)
                list += BoneSegment(sh, elbow, 14f, isTargetMuscle = tricepGlow || chestGlow || shoulderGlow)
                list += BoneSegment(elbow, hand, 12f, isTargetMuscle = tricepGlow)
            }
            // Barbell
            list += BoneSegment(Vec3(handX, handY, -0.78f), Vec3(handX, handY, 0.78f), 9f, isEquipment = true)
            list += BoneSegment(Vec3(handX, handY - 0.16f, -0.68f), Vec3(handX, handY + 0.16f, -0.68f), 16f, isEquipment = true)
            list += BoneSegment(Vec3(handX, handY - 0.16f, 0.68f), Vec3(handX, handY + 0.16f, 0.68f), 16f, isEquipment = true)
        }

        Exercise3DModelType.PUSH_UP,
        Exercise3DModelType.PLANK -> {
            val drop = if (model == Exercise3DModelType.PLANK) 0f else t * 0.28f
            val ankle = Vec3(-0.82f, -0.88f, 0f)
            val hip = Vec3(-0.15f, -0.54f - drop * 0.5f, 0f)
            val shoulder = Vec3(0.45f, -0.26f - drop, 0f)
            val head = Vec3(0.68f, -0.18f - drop, 0f)
            list += BoneSegment(ankle, hip, 16f, isTargetMuscle = legGlow)
            list += BoneSegment(hip, shoulder, 24f, isTargetMuscle = absGlow || chestGlow)
            list += BoneSegment(shoulder, head, 17f)
            for (side in listOf(-0.24f, 0.24f)) {
                val elbow = Vec3(0.36f, -0.60f - drop * 0.5f, side * 1.15f)
                val hand = Vec3(0.45f, -0.94f, side)
                list += BoneSegment(shoulder.copy(z = side * 0.65f), elbow, 13f, isTargetMuscle = chestGlow || tricepGlow)
                list += BoneSegment(elbow, hand, 11f, isTargetMuscle = tricepGlow)
            }
        }

        Exercise3DModelType.PULL_UP,
        Exercise3DModelType.HANGING_LEG_RAISE -> {
            // Pull-up bar
            list += BoneSegment(Vec3(0f, 0.92f, -0.82f), Vec3(0f, 0.92f, 0.82f), 10f, isEquipment = true)
            val lift = if (model == Exercise3DModelType.PULL_UP) t * 0.36f else 0f
            val legLift = if (model == Exercise3DModelType.HANGING_LEG_RAISE) t * 0.68f else 0f
            val shoulder = Vec3(0f, 0.30f + lift, 0f)
            val hip = Vec3(0f, -0.20f + lift, 0f)
            val head = Vec3(0f, 0.52f + lift, 0f)
            list += BoneSegment(hip, shoulder, 25f, isTargetMuscle = backGlow || absGlow)
            list += BoneSegment(shoulder, head, 17f)
            for (side in listOf(-0.22f, 0.22f)) {
                val hand = Vec3(0f, 0.92f, side * 1.45f)
                val elbow = Vec3(0.08f, 0.58f + lift * 0.4f, side * 1.65f)
                list += BoneSegment(shoulder.copy(z = side * 0.65f), elbow, 13f, isTargetMuscle = backGlow)
                list += BoneSegment(elbow, hand, 11f, isTargetMuscle = bicepGlow || forearmGlow)

                val knee = Vec3(legLift * 0.55f, -0.58f + lift + legLift * 0.55f, side * 0.55f)
                val foot = Vec3(legLift * 0.92f, -0.92f + lift + legLift * 0.85f, side * 0.55f)
                list += BoneSegment(hip.copy(z = side * 0.4f), knee, 15f, isTargetMuscle = absGlow)
                list += BoneSegment(knee, foot, 12f)
            }
        }

        Exercise3DModelType.BARBELL_SQUAT,
        Exercise3DModelType.DEADLIFT,
        Exercise3DModelType.ROMANIAN_DEADLIFT,
        Exercise3DModelType.LUNGES,
        Exercise3DModelType.LEG_PRESS -> {
            val depth = t * 0.42f
            val hip = Vec3(-0.14f * (depth / 0.42f), -0.05f - depth, 0f)
            val shoulder = Vec3(0.10f, 0.48f - depth * 0.85f, 0f)
            val head = Vec3(0.14f, 0.72f - depth * 0.85f, 0f)
            list += BoneSegment(hip, shoulder, 25f, isTargetMuscle = backGlow || absGlow)
            list += BoneSegment(shoulder, head, 17f)

            for (side in listOf(-0.20f, 0.20f)) {
                val stride = if (model == Exercise3DModelType.LUNGES) (if (side > 0) 0.35f else -0.35f) else 0f
                val knee = Vec3(0.22f + stride * 0.7f, -0.50f - depth * 0.35f, side)
                val foot = Vec3(stride, -0.95f, side)
                list += BoneSegment(hip.copy(z = side * 0.5f), knee, 18f, isTargetMuscle = legGlow || backGlow)
                list += BoneSegment(knee, foot, 14f, isTargetMuscle = legGlow || calfGlow)

                val hand = if (model == Exercise3DModelType.BARBELL_SQUAT) {
                    Vec3(0.05f, 0.48f - depth * 0.85f, side * 1.55f)
                } else {
                    Vec3(0.18f, -0.18f - depth * 0.75f, side * 1.05f)
                }
                list += BoneSegment(shoulder.copy(z = side * 0.65f), hand, 12f, isTargetMuscle = forearmGlow)
            }

            // Barbell for squat/deadlift/RDL
            if (model != Exercise3DModelType.LUNGES) {
                val barY = if (model == Exercise3DModelType.BARBELL_SQUAT) 0.48f - depth * 0.85f else -0.18f - depth * 0.75f
                val barX = if (model == Exercise3DModelType.BARBELL_SQUAT) 0.05f else 0.18f
                list += BoneSegment(Vec3(barX, barY, -0.78f), Vec3(barX, barY, 0.78f), 9f, isEquipment = true)
                list += BoneSegment(Vec3(barX, barY - 0.16f, -0.68f), Vec3(barX, barY + 0.16f, -0.68f), 16f, isEquipment = true)
                list += BoneSegment(Vec3(barX, barY - 0.16f, 0.68f), Vec3(barX, barY + 0.16f, 0.68f), 16f, isEquipment = true)
            }
        }

        else -> {
            // Standing upper-body / isolation / cardio dynamic athletic pose
            val hip = Vec3(0f, -0.05f, 0f)
            val shoulder = Vec3(0f, 0.48f, 0f)
            val head = Vec3(0f, 0.72f, 0f)
            list += BoneSegment(hip, shoulder, 24f, isTargetMuscle = chestGlow || backGlow || absGlow || cardioGlow)
            list += BoneSegment(shoulder, head, 17f)

            val legSwing = if (cardioGlow) (t - 0.5f) * 0.45f else 0f
            val calfLift = if (calfGlow) t * 0.12f else 0f

            for (side in listOf(-0.18f, 0.18f)) {
                val phaseDir = if (side > 0) 1f else -1f
                val knee = Vec3(legSwing * phaseDir, -0.50f + calfLift, side)
                val foot = Vec3(-legSwing * phaseDir * 0.8f, -0.95f + calfLift, side)
                list += BoneSegment(hip.copy(z = side * 0.5f), knee, 17f, isTargetMuscle = legGlow || cardioGlow)
                list += BoneSegment(knee, foot, 14f, isTargetMuscle = calfGlow || cardioGlow)

                // Dynamic arm kinematics by model type
                val (elbow, hand) = when (model) {
                    Exercise3DModelType.OVERHEAD_PRESS -> {
                        val liftY = 0.46f + t * 0.46f
                        Vec3(0.06f, liftY - 0.14f, side * 1.35f) to Vec3(0.04f, liftY, side * 1.25f)
                    }
                    Exercise3DModelType.LATERAL_RAISE -> {
                        val raise = t * 0.55f
                        Vec3(0.05f, 0.25f + raise * 0.5f, side * (1.25f + t * 0.85f)) to
                            Vec3(0.06f, 0.05f + raise, side * (1.45f + t * 1.45f))
                    }
                    Exercise3DModelType.BARBELL_CURL,
                    Exercise3DModelType.HAMMER_CURL,
                    Exercise3DModelType.PREACHER_CURL,
                    Exercise3DModelType.REVERSE_CURL -> {
                        val curlY = 0.05f + t * 0.40f
                        val curlX = 0.14f + t * 0.14f
                        Vec3(0.04f, 0.16f, side * 1.15f) to Vec3(curlX, curlY, side * 1.15f)
                    }
                    Exercise3DModelType.TRICEP_PUSHDOWN -> {
                        val pushY = 0.34f - t * 0.34f
                        Vec3(0.05f, 0.18f, side * 1.1f) to Vec3(0.18f, pushY, side * 1.0f)
                    }
                    Exercise3DModelType.CABLE_FLY -> {
                        val close = t * 0.85f
                        Vec3(0.15f + close * 0.15f, 0.38f, side * (1.9f - close * 1.25f)) to
                            Vec3(0.32f + close * 0.15f, 0.35f, side * (2.2f - close * 1.95f))
                    }
                    else -> {
                        val swing = (t - 0.5f) * 0.35f * phaseDir
                        Vec3(swing, 0.22f, side * 1.2f) to Vec3(swing * 1.4f, 0.02f, side * 1.25f)
                    }
                }
                list += BoneSegment(shoulder.copy(z = side * 0.65f), elbow, 13f, isTargetMuscle = shoulderGlow || tricepGlow || bicepGlow)
                list += BoneSegment(elbow, hand, 11f, isTargetMuscle = bicepGlow || forearmGlow || tricepGlow)
                // Dumbbell / cable handle
                list += BoneSegment(hand.copy(x = hand.x - 0.08f), hand.copy(x = hand.x + 0.08f), 11f, isEquipment = true)
            }
        }
    }
    return list
}
