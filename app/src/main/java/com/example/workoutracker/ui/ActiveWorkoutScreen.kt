package com.example.workoutracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workoutracker.data.WorkoutEntity
import com.example.workoutracker.data.WorkoutExerciseWithSets
import com.example.workoutracker.data.WorkoutSetEntity

@Composable
fun ActiveWorkoutScreen(
    onFinished: () -> Unit,
    onDiscarded: () -> Unit,
    onSaved: () -> Unit,
    onAddExercise: (workoutId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val previousSets by viewModel.previousSets.collectAsStateWithLifecycle()
    val current = workout

    if (current == null) {
        // Still loading, or just deleted while we navigate away.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ActiveWorkoutContent(
            workout = current,
            exercises = exercises,
            previousSets = previousSets,
            actions = WorkoutActions(
                addExercise = { onAddExercise(current.id) },
                removeExercise = viewModel::removeExercise,
                moveExercise = viewModel::moveExercise,
                addSet = viewModel::addSet,
                updateSet = viewModel::updateSet,
                setDone = viewModel::setDone,
                deleteSet = viewModel::deleteSet,
                finish = { title, notes -> viewModel.finish(title, notes, onFinished) },
                saveEdits = { title, notes, startedAt -> viewModel.saveEdits(title, notes, startedAt, onSaved) },
                discard = { viewModel.discard(onDiscarded) }
            ),
            modifier = modifier
        )
    }
}

/** Everything the workout screen can ask the ViewModel to do, bundled so the parameter lists stay short. */
private class WorkoutActions(
    val addExercise: () -> Unit,
    val removeExercise: (workoutExerciseId: Long) -> Unit,
    val moveExercise: (workoutExerciseId: Long, up: Boolean) -> Unit,
    val addSet: (workoutExerciseId: Long) -> Unit,
    val updateSet: (setId: Long, weightKg: Double?, reps: Int?) -> Unit,
    val setDone: (setId: Long, isDone: Boolean) -> Unit,
    val deleteSet: (setId: Long) -> Unit,
    val finish: (title: String, notes: String) -> Unit,
    val saveEdits: (title: String, notes: String, startedAt: Long) -> Unit,
    val discard: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveWorkoutContent(
    workout: WorkoutEntity,
    exercises: List<WorkoutExerciseWithSets>,
    previousSets: Map<Long, List<WorkoutSetEntity>>,
    actions: WorkoutActions,
    modifier: Modifier = Modifier
) {
    // Local state, seeded once from the database. Writing every keystroke to the database and
    // reading it straight back makes the cursor jump, so we save when the user taps Finish.
    // rememberSaveable keeps the text across rotation and process death.
    var title by rememberSaveable { mutableStateOf(workout.title) }
    var notes by rememberSaveable { mutableStateOf(workout.notes ?: "") }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    // The id of an exercise waiting for "remove?" confirmation, or null.
    var pendingRemoval by rememberSaveable { mutableStateOf<Long?>(null) }

    // A workout that already has an end time is being edited from History, not done live.
    // The same screen serves both: edit mode has no timer and no discard, and saves on exit.
    val isEditing = workout.endedAt != null
    // Edit mode only: a new start time chosen with the picker, saved along with the title.
    var editedStartedAt by rememberSaveable { mutableLongStateOf(workout.startedAt) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    fun trySave() {
        // Same rule as finishing: at least one ticked set, otherwise show the warning.
        if (summarizeForFinish(exercises).hasNothingDone) {
            confirmFinish = true
        } else {
            actions.saveEdits(title, notes, editedStartedAt)
        }
    }
    // In edit mode, the system back button saves too, so changes are never half-kept.
    BackHandler(enabled = isEditing) { trySave() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit workout" else "Active workout") },
                actions = {
                    Button(
                        onClick = {
                            if (isEditing) trySave() else { confirmFinish = true }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(if (isEditing) "Save" else "Finish") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding() // keeps the focused box above the keyboard
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val endedAt = workout.endedAt
            if (endedAt != null) {
                // Editing: no running clock. Shows the (possibly changed) start, the unchanged
                // duration, and a button to move the workout to another day or time.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${formatWorkoutDate(editedStartedAt)} \u00B7 ${formatDuration(endedAt - workout.startedAt)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                }
            } else {
                // The timer has its own composable so only it redraws every second,
                // not the whole screen full of text boxes.
                ElapsedText(workout.startedAt)
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Workout title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (exercises.isEmpty()) {
                Text(
                    "No exercises yet. Tap Add exercise to start.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            exercises.forEachIndexed { index, item ->
                // key() ties each block's state (the text in its boxes) to that exercise,
                // so reordering or removing one doesn't shuffle the typed values around.
                key(item.workoutExercise.id) {
                    ExerciseBlock(
                        item = item,
                        previous = previousSets[item.exercise.id].orEmpty(),
                        canMoveUp = index > 0,
                        canMoveDown = index < exercises.lastIndex,
                        actions = actions,
                        onRemove = {
                            // Nothing to lose if it has no sets, so skip the confirmation.
                            if (item.sets.isEmpty()) actions.removeExercise(item.workoutExercise.id)
                            else pendingRemoval = item.workoutExercise.id
                        }
                    )
                }
            }

            FilledTonalButton(onClick = actions.addExercise, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add exercise")
            }

            // Deleting a finished workout lives on its detail screen, so no discard when editing.
            if (!isEditing) {
                OutlinedButton(
                    onClick = { confirmDiscard = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Discard workout") }
            }
        }
    }

    if (confirmFinish) {
        val summary = summarizeForFinish(exercises)
        if (summary.hasNothingDone && isEditing) {
            AlertDialog(
                onDismissRequest = { confirmFinish = false },
                title = { Text("No sets ticked") },
                text = {
                    Text(
                        "A saved workout needs at least one ticked set. " +
                                "To remove this workout instead, delete it from its detail screen."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { confirmFinish = false }) { Text("Keep editing") }
                }
            )
        } else if (summary.hasNothingDone) {
            // Saving an empty workout would just clutter History, so the only choices
            // are to carry on or throw it away.
            AlertDialog(
                onDismissRequest = { confirmFinish = false },
                title = { Text("No sets ticked yet") },
                text = {
                    Text(
                        "Tick at least one set you've done before finishing. " +
                                "Or discard this workout if you started it by mistake."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { confirmFinish = false }) { Text("Keep going") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { confirmFinish = false; confirmDiscard = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Discard") }
                }
            )
        } else {
            val elapsed = rememberElapsedMillis(workout.startedAt)
            val shownTitle = title.trim().ifEmpty { workout.title }
            AlertDialog(
                onDismissRequest = { confirmFinish = false },
                title = { Text("Finish workout?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("\"$shownTitle\" will be saved to your history.")
                        Text("Duration: ${formatElapsed(elapsed)}")
                        Text("Sets done: ${summary.doneSets}")
                        finishNotes(summary).forEach { note ->
                            Text(note, color = MaterialTheme.colorScheme.error)
                        }
                        if (isSuspiciouslyShort(elapsed)) {
                            Text(
                                "This workout is under a minute long. Did you mean to finish it?",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { confirmFinish = false; actions.finish(title, notes) }) { Text("Finish") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmFinish = false }) { Text("Keep going") }
                }
            )
        }
    }

    if (showDatePicker) {
        WorkoutDateTimePicker(
            startedAt = editedStartedAt,
            onPicked = { newStart ->
                editedStartedAt = newStart
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    pendingRemoval?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove exercise?") },
            text = { Text("Its logged sets will be deleted too.") },
            confirmButton = {
                TextButton(onClick = { pendingRemoval = null; actions.removeExercise(id) }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Keep") }
            }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this workout?") },
            text = { Text("Everything logged in it will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; actions.discard() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun ElapsedText(startedAt: Long) {
    val elapsed = rememberElapsedMillis(startedAt)
    Text(formatElapsed(elapsed), style = MaterialTheme.typography.displayMedium)
}

/** One exercise in the active workout: its name, controls, and its sets. */
@Composable
private fun ExerciseBlock(
    item: WorkoutExerciseWithSets,
    previous: List<WorkoutSetEntity>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: WorkoutActions,
    onRemove: () -> Unit
) {
    val weId = item.workoutExercise.id
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                    item.exercise.muscleGroup?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    // Live total: updates as you type, because the sets come from the database flow.
                    val volume = exerciseVolume(item.sets)
                    if (volume > 0) {
                        Text("Volume: ${formatVolume(volume)} kg", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { actions.moveExercise(weId, true) }, enabled = canMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${item.exercise.name} up")
                }
                IconButton(onClick = { actions.moveExercise(weId, false) }, enabled = canMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${item.exercise.name} down")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove ${item.exercise.name}")
                }
            }

            if (item.sets.isNotEmpty()) {
                SetHeaderRow()
                item.sets.forEachIndexed { index, set ->
                    key(set.id) {
                        SetRow(
                            number = index + 1,
                            set = set,
                            // Matched by set number (1st with 1st...), not setIndex, which can have gaps.
                            previous = previous.getOrNull(index),
                            onChange = { weightKg, reps -> actions.updateSet(set.id, weightKg, reps) },
                            onDoneChange = { done -> actions.setDone(set.id, done) },
                            onDelete = { actions.deleteSet(set.id) }
                        )
                    }
                }
            }

            TextButton(onClick = { actions.addSet(weId) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun SetHeaderRow() {
    Row(
        // End padding = tick button width + spacing, so the labels line up with the boxes.
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, end = TICK_BUTTON_SIZE + ROW_SPACING),
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        Text("Set", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(SET_NUMBER_WIDTH))
        Text("Previous", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(
            "kg", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Reps", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SetRow(
    number: Int,
    set: WorkoutSetEntity,
    previous: WorkoutSetEntity?,
    onChange: (weightKg: Double?, reps: Int?) -> Unit,
    onDoneChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // Seeded once from the database, then the boxes own the text (same reason as the title box).
    // Each keystroke is still saved straight away, so nothing is lost if the app is closed.
    var weightText by rememberSaveable { mutableStateOf(formatWeight(set.weightKg)) }
    var repsText by rememberSaveable { mutableStateOf(set.reps?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            // Done sets get a tinted background, so you can see at a glance what you've completed.
            .background(
                if (set.isDone) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        // Tap the set number for its menu (delete lives here now, to leave room for the tick).
        Box {
            Text(
                "$number",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(SET_NUMBER_WIDTH)
                    .clickable { showMenu = true }
                    .padding(vertical = 8.dp)
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete set $number") },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
        Text(
            text = formatPrevious(previous?.weightKg, previous?.reps),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { new ->
                // Typing a character that would make the number invalid just does nothing.
                if (isValidWeightInput(new)) {
                    weightText = new
                    onChange(parseWeight(new), parseReps(repsText))
                }
            },
            singleLine = true,
            textStyle = numberBoxStyle(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { new ->
                if (isValidRepsInput(new)) {
                    repsText = new
                    onChange(parseWeight(weightText), parseReps(new))
                }
            },
            singleLine = true,
            textStyle = numberBoxStyle(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f)
        )
        FilledIconToggleButton(
            checked = set.isDone,
            onCheckedChange = onDoneChange,
            // Needs reps first. Uses the box's text, so it enables the moment you type.
            enabled = set.isDone || canTick(repsText),
            modifier = Modifier.size(TICK_BUTTON_SIZE)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = if (set.isDone) "Untick set $number" else "Tick set $number as done",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Sizes for the set table, in one place so the header and the rows always line up.
private val SET_NUMBER_WIDTH = 24.dp
private val TICK_BUTTON_SIZE = 36.dp
private val ROW_SPACING = 6.dp

/** Centred numbers in the kg and reps boxes. */
@Composable
private fun numberBoxStyle() =
    MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)