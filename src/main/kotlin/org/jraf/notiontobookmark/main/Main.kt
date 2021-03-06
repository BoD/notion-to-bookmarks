/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2020-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:Suppress("EXPERIMENTAL_API_USAGE")

package org.jraf.notiontobookmark.main

import com.petersamokhin.notionapi.Notion
import com.petersamokhin.notionapi.model.NotionResponse
import com.petersamokhin.notionapi.utils.dashifyId
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val DEFAULT_PORT = 8042

private const val ENV_PORT = "PORT"

private const val PATH_COOKIE = "cookie"
private const val PATH_PAGE_ID = "pageId"

private const val APP_URL = "https://notion-to-bookmarks.herokuapp.com"

private const val MAX_ALLOWED_SIZE = 100

private const val NB_RETRIES = 5


private val LOGGER = LoggerFactory.getLogger("org.jraf.notiontobookmark.main")

suspend fun main() {
    val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
    embeddedServer(Netty, listenPort) {
        install(DefaultHeaders)

        install(StatusPages) {
            status(HttpStatusCode.NotFound) {
                call.respondText(
                    text = "Usage: $APP_URL/<Notion cookie>/<page id>\n\nSee https://github.com/BoD/notion-to-bookmarks for more info.",
                    status = it
                )
            }

            exception<StackOverflowError> {
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    "Requested page had too many sub pages, choose a less deep one"
                )
            }
        }

        routing {
            get("{$PATH_COOKIE}/{$PATH_PAGE_ID}") {
                val cookie = call.parameters[PATH_COOKIE]!!
                val pageId = call.parameters[PATH_PAGE_ID]!!
                val notion = Notion(cookie)
                val jsonBookmarks = getAllSubPages(notion, pageId).sorted().asJsonBookmarks()
                val jsonBookmarksWithEnvelope = """{"version": 1, ${jsonBookmarks}}"""
                call.respondText(jsonBookmarksWithEnvelope, ContentType.Application.Json.withCharset(Charsets.UTF_8))
            }
        }
    }.start(wait = true)
}

data class Page(
    val id: String,
    val title: String,
    val pages: List<Page>
)

private suspend fun getAllSubPages(notion: Notion, pageId: String, count: Int = 0): List<Page> {
    var count = count
    val res = mutableListOf<Page>()
    val dashPageId = pageId.dashifyId()
    val page = loadNotionPage(notion, dashPageId) ?: return emptyList()
    val blocks = page.recordMap.blocksMap.values
        .map { it.value }
        .filter { it.type == "page" }
    val jobs = mutableListOf<Deferred<Unit>>()
    for (block in blocks) {
        jobs += GlobalScope.async {
            count++
            if (count > MAX_ALLOWED_SIZE) throw StackOverflowError()

            // Ignore parents, but keep this page itself
            val isSelf = block.id == dashPageId
            if (block.parentId != dashPageId && !isSelf) return@async
            res += Page(
                id = block.id,
                title = block.properties?.get("title")?.get(0)?.get(0) as? String ?: "(Untitled)",
                pages = if (isSelf) emptyList() else getAllSubPages(notion, block.id, count)
            )
        }
    }
    for (job in jobs) job.await()
    return res
}

private suspend fun loadNotionPage(notion: Notion, dashPageId: String): NotionResponse? {
    var retries = NB_RETRIES
    var page: NotionResponse?
    do {
        val retry = NB_RETRIES - retries
        delay(TimeUnit.SECONDS.toMillis(retry.toLong()))
        LOGGER.debug("Load page $dashPageId${if (retry > 0) " Retry $retry" else ""}")
        page = try {
            notion.loadPage(dashPageId)
        } catch (e: Exception) {
            LOGGER.debug("Load page didn't work", e)
            null
        }
        retries--
    } while (page?.recordMap?.blocksMap == null && retries >= 0)
    if (page?.recordMap?.blocksMap == null) return null
    return page
}

private fun List<Page>.sorted(): List<Page> {
    return sortedBy { page ->
        if (page.pages.isEmpty() || page.pages.size == 1) {
            "000" + page.title.toLowerCase(Locale.ROOT)
        } else {
            page.title.toLowerCase(Locale.ROOT)
        }
    }
}

private fun List<Page>.asJsonBookmarks(): String {
    var res = """
        "bookmarks": [
    """.trimIndent()
    for ((i, page) in this.withIndex()) {
        res += if (page.pages.isEmpty() || page.pages.size == 1) {
            """
                    {
                        "title": ${JSONObject.quote(page.title)},
                        "url": "https://notion.so/${page.id.removeDashes()}"
                    }${if (i == this.lastIndex) "" else ","}
                """.trimIndent()
        } else {
            """
                    {
                        "title": ${JSONObject.quote(page.title)},
                        ${page.pages.sorted().asJsonBookmarks()}
                    }${if (i == this.lastIndex) "" else ","}
                """.trimIndent()
        }
    }
    res += "]"
    return res
}

private fun String.removeDashes() = filterNot { it == '-' }
