package com.example.meaningosapp.ui.main.face.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.meaningosapp.ui.theme.BabyNodeTheme

import com.example.meaningosapp.ui.main.face.viewmodel.BabyNodeViewModel
import com.example.meaningosapp.ui.main.face.viewmodel.BabyNodeViewModelFactory
import com.example.meaningosapp.ui.main.face.ui.BabyNodeScreen

import com.example.meaningosapp.core.ptp.PTPRoot
import com.example.meaningosapp.core.ptp.PTPGraph
import com.example.meaningosapp.core.ptp.PTPGraphBuilder
import kotlinx.serialization.json.Json

// ⭐ REQUIRED FIX — this import was missing
import androidx.compose.runtime.collectAsState

class BabyNodeActivity : ComponentActivity() {

    private val viewModel: BabyNodeViewModel by viewModels {
        BabyNodeViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 — Load raw JSON
        val ptpRaw = loadPTP()
        viewModel.setDebug("PTP Loaded: ${ptpRaw.length} chars")

        // STEP 2 — Parse JSON into PTPRoot
        val ptpRoot = parsePTP(ptpRaw)
        viewModel.setDebug("PTP Parsed: ${ptpRoot.layers.size} layers")

        // STEP 3 — Build semantic graph
        val ptpGraph: PTPGraph = PTPGraphBuilder.build(ptpRoot)
        viewModel.setDebug("Graph Built: ${ptpGraph.nodesByName.size} nodes")

        // STEP 4 — Store graph in ViewModel
        viewModel.setGraph(ptpGraph)

        // STEP 5 — Example node
        val example = ptpGraph.getNode("init.identity")
        viewModel.setDebug("Example Node: $example")

        setContent {
            BabyNodeTheme {

                BabyNodeScreen(
                    mode = viewModel.mode.collectAsState().value,
                    partialText = viewModel.partialText.collectAsState().value,
                    finalText = viewModel.finalText.collectAsState().value,
                    reply = viewModel.reply.collectAsState().value,
                    waveform = viewModel.waveform.collectAsState().value,
                    onStartListening = { viewModel.startListening() },
                    onStopListening = { viewModel.stopListening() }
                )
            }
        }
    }

    private fun loadPTP(): String {
        return assets.open("PTP.json")
            .bufferedReader()
            .use { it.readText() }
    }

    private fun parsePTP(json: String): PTPRoot {
        return Json { ignoreUnknownKeys = true }
            .decodeFromString(PTPRoot.serializer(), json)
    }
}
