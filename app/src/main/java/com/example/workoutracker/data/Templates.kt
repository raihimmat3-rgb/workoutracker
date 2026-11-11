package com.example.workoutracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A saved routine, like "Push Day". Added in database version 3. */
@Entity(tableName = "template")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

/** One exercise in a routine, in order, with how many sets to plan for it. */
@Entity(
    tableName = "template_exercise",
    foreignKeys = [
        // Deleting a routine deletes its exercise list.
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        // A library exercise used in a routine can't be deleted (same rule as workouts).
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val setCount: Int
)

data class TemplateExerciseWithExercise(
    @Embedded val templateExercise: TemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity
)

/** A routine with its exercises. As with workouts, sort the list by position yourself. */
data class TemplateWithExercises(
    @Embedded val template: TemplateEntity,
    @Relation(
        entity = TemplateExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val exercises: List<TemplateExerciseWithExercise>
)