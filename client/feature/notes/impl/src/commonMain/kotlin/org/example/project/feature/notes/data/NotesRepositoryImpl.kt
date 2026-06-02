package org.example.project.feature.notes.data

import arrow.core.Either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import org.example.project.core.error.AppError
import org.example.project.core.network.call
import org.example.project.feature.notes.Note
import org.example.project.feature.notes.NotesRepository
import org.example.project.shared.notes.CreateNoteRequest
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NotesApi
import org.example.project.shared.notes.NotesResource

@Inject
@ContributesBinding(AppScope::class)
class NotesRepositoryImpl(private val client: HttpClient) : NotesRepository {
    override suspend fun list(): Either<AppError, List<Note>> =
        client.call(NotesApi.list, NotesResource()).map { response ->
            response.notes.map { it.toModel() }
        }

    override suspend fun create(text: String): Either<AppError, Unit> =
        client.call(NotesApi.create, NotesResource(), CreateNoteRequest(text)).map {}

    override suspend fun delete(id: String): Either<AppError, Unit> =
        client.call(NotesApi.delete, NotesResource.ById(id = id))

    /** Share the wire, not the domain: map the server's [NoteResponse] to the client [Note]. */
    private fun NoteResponse.toModel(): Note = Note(id = id, text = text, author = authorEmail)
}
