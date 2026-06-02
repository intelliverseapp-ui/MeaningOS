package com.example.meaningosapp.ui.main.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

// ⭐ Correct BabyNodeMode import
import com.example.meaningosapp.ui.main.face.ui.BabyNodeMode

import com.example.meaningosapp.ui.model.ChatMessage

@Composable
fun ChatHistory(
    messages: List<ChatMessage>,
    mode: BabyNodeMode,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState
    ) {
        itemsIndexed(messages, key = { index, _ -> index }) { _, msg ->
            ChatBubble(
                message = msg,
                mode = mode
            )
        }
    }
}
