package org.example.project.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Room refuses a database with no entities; this holds the slot until a real one exists. */
@Entity(tableName = "placeholder") data class PlaceholderEntity(@PrimaryKey val id: Long)
