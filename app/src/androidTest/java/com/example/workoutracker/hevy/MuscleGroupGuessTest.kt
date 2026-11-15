package com.example.workoutracker.hevy

import com.example.workoutracker.data.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MuscleGroupGuessTest {

    @Test
    fun yourOwnExercisesUseYourList() {
        assertEquals(MuscleGroup.U_BACK, guessMuscleGroup("kelso"))
        assertEquals(MuscleGroup.TRICEPS, guessMuscleGroup("diddy extensions"))
        assertEquals(MuscleGroup.LATS, guessMuscleGroup("Chest Supported Incline Row (Dumbbell)"))
        assertEquals(MuscleGroup.HAMSTRINGS, guessMuscleGroup("Back Extension (Weighted Hyperextension)"))
    }

    @Test
    fun ignoresCaseAndSpaces() {
        assertEquals(MuscleGroup.CHEST, guessMuscleGroup("  BENCH PRESS (BARBELL) "))
    }

    @Test
    fun specificRulesBeatGeneralOnes() {
        assertEquals(MuscleGroup.HAMSTRINGS, guessMuscleGroup("Nordic Leg Curl"))       // not Biceps
        assertEquals(MuscleGroup.FOREARMS, guessMuscleGroup("Reverse Wrist Curl"))     // not Biceps
        assertEquals(MuscleGroup.SHOULDERS, guessMuscleGroup("Reverse Fly (Dumbbell)")) // not Chest
        assertEquals(MuscleGroup.U_BACK, guessMuscleGroup("Chest Supported T-Bar Row")) // not Chest
        assertEquals(MuscleGroup.TRICEPS, guessMuscleGroup("Overhead Triceps Extension (Dumbbell)"))
        assertEquals(MuscleGroup.ABS, guessMuscleGroup("Lying Leg Raise"))              // not Quads
    }

    @Test
    fun newExercisesGetAReasonableGuess() {
        assertEquals(MuscleGroup.BICEPS, guessMuscleGroup("Spider Curl"))
        assertEquals(MuscleGroup.CHEST, guessMuscleGroup("Decline Bench Press"))
        assertEquals(MuscleGroup.QUADS, guessMuscleGroup("Bulgarian Split Squat"))
        assertEquals(MuscleGroup.CALVES, guessMuscleGroup("Standing Calf Raise"))
        assertEquals(MuscleGroup.LATS, guessMuscleGroup("Chin Up"))
        assertEquals(MuscleGroup.ADDUCTORS, guessMuscleGroup("Cable Hip Adduction"))
    }

    @Test
    fun unknownStaysBlank() {
        assertNull(guessMuscleGroup("Farmer's Walk"))
        assertNull(guessMuscleGroup("Deadlift (Barbell)")) // works several groups: left for you to pick
    }
}