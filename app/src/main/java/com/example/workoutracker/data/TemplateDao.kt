package com.example.workoutracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Insert
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Insert
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    /** Its exercise list goes too (cascade). Returns rows deleted. */
    @Query("DELETE FROM template WHERE id = :id")
    suspend fun deleteTemplate(id: Long): Int

    @Query("UPDATE template SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    @Transaction
    @Query("SELECT * FROM template ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM template WHERE id = :id")
    suspend fun getTemplate(id: Long): TemplateWithExercises?
}