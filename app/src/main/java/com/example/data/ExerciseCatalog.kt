package com.example.data

import com.example.model.DifficultyLevel
import com.example.model.Exercise3DModelType
import com.example.model.ExerciseItem
import com.example.model.MuscleGroup

object ExerciseCatalog {

    val allExercises: List<ExerciseItem> = listOf(
        // ======================== CHEST (4) ========================
        ExerciseItem(
            id = "chest_bench_press",
            name = "Barbell Bench Press",
            muscleGroup = MuscleGroup.CHEST,
            primaryMuscles = listOf("Pectoralis Major (Sternal)"),
            secondaryMuscles = listOf("Anterior Deltoid", "Triceps Brachii"),
            equipment = "Flat Bench & Olympic Barbell",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.BENCH_PRESS,
            postureGuide = "Pin shoulder blades back and down onto the bench, maintain a slight natural lumbar arch, and plant both feet firmly on the floor.",
            steps = listOf(
                "Lie flat with eyes directly beneath the barbell and grip slightly wider than shoulder width.",
                "Unrack the bar with locked elbows and stabilize it directly over your mid-chest.",
                "Inhale and lower the bar under control to your lower sternum with elbows tucked at ~45°.",
                "Press powerfully upward in a slight arc back over your shoulders without bouncing off the chest."
            ),
            defaultSets = 4,
            defaultReps = "8–10 reps",
            defaultRestSec = 90,
            commonMistakes = listOf(
                "Flaring elbows out to 90°, stressing the shoulder capsule.",
                "Bouncing the barbell off the ribcage or lifting glutes off the bench.",
                "Bending wrists backward instead of stacking bar over forearms."
            ),
            safetyInstructions = listOf(
                "Always use safety spotter arms or a spotter on heavy working sets.",
                "Never use a thumbless 'suicide grip' over your chest."
            ),
            caloriesPerSet = 14
        ),
        ExerciseItem(
            id = "chest_incline_db",
            name = "Incline Dumbbell Press",
            muscleGroup = MuscleGroup.CHEST,
            primaryMuscles = listOf("Upper Pectorals (Clavicular)"),
            secondaryMuscles = listOf("Anterior Deltoid", "Triceps"),
            equipment = "30° Incline Bench & Dumbbells",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.INCLINE_PRESS,
            postureGuide = "Set bench angle to 30°–35°, keep upper back pinned, chest proud, and wrists neutral.",
            steps = listOf(
                "Kick dumbbells up to shoulder height and press to start position above upper chest.",
                "Lower slowly for 3 seconds until dumbbells flank your upper chest with elbows angled 45°.",
                "Drive through your upper chest to press dumbbells smoothly together without clanging."
            ),
            defaultSets = 3,
            defaultReps = "10–12 reps",
            defaultRestSec = 75,
            commonMistakes = listOf(
                "Setting bench too steep (60°+), shifting load to front deltoids.",
                "Cutting range of motion short at the bottom."
            ),
            safetyInstructions = listOf(
                "Lower dumbbells to thighs before sitting up rather than dropping sideways.",
                "Stop immediately if you feel sharp anterior shoulder pinching."
            ),
            caloriesPerSet = 12
        ),
        ExerciseItem(
            id = "chest_pushup",
            name = "Weighted / Tempo Push-Up",
            muscleGroup = MuscleGroup.CHEST,
            primaryMuscles = listOf("Pectoralis Major", "Serratus Anterior"),
            secondaryMuscles = listOf("Triceps", "Core Stabilizers"),
            equipment = "Bodyweight / Optional Plate",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.PUSH_UP,
            postureGuide = "Maintain a rigid plank line from ears through hips to ankles; brace abs and glutes.",
            steps = listOf(
                "Place hands slightly outside shoulder width with fingers spread for grip.",
                "Lower entire body as one rigid unit until chest hovers 2 cm above floor.",
                "Push the floor away until arms extend and protract shoulder blades slightly at top."
            ),
            defaultSets = 3,
            defaultReps = "12–18 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Letting hips sag or piking glutes upward.",
                "Leading downward with the chin instead of the chest."
            ),
            safetyInstructions = listOf(
                "Use push-up bars or fists if flat palms cause wrist discomfort.",
                "Drop to knees if lumbar spine begins sagging."
            ),
            caloriesPerSet = 9
        ),
        ExerciseItem(
            id = "chest_cable_fly",
            name = "Standing Cable Chest Fly",
            muscleGroup = MuscleGroup.CHEST,
            primaryMuscles = listOf("Inner & Mid Pectorals"),
            secondaryMuscles = listOf("Anterior Deltoid"),
            equipment = "Dual Cable Crossover Machine",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.CABLE_FLY,
            postureGuide = "Step forward into a staggered athletic stance, torso leaned 15° forward, soft fixed bend in elbows.",
            steps = listOf(
                "Hold D-handles at chest height with wide open arms and stretched pectorals.",
                "Sweep hands inward in a wide hugging arc until knuckles meet in front of lower chest.",
                "Squeeze pectorals hard for 1 second, then resist the cables slowly back to stretch."
            ),
            defaultSets = 3,
            defaultReps = "12–15 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Turning the fly into a press by bending elbows excessively.",
                "Using momentum by swinging the torso backward."
            ),
            safetyInstructions = listOf(
                "Do not let arms travel far behind torso plane to protect anterior shoulder ligaments."
            ),
            caloriesPerSet = 8
        ),

        // ======================== BACK (4) ========================
        ExerciseItem(
            id = "back_pullup",
            name = "Wide-Grip Pull-Up",
            muscleGroup = MuscleGroup.BACK,
            primaryMuscles = listOf("Latissimus Dorsi", "Teres Major"),
            secondaryMuscles = listOf("Biceps", "Rhomboids", "Lower Traps"),
            equipment = "Pull-Up Bar",
            difficulty = DifficultyLevel.ADVANCED,
            modelType = Exercise3DModelType.PULL_UP,
            postureGuide = "Hang with active shoulders (depress scapulae slightly), ribs tucked, legs crossed or straight ahead in hollow hold.",
            steps = listOf(
                "Grip the bar 1.5x shoulder width with palms facing away.",
                "Initiate by pulling shoulder blades down, then drive elbows toward your hips.",
                "Pull until collarbone approaches the bar, pause briefly, and lower cleanly to full extension."
            ),
            defaultSets = 4,
            defaultReps = "6–12 reps",
            defaultRestSec = 90,
            commonMistakes = listOf(
                "Kipping or swinging legs to clear the bar.",
                "Half-repping without extending elbows at the bottom."
            ),
            safetyInstructions = listOf(
                "Warm up rotator cuffs and use resistance band assistance if needed to preserve form."
            ),
            caloriesPerSet = 13
        ),
        ExerciseItem(
            id = "back_lat_pulldown",
            name = "Lat Pulldown",
            muscleGroup = MuscleGroup.BACK,
            primaryMuscles = listOf("Latissimus Dorsi"),
            secondaryMuscles = listOf("Biceps", "Middle Trapezius", "Rhomboids"),
            equipment = "Lat Pulldown Cable Station",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.LAT_PULLDOWN,
            postureGuide = "Lock thighs firmly under pad, lean torso back ~15°, keep chest lifted.",
            steps = listOf(
                "Grasp wide bar outside shoulder width and let lats stretch upward at start.",
                "Pull elbows straight down toward your pockets until bar touches upper chest.",
                "Control the ascent over 2–3 seconds without letting weight stack slam."
            ),
            defaultSets = 3,
            defaultReps = "10–12 reps",
            defaultRestSec = 75,
            commonMistakes = listOf(
                "Pulling the bar behind the neck (hazardous to cervical spine & shoulders).",
                "Leaning far backward to turn it into a row."
            ),
            safetyInstructions = listOf(
                "Always pull to the front collarbone, never behind the head."
            ),
            caloriesPerSet = 10
        ),
        ExerciseItem(
            id = "back_barbell_row",
            name = "Bent-Over Barbell Row",
            muscleGroup = MuscleGroup.BACK,
            primaryMuscles = listOf("Rhomboids", "Mid Traps", "Latissimus Dorsi"),
            secondaryMuscles = listOf("Erector Spinae", "Rear Delts", "Biceps"),
            equipment = "Olympic Barbell",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.BARBELL_ROW,
            postureGuide = "Hinge at hips to a flat-back 45° torso angle, knees softly bent, spine completely neutral.",
            steps = listOf(
                "Hold bar just outside knees with braced core and lats engaged.",
                "Row barbell smoothly toward your lower ribs / belly button by squeezing shoulder blades.",
                "Lower with control while keeping torso angle locked."
            ),
            defaultSets = 4,
            defaultReps = "8–10 reps",
            defaultRestSec = 90,
            commonMistakes = listOf(
                "Rounding the lower back under heavy weight.",
                "Standing upright by jerking torso up on every rep."
            ),
            safetyInstructions = listOf(
                "Keep lumbar spine braced with deep diaphragmatic breath; reduce load if lower back fatigues first."
            ),
            caloriesPerSet = 13
        ),
        ExerciseItem(
            id = "back_deadlift",
            name = "Conventional Barbell Deadlift",
            muscleGroup = MuscleGroup.BACK,
            primaryMuscles = listOf("Erector Spinae", "Gluteus Maximus", "Hamstrings"),
            secondaryMuscles = listOf("Trapezius", "Lats", "Forearms", "Core"),
            equipment = "Olympic Barbell & Bumper Plates",
            difficulty = DifficultyLevel.ADVANCED,
            modelType = Exercise3DModelType.DEADLIFT,
            postureGuide = "Bar over mid-foot, shins vertical against bar, chest tall, lats packed ('squeeze oranges in armpits'), flat neutral spine.",
            steps = listOf(
                "Hinge hips back, grip bar just outside shins, pull slack out of the bar until you hear plates click.",
                "Push the floor away with your legs while keeping the bar dragging along your legs.",
                "Lock hips forward cleanly at top without hyperextending lumbar spine, then hinge down smoothly."
            ),
            defaultSets = 3,
            defaultReps = "5–6 reps",
            defaultRestSec = 150,
            commonMistakes = listOf(
                "Yanking bar off floor with rounded lower back.",
                "Letting bar drift forward away from shins."
            ),
            safetyInstructions = listOf(
                "Never round lumbar spine under maximal load. Consider a lifting belt on top sets."
            ),
            caloriesPerSet = 18
        ),

        // ======================== SHOULDERS (3) ========================
        ExerciseItem(
            id = "sh_overhead_press",
            name = "Standing Overhead Barbell Press",
            muscleGroup = MuscleGroup.SHOULDERS,
            primaryMuscles = listOf("Anterior & Medial Deltoid"),
            secondaryMuscles = listOf("Triceps", "Upper Chest", "Core"),
            equipment = "Barbell & Squat Rack",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.OVERHEAD_PRESS,
            postureGuide = "Squeeze glutes and abs hard to prevent lower-back hyperextension; stack wrists vertically over elbows.",
            steps = listOf(
                "Unrack bar onto upper chest with hands just outside shoulders and elbows slightly ahead of bar.",
                "Tuck chin slightly back and press bar vertically overhead.",
                "Push head forward through the window once bar clears forehead, locking directly over mid-foot."
            ),
            defaultSets = 4,
            defaultReps = "6–10 reps",
            defaultRestSec = 90,
            commonMistakes = listOf(
                "Excessively arching the lumbar spine as weight goes overhead.",
                "Pressing the bar forward instead of straight vertically."
            ),
            safetyInstructions = listOf(
                "Keep glutes squeezed throughout every rep to shield your lower back."
            ),
            caloriesPerSet = 12
        ),
        ExerciseItem(
            id = "sh_lateral_raise",
            name = "Dumbbell Lateral Raise",
            muscleGroup = MuscleGroup.SHOULDERS,
            primaryMuscles = listOf("Lateral (Medial) Deltoid"),
            secondaryMuscles = listOf("Supraspinatus", "Upper Traps"),
            equipment = "Pair of Light–Medium Dumbbells",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.LATERAL_RAISE,
            postureGuide = "Stand tall with slight 10° forward lean, lead movement in the scapular plane (~30° forward of sides).",
            steps = listOf(
                "Hold dumbbells at sides with soft 15° elbow bend.",
                "Sweep arms outward and upward until elbows reach shoulder height (pinky level or slightly lower).",
                "Lower slowly over 3 seconds maintaining tension on side delts."
            ),
            defaultSets = 4,
            defaultReps = "12–15 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Shrugging traps upward toward ears.",
                "Using heavy dumbbells and swinging hips for momentum."
            ),
            safetyInstructions = listOf(
                "Avoid pouring-pitcher internal rotation if it causes shoulder impingement; keep thumbs neutral or slightly up."
            ),
            caloriesPerSet = 8
        ),
        ExerciseItem(
            id = "sh_face_pull",
            name = "Rope Cable Face Pull",
            muscleGroup = MuscleGroup.SHOULDERS,
            primaryMuscles = listOf("Posterior Deltoid", "Infraspinatus"),
            secondaryMuscles = listOf("Rhomboids", "Mid Traps"),
            equipment = "Cable Machine & Rope Attachment",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.FACE_PULL,
            postureGuide = "Set pulley at forehead height, step back into a split stance, keep ribs down and torso vertical.",
            steps = listOf(
                "Grip rope with thumbs pointing backward toward you.",
                "Pull rope ends apart directly toward your ears/forehead while rotating thumbs backward (external rotation).",
                "Hold peak contraction for 1 second, then extend arms smoothly."
            ),
            defaultSets = 3,
            defaultReps = "15 reps",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Going too heavy and turning it into a lower-back lean.",
                "Pulling toward the chest instead of ears."
            ),
            safetyInstructions = listOf(
                "Prioritize smooth external rotation for lifelong shoulder health."
            ),
            caloriesPerSet = 7
        ),

        // ======================== BICEPS (3) ========================
        ExerciseItem(
            id = "bi_barbell_curl",
            name = "EZ-Bar / Barbell Curl",
            muscleGroup = MuscleGroup.BICEPS,
            primaryMuscles = listOf("Biceps Brachii (Short & Long Head)"),
            secondaryMuscles = listOf("Brachialis", "Forearm Flexors"),
            equipment = "EZ-Curl Bar or Straight Barbell",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.BARBELL_CURL,
            postureGuide = "Pin elbows tight beside your ribcage, chest tall, knees unlocked, no lower-back sway.",
            steps = listOf(
                "Grip bar shoulder-width with palms facing upward.",
                "Curl bar upward strictly by flexing elbows until forearms approach biceps.",
                "Squeeze biceps hard at top and lower on a slow 3-second negative."
            ),
            defaultSets = 3,
            defaultReps = "10–12 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Swinging torso backward to heave the bar up.",
                "Driving elbows far forward to cheat the top half."
            ),
            safetyInstructions = listOf(
                "Use an EZ-curl bar if straight bars strain wrists or inner elbows."
            ),
            caloriesPerSet = 8
        ),
        ExerciseItem(
            id = "bi_hammer_curl",
            name = "Dumbbell Hammer Curl",
            muscleGroup = MuscleGroup.BICEPS,
            primaryMuscles = listOf("Brachialis", "Brachioradialis"),
            secondaryMuscles = listOf("Biceps Brachii"),
            equipment = "Dumbbells",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.HAMMER_CURL,
            postureGuide = "Keep neutral 'handshake' wrist alignment throughout the entire curl.",
            steps = listOf(
                "Stand tall holding dumbbells at sides with palms facing inward.",
                "Curl weights upward while keeping elbows pinned at your sides.",
                "Pause at 90°–120° peak flexion, then lower smoothly to full stretch."
            ),
            defaultSets = 3,
            defaultReps = "10–12 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Twisting wrists outward mid-rep or swinging shoulders."
            ),
            safetyInstructions = listOf(
                "Avoid locking elbows aggressively at the bottom under heavy weight."
            ),
            caloriesPerSet = 8
        ),
        ExerciseItem(
            id = "bi_preacher_curl",
            name = "Preacher Curl",
            muscleGroup = MuscleGroup.BICEPS,
            primaryMuscles = listOf("Biceps Brachii (Short Head Peak)"),
            secondaryMuscles = listOf("Brachialis"),
            equipment = "Preacher Bench & EZ-Bar / Dumbbell",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.PREACHER_CURL,
            postureGuide = "Seat armpits snugly atop the preacher pad so upper arms cannot shift.",
            steps = listOf(
                "Grip bar with upper arms fully supported against sloped pad.",
                "Curl smoothly upward stopping just before forearm becomes vertical so tension stays on biceps.",
                "Lower slowly to 95% extension—never snap into hyperextension."
            ),
            defaultSets = 3,
            defaultReps = "10–12 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Dropping fast into hyperextension at bottom (high bicep tendon risk).",
                "Lifting hips off the seat to assist."
            ),
            safetyInstructions = listOf(
                "Control the bottom stretch slowly; never bounce out of full elbow lock."
            ),
            caloriesPerSet = 7
        ),

        // ======================== TRICEPS (3) ========================
        ExerciseItem(
            id = "tri_pushdown",
            name = "Cable Rope Tricep Pushdown",
            muscleGroup = MuscleGroup.TRICEPS,
            primaryMuscles = listOf("Triceps Brachii (Lateral & Long Head)"),
            secondaryMuscles = listOf("Anconeus"),
            equipment = "High Pulley Cable & Rope",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.TRICEP_PUSHDOWN,
            postureGuide = "Lock upper arms glued to your sides, slight forward torso hinge, wrists firm.",
            steps = listOf(
                "Grasp rope ends with elbows bent slightly above 90°.",
                "Drive hands downward and flare rope apart at the bottom to fully contract triceps.",
                "Return smoothly up until forearms touch biceps without letting elbows drift forward."
            ),
            defaultSets = 3,
            defaultReps = "12–15 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Flaring elbows outward and pressing down with body weight.",
                "Cutting upward range short."
            ),
            safetyInstructions = listOf(
                "Keep wrists neutral rather than bent back at peak extension."
            ),
            caloriesPerSet = 8
        ),
        ExerciseItem(
            id = "tri_skull_crusher",
            name = "EZ-Bar Skull Crusher",
            muscleGroup = MuscleGroup.TRICEPS,
            primaryMuscles = listOf("Triceps Brachii (Long Head)"),
            secondaryMuscles = listOf("Medial & Lateral Triceps"),
            equipment = "Flat Bench & EZ-Bar",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.SKULL_CRUSHER,
            postureGuide = "Angle upper arms slightly backward (~100° from torso) for continuous long-head tension.",
            steps = listOf(
                "Press EZ-bar over upper chest/eyes and angle arms slightly back.",
                "Bend strictly at elbows to lower bar smoothly just behind crown of head.",
                "Extend elbows cleanly back to start without letting upper arms swing."
            ),
            defaultSets = 3,
            defaultReps = "10–12 reps",
            defaultRestSec = 75,
            commonMistakes = listOf(
                "Flaring elbows wide apart like a guillotine.",
                "Lowering bar directly onto the nose/forehead."
            ),
            safetyInstructions = listOf(
                "Always lower slightly behind the top of your head and warm up elbows thoroughly first."
            ),
            caloriesPerSet = 9
        ),
        ExerciseItem(
            id = "tri_dips",
            name = "Parallel Bar Tricep Dips",
            muscleGroup = MuscleGroup.TRICEPS,
            primaryMuscles = listOf("Triceps Brachii", "Lower Pectorals"),
            secondaryMuscles = listOf("Anterior Deltoid"),
            equipment = "Parallel Dip Bars",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.TRICEP_DIP,
            postureGuide = "Keep torso upright, shoulders pressed down away from ears, elbows tracking straight backward.",
            steps = listOf(
                "Support body on locked arms with ankles crossed behind.",
                "Lower smoothly until upper arms reach parallel to floor (~90° elbow bend).",
                "Press powerfully back up without shrugging shoulders."
            ),
            defaultSets = 3,
            defaultReps = "8–12 reps",
            defaultRestSec = 75,
            commonMistakes = listOf(
                "Dropping shoulders up into ears or going overly deep causing front-shoulder strain."
            ),
            safetyInstructions = listOf(
                "Stop descent when upper arms hit parallel if you feel shoulder tightness."
            ),
            caloriesPerSet = 11
        ),

        // ======================== LEGS (4) ========================
        ExerciseItem(
            id = "legs_squat",
            name = "Barbell Back Squat",
            muscleGroup = MuscleGroup.LEGS,
            primaryMuscles = listOf("Quadriceps", "Gluteus Maximus"),
            secondaryMuscles = listOf("Adductors", "Hamstrings", "Core", "Spinal Erectors"),
            equipment = "Squat Rack & Olympic Barbell",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.BARBELL_SQUAT,
            postureGuide = "Bar centered on upper traps, feet shoulder-width with toes angled 15°–30° out, chest proud, 360° core brace.",
            steps = listOf(
                "Unrack bar, take 2–3 controlled steps back, and take a deep abdominal brace.",
                "Break simultaneously at hips and knees, tracking knees directly over toes until thighs reach parallel or below.",
                "Drive evenly through mid-foot and heels to stand tall."
            ),
            defaultSets = 4,
            defaultReps = "6–10 reps",
            defaultRestSec = 120,
            commonMistakes = listOf(
                "Knees caving inward (valgus collapse) on ascent.",
                "Heels lifting off floor or rounding lower back at bottom ('butt wink')."
            ),
            safetyInstructions = listOf(
                "Always set rack safety pins just below your bottom squat height."
            ),
            caloriesPerSet = 18
        ),
        ExerciseItem(
            id = "legs_press",
            name = "45° Leg Press",
            muscleGroup = MuscleGroup.LEGS,
            primaryMuscles = listOf("Quadriceps", "Glutes"),
            secondaryMuscles = listOf("Hamstrings", "Calves"),
            equipment = "45° Leg Press Machine",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.LEG_PRESS,
            postureGuide = "Keep lower back and tailbone pressed flat against seat pad at all times.",
            steps = listOf(
                "Place feet shoulder-width mid-sled, unlock safety handles.",
                "Lower sled slowly until knees reach ~90°–110° without lifting glutes off pad.",
                "Press smoothly upward stopping just shy of locking knees out."
            ),
            defaultSets = 4,
            defaultReps = "10–12 reps",
            defaultRestSec = 90,
            commonMistakes = listOf(
                "Hyperextending/locking knees straight under heavy load.",
                "Allowing lower back to peel off the backrest."
            ),
            safetyInstructions = listOf(
                "NEVER lock knees completely straight and NEVER place hands on knees to push."
            ),
            caloriesPerSet = 14
        ),
        ExerciseItem(
            id = "legs_lunges",
            name = "Walking Dumbbell Lunges",
            muscleGroup = MuscleGroup.LEGS,
            primaryMuscles = listOf("Quadriceps", "Gluteus Maximus"),
            secondaryMuscles = listOf("Hamstrings", "Core", "Calves"),
            equipment = "Pair of Dumbbells",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.LUNGES,
            postureGuide = "Torso tall or slight 10° athletic lean, front shin nearly vertical, rear knee hovers cleanly above floor.",
            steps = listOf(
                "Step forward onto a hip-width track (not a tightrope).",
                "Lower both knees to ~90° angles while maintaining balance.",
                "Drive through front heel to step smoothly into next stride."
            ),
            defaultSets = 3,
            defaultReps = "10 reps / leg",
            defaultRestSec = 75,
            commonMistakes = listOf(
                "Stepping inline like a tightrope causing wobble.",
                "Smashing back knee into the floor."
            ),
            safetyInstructions = listOf(
                "Control descent so rear patella kisses or hovers 2 cm above the mat."
            ),
            caloriesPerSet = 14
        ),
        ExerciseItem(
            id = "legs_rdl",
            name = "Romanian Deadlift (RDL)",
            muscleGroup = MuscleGroup.LEGS,
            primaryMuscles = listOf("Hamstrings", "Gluteus Maximus"),
            secondaryMuscles = listOf("Erector Spinae", "Forearms"),
            equipment = "Barbell or Heavy Dumbbells",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.ROMANIAN_DEADLIFT,
            postureGuide = "Unlock knees slightly (~15° fixed bend), keep spine neutral and weights brushing thighs/shins.",
            steps = listOf(
                "Stand tall with braced core and lats engaged.",
                "Push hips straight backward as if closing a car door with your glutes until deep hamstring stretch (mid-shin).",
                "Drive hips forward to stand tall without arching lower back at top."
            ),
            defaultSets = 3,
            defaultReps = "8–10 reps",
            defaultRestSec = 90,
            commonMistakes = listOf(
                "Bending knees progressively into a squat.",
                "Rounding upper/lower back to reach lower toward the floor."
            ),
            safetyInstructions = listOf(
                "Stop descent the moment your hips stop moving backward—flexibility defines your bottom point."
            ),
            caloriesPerSet = 13
        ),

        // ======================== ABS (3) ========================
        ExerciseItem(
            id = "abs_hanging_leg_raise",
            name = "Hanging Leg Raise",
            muscleGroup = MuscleGroup.ABS,
            primaryMuscles = listOf("Lower Rectus Abdominis", "Hip Flexors"),
            secondaryMuscles = listOf("Obliques", "Forearms"),
            equipment = "Pull-Up Bar or Captain's Chair",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.HANGING_LEG_RAISE,
            postureGuide = "Curl pelvis upward toward ribs at top rather than just swinging legs at hip.",
            steps = listOf(
                "Hang steadily with depressed shoulders so body does not swing.",
                "Raise legs upward and curl tailbone slightly forward at top.",
                "Lower slowly under full control to stop pendulum momentum."
            ),
            defaultSets = 3,
            defaultReps = "10–15 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Swinging back and forth using hip momentum.",
                "Stopping at 90° without curling pelvis."
            ),
            safetyInstructions = listOf(
                "Bend knees to chest if straight legs pull on your lower back."
            ),
            caloriesPerSet = 9
        ),
        ExerciseItem(
            id = "abs_cable_crunch",
            name = "Kneeling Rope Cable Crunch",
            muscleGroup = MuscleGroup.ABS,
            primaryMuscles = listOf("Rectus Abdominis (Upper & Mid)"),
            secondaryMuscles = listOf("Obliques"),
            equipment = "High Pulley Cable & Rope",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.CABLE_CRUNCH,
            postureGuide = "Keep hips stationary directly over knees; movement comes purely from spinal flexion.",
            steps = listOf(
                "Kneel beneath high pulley, hold rope handles beside temples.",
                "Exhale forcefully and curl ribs downward toward pelvis.",
                "Hold 1-second peak crunch, then slowly uncurl under tension."
            ),
            defaultSets = 3,
            defaultReps = "15 reps",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Sitting hips back toward heels instead of crunching spine.",
                "Pulling rope with arms and shoulders."
            ),
            safetyInstructions = listOf(
                "Avoid snapping neck downward; fix rope firmly against sides of head."
            ),
            caloriesPerSet = 8
        ),
        ExerciseItem(
            id = "abs_plank",
            name = "RKC Forearm Plank",
            muscleGroup = MuscleGroup.ABS,
            primaryMuscles = listOf("Transverse Abdominis", "Rectus Abdominis"),
            secondaryMuscles = listOf("Obliques", "Glutes", "Serratus"),
            equipment = "Exercise Mat",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.PLANK,
            postureGuide = "Posterior pelvic tilt (tuck tailbone slightly), squeeze glutes and quads maximally, neutral neck looking at hands.",
            steps = listOf(
                "Place forearms parallel beneath shoulders and step feet hip-width back.",
                "Pull elbows mentally toward toes without moving them to fire deep core tension.",
                "Hold rock-solid alignment while breathing smoothly through diaphragm."
            ),
            defaultSets = 3,
            defaultReps = "45–60 sec",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Allowing hips to sag and transferring load to lumbar spine.",
                "Raising hips high like a pike."
            ),
            safetyInstructions = listOf(
                "End the hold immediately when lumbar spine begins dipping."
            ),
            caloriesPerSet = 7
        ),

        // ======================== FOREARMS (3) ========================
        ExerciseItem(
            id = "fa_wrist_curl",
            name = "Seated Dumbbell Wrist Curl",
            muscleGroup = MuscleGroup.FOREARMS,
            primaryMuscles = listOf("Forearm Flexors"),
            secondaryMuscles = listOf("Grip Stabilizers"),
            equipment = "Flat Bench & Light Dumbbell / Barbell",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.WRIST_CURL,
            postureGuide = "Rest entire forearms flat along bench or thighs with wrists extending just past edge.",
            steps = listOf(
                "Hold weight palm-up and let it roll slightly into fingertips at bottom.",
                "Curl fingers and wrist upward smoothly toward ceiling.",
                "Pause for 1 second at top flexion, lower slowly."
            ),
            defaultSets = 3,
            defaultReps = "15–20 reps",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Lifting forearms off bench or loading too heavy."
            ),
            safetyInstructions = listOf(
                "Use high reps and light–moderate load to protect delicate wrist tendons."
            ),
            caloriesPerSet = 5
        ),
        ExerciseItem(
            id = "fa_farmers_walk",
            name = "Heavy Farmer's Walk",
            muscleGroup = MuscleGroup.FOREARMS,
            primaryMuscles = listOf("Forearm Flexors", "Crush Grip", "Upper Traps"),
            secondaryMuscles = listOf("Core", "Calves", "Obliques"),
            equipment = "Heavy Dumbbells or Farmer's Handles",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.FARMERS_WALK,
            postureGuide = "Chest proud, shoulders packed down and back, braced core, eyes straight ahead.",
            steps = listOf(
                "Deadlift weights cleanly using legs, not lower back.",
                "Take crisp, controlled short strides along a 20–30 m lane without swaying sideways.",
                "Set weights down cleanly via hip hinge."
            ),
            defaultSets = 3,
            defaultReps = "30 meters",
            defaultRestSec = 75,
            commonMistakes = listOf(
                "Walking while hunched forward or letting weights bounce against thighs."
            ),
            safetyInstructions = listOf(
                "Keep path clear of obstacles and brace core tightly throughout."
            ),
            caloriesPerSet = 11
        ),
        ExerciseItem(
            id = "fa_reverse_curl",
            name = "EZ-Bar Reverse Curl",
            muscleGroup = MuscleGroup.FOREARMS,
            primaryMuscles = listOf("Brachioradialis", "Forearm Extensors"),
            secondaryMuscles = listOf("Biceps Brachii"),
            equipment = "EZ-Bar or Straight Barbell",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.REVERSE_CURL,
            postureGuide = "Overhand (pronated) grip, elbows pinned tight to sides, straight wrists.",
            steps = listOf(
                "Grip bar with palms facing downward.",
                "Curl bar smoothly to shoulder height keeping knuckles facing forward/up.",
                "Lower slowly for 3 seconds."
            ),
            defaultSets = 3,
            defaultReps = "12–15 reps",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Bending wrists downward ('broken wrist') during ascent."
            ),
            safetyInstructions = listOf(
                "Select a lighter weight than standard barbell curls to avoid lateral elbow tendonitis."
            ),
            caloriesPerSet = 7
        ),

        // ======================== CALVES (3) ========================
        ExerciseItem(
            id = "calves_standing",
            name = "Standing Calf Raise",
            muscleGroup = MuscleGroup.CALVES,
            primaryMuscles = listOf("Gastrocnemius (Medial & Lateral)"),
            secondaryMuscles = listOf("Soleus", "Achilles Tendon"),
            equipment = "Calf Raise Machine or Step & Dumbbells",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.STANDING_CALF_RAISE,
            postureGuide = "Keep knees straight (not hyperextended), balls of feet squarely on platform edge.",
            steps = listOf(
                "Lower heels gently into a deep stretch and pause 2 seconds to remove Achilles bounce.",
                "Drive forcefully through big toe and ball of foot to maximum height.",
                "Hold peak contraction 1–2 seconds before descending slowly."
            ),
            defaultSets = 4,
            defaultReps = "12–16 reps",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Bouncing rapidly like a pogo stick using tendon elasticity instead of muscle."
            ),
            safetyInstructions = listOf(
                "Use the 2-second bottom pause on every rep to prevent Achilles strain."
            ),
            caloriesPerSet = 6
        ),
        ExerciseItem(
            id = "calves_seated",
            name = "Seated Calf Raise",
            muscleGroup = MuscleGroup.CALVES,
            primaryMuscles = listOf("Soleus"),
            secondaryMuscles = listOf("Gastrocnemius"),
            equipment = "Seated Calf Raise Machine",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.SEATED_CALF_RAISE,
            postureGuide = "Thighs secured beneath pad at 90° knee bend, spine upright.",
            steps = listOf(
                "Lower heels smoothly into deep stretch for 2 seconds.",
                "Press balls of feet upward to maximum plantar flexion and squeeze.",
                "Control downward motion smoothly."
            ),
            defaultSets = 3,
            defaultReps = "15–20 reps",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Half-reps without reaching full bottom dorsiflexion."
            ),
            safetyInstructions = listOf(
                "Ensure safety stop latch is engaged before removing feet."
            ),
            caloriesPerSet = 5
        ),
        ExerciseItem(
            id = "calves_jump_rope_burn",
            name = "Single-Leg Calf Hop / Rope Pulse",
            muscleGroup = MuscleGroup.CALVES,
            primaryMuscles = listOf("Gastrocnemius", "Soleus"),
            secondaryMuscles = listOf("Foot Intrinsics", "Ankle Stabilizers"),
            equipment = "Jump Rope or Flat Surface",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.JUMP_ROPE,
            postureGuide = "Stay light on balls of feet with ankles springy and posture upright.",
            steps = listOf(
                "Rebound rhythmically 2–3 cm off floor using ankle flexion.",
                "Keep knees soft and elbows tucked near waist.",
                "Alternate left/right or perform double-leg pulses for 45 seconds."
            ),
            defaultSets = 3,
            defaultReps = "45 sec",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Landing heavily on flat heels."
            ),
            safetyInstructions = listOf(
                "Perform on rubber gym flooring or mat to cushion joint impact."
            ),
            caloriesPerSet = 11
        ),

        // ======================== CARDIO (4) ========================
        ExerciseItem(
            id = "cardio_treadmill",
            name = "HIIT Treadmill Sprints",
            muscleGroup = MuscleGroup.CARDIO,
            primaryMuscles = listOf("Cardiorespiratory System", "Quads & Glutes"),
            secondaryMuscles = listOf("Hamstrings", "Calves", "Core"),
            equipment = "Motorized Treadmill",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.TREADMILL_RUN,
            postureGuide = "Run upright near center of belt, arms swinging 90° forward/back, land mid-foot beneath center of mass.",
            steps = listOf(
                "Warm up 5 minutes at brisk walk/jog, attach safety clip.",
                "Sprint 30 seconds at 85% effort, then recover 60 seconds at slow walk.",
                "Repeat for 6–8 rounds followed by 3-minute cool-down."
            ),
            defaultSets = 6,
            defaultReps = "30s sprint / 60s walk",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Holding onto handrails while running.",
                "Overstriding with heavy heel strikes."
            ),
            safetyInstructions = listOf(
                "Always clip emergency safety lanyard before starting sprints."
            ),
            caloriesPerSet = 22
        ),
        ExerciseItem(
            id = "cardio_cycling",
            name = "Assault / Spin Bike Intervals",
            muscleGroup = MuscleGroup.CARDIO,
            primaryMuscles = listOf("VO2 Max & Aerobic Engine", "Quadriceps"),
            secondaryMuscles = listOf("Hamstrings", "Calves"),
            equipment = "Stationary Spin or Air Bike",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.CYCLING_SPRINT,
            postureGuide = "Adjust seat height so knee has a gentle 25°–30° bend at bottom of pedal stroke; keep spine neutral.",
            steps = listOf(
                "Pedal 3 minutes at moderate cadence to lubricate knees.",
                "Increase resistance and sprint 20–40 seconds at high RPM.",
                "Drop resistance and spin easily for recovery interval."
            ),
            defaultSets = 6,
            defaultReps = "30s hard / 45s easy",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Setting seat too low causing front-knee pain."
            ),
            safetyInstructions = listOf(
                "Secure foot straps snugly before standing or sprinting."
            ),
            caloriesPerSet = 20
        ),
        ExerciseItem(
            id = "cardio_rowing",
            name = "Concept2 Rowing Ergometer",
            muscleGroup = MuscleGroup.CARDIO,
            primaryMuscles = listOf("Full Posterior Chain", "Cardio Endurance"),
            secondaryMuscles = listOf("Quadriceps", "Lats", "Biceps", "Core"),
            equipment = "Rowing Machine",
            difficulty = DifficultyLevel.INTERMEDIATE,
            modelType = Exercise3DModelType.ROWING_MACHINE,
            postureGuide = "Order of drive: 60% Legs, 20% Hip Hinge, 20% Arm Pull; reverse smoothly (Arms -> Hinge -> Legs).",
            steps = listOf(
                "Strap feet securely, sit tall, arms extended forward at catch.",
                "Push powerfully with legs first, lean slightly back to 11 o'clock, and pull handle to lower ribs.",
                "Extend arms first, hinge forward, then bend knees cleanly back to catch."
            ),
            defaultSets = 4,
            defaultReps = "500m / 2 min",
            defaultRestSec = 60,
            commonMistakes = listOf(
                "Pulling with arms too early before legs finish driving.",
                "Rounding lower back at the catch."
            ),
            safetyInstructions = listOf(
                "Keep damper setting around 4–6 for optimal athletic stroke mechanics."
            ),
            caloriesPerSet = 24
        ),
        ExerciseItem(
            id = "cardio_jump_rope",
            name = "Conditioning Jump Rope",
            muscleGroup = MuscleGroup.CARDIO,
            primaryMuscles = listOf("Cardiovascular Agility", "Calves"),
            secondaryMuscles = listOf("Shoulders", "Forearms", "Core"),
            equipment = "Speed Jump Rope",
            difficulty = DifficultyLevel.BEGINNER,
            modelType = Exercise3DModelType.JUMP_ROPE,
            postureGuide = "Elbows tucked close to ribs, wrists drive rope rotation, minimal 2–3 cm hop.",
            steps = listOf(
                "Size rope so handles reach mid-chest when standing on center.",
                "Flick wrists cleanly and jump softly on balls of feet.",
                "Perform continuous intervals keeping breathing rhythmic."
            ),
            defaultSets = 4,
            defaultReps = "60 sec",
            defaultRestSec = 45,
            commonMistakes = listOf(
                "Jumping too high and tucking knees backward.",
                "Swinging whole shoulders instead of wrists."
            ),
            safetyInstructions = listOf(
                "Wear supportive athletic shoes and use a shock-absorbing mat."
            ),
            caloriesPerSet = 16
        )
    )

    fun byMuscleGroup(group: MuscleGroup): List<ExerciseItem> =
        allExercises.filter { it.muscleGroup == group }

    fun findById(id: String): ExerciseItem? =
        allExercises.firstOrNull { it.id == id }
}
