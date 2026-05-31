package org.example.project.feature.notes.data

import arrow.core.Either
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.core.error.AppError
import org.example.project.core.network.executeSafe
import org.example.project.feature.notes.Note
import org.example.project.feature.notes.NotesRepository
import org.example.project.shared.notes.CreateNoteRequest
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NotesResource

@Inject
@ContributesBinding(AppScope::class)
class NotesRepositoryImpl(private val client: HttpClient) : NotesRepository {
    override suspend fun list(): Either<AppError, List<Note>> = either {
        val response =
            executeSafe({ client.get(NotesResource()) }) { it.body<NoteListResponse>() }.bind()
        response.notes.map { it.toModel() }
    }

    override suspend fun create(text: String): Either<AppError, Unit> = either {
        executeSafe({
                client.post(NotesResource()) {
                    contentType(ContentType.Application.Json)
                    setBody(CreateNoteRequest(text))
                }
            }) {}
            .bind()
    }

    override suspend fun delete(id: String): Either<AppError, Unit> = either {
        executeSafe({ client.delete(NotesResource.ById(id = id)) }) {}.bind()
    }

    /** Share the wire, not the domain: map the server's [NoteResponse] to the client [Note]. */
    private fun NoteResponse.toModel(): Note = Note(id = id, text = text, author = authorEmail)
}
