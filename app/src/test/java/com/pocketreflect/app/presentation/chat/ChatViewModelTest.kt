// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.chat

import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.chat.ChatContextPolicy
import com.pocketreflect.app.domain.chat.ChatRole
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.testing.FakeAiEngineStatusSource
import com.pocketreflect.app.testing.FakeChatRepository
import com.pocketreflect.app.testing.FakeGemmaLocalEngine
import com.pocketreflect.app.testing.FakeJournalRepository
import com.pocketreflect.app.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val chatRepository = FakeChatRepository()
    private val journalRepository = FakeJournalRepository()
    private val prefs = FakeUserPreferencesRepository()
    private val engine = FakeGemmaLocalEngine()
    private val aiEngineStatus = FakeAiEngineStatusSource()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        runBlocking { prefs.setChatDisclaimerAccepted(true) }
    }

    @Test
    fun sendMessage_appendsUserAndAssistant() = runTest(dispatcher) {
        engine.chatResponse = "Ответ ментора"
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatContract.Intent.UpdateInput("Привет"))
        vm.onIntent(ChatContract.Intent.SendMessage)
        advanceUntilIdle()
        val messages = chatRepository.observeMessages().first()
        assertEquals(2, messages.size)
        assertEquals("Привет", messages[0].content)
        assertEquals("Ответ ментора", messages[1].content)
    }

    @Test
    fun contextFull_blocksSend() = runTest(dispatcher) {
        val huge = "z".repeat(ChatContextPolicy.MAX_CHAT_CONTEXT_CHARS)
        chatRepository.insert(
            ChatMessage(role = ChatRole.USER, content = huge, timestamp = 1L),
        )
        val vm = createViewModel()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state.isContextFull)
        assertFalse(state.canSend)
    }

    private fun createViewModel(): ChatViewModel = ChatViewModel(
        appContext = ApplicationProvider.getApplicationContext(),
        chatRepository = chatRepository,
        journalRepository = journalRepository,
        userPreferencesRepository = prefs,
        gemmaEngine = engine,
        clock = object : Clock {
            override fun nowMillis(): Long = 1000L
            override fun today(): String = "2026-05-20"
        },
        aiEngineStatusSource = aiEngineStatus,
        appLanguageResolver = AppLanguageResolver(prefs),
    )

}
