package org.example.project.feature.notes

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import org.example.project.core.error.CallFailure
import org.example.project.feature.notes.data.NotesRepositoryImpl
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.buildSeamJson
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NotesQuotaExceeded
import org.example.project.shared.notes.notesErrorSerializersModule

/**
 * The client side of the notes domain, against a `MockEngine` server: it exercises the real
 * request-building (shared `@Resource`), the `toModel()` mapping, and — crucially for Phase 5 — the
 * seam `Json` built with [notesErrorSerializersModule], so a 4xx [NotesQuotaExceeded] parses back
 * into the *typed* error rather than degrading to `UnknownApiError`. That proves the per-domain
 * `SerializersModule` multibinding composes on the client end too.
 */
class NotesRepositoryNetworkTest {
    private val json = buildSeamJson(setOf(notesErrorSerializersModule))
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `list maps the wire notes to the client model`() = runTest {
        val repo = repo {
            respond(
                json.encodeToString(
                    NoteListResponse(
                        listOf(
                            NoteResponse(
                                id = "id-1",
                                text = "Milk, eggs",
                                authorEmail = "alice@example.com",
                                createdAt = Clock.System.now(),
                            )
                        )
                    )
                ),
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = repo.list()

        assertThat(result.leftOrNull()).isNull()
        assertThat(result.getOrNull().orEmpty())
            .containsExactly(Note(id = "id-1", text = "Milk, eggs", author = "alice@example.com"))
    }

    @Test
    fun `exceeding the quota surfaces the typed NotesQuotaExceeded`() = runTest {
        val repo = repo {
            respond(
                json.encodeToString(
                    ErrorEnvelope(NotesQuotaExceeded(quota = 20_000, used = 18_000))
                ),
                HttpStatusCode.BadRequest,
                jsonHeaders,
            )
        }

        val result = repo.create("one more note")

        assertThat(result.leftOrNull())
            .isEqualTo(CallFailure.Declared(NotesQuotaExceeded(quota = 20_000, used = 18_000)))
    }

    private fun repo(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): NotesRepositoryImpl =
        NotesRepositoryImpl(
            HttpClient(MockEngine(handler)) {
                install(ContentNegotiation) { json(json) }
                install(Resources)
            }
        )
}
