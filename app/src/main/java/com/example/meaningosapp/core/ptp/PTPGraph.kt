package com.example.meaningosapp.core.ptp

/**
 * A single primitive in the semantic graph.
 * This enriches the raw PTP model with layer metadata.
 */
data class PTPGraphNode(
    val name: String,
    val definition: String,
    val layerIndex: Int,
    val layerName: String,
    val dependencies: List<String>
)

/**
 * The full semantic graph.
 * nodesByName lets Baby Node instantly look up any primitive.
 */
data class PTPGraph(
    val nodesByName: Map<String, PTPGraphNode>
) {
    fun getNode(name: String): PTPGraphNode? = nodesByName[name]
}

/**
 * Builds a PTPGraph from the parsed PTPRoot model.
 * This is the bridge between PTP.json and Baby Node’s meaning engine.
 */
object PTPGraphBuilder {

    fun build(root: PTPRoot): PTPGraph {
        val map = mutableMapOf<String, PTPGraphNode>()

        for (layer in root.layers) {
            for (primitive in layer.primitives) {
                val node = PTPGraphNode(
                    name = primitive.name,
                    definition = primitive.definition,
                    layerIndex = layer.layer,
                    layerName = layer.name,
                    dependencies = primitive.dependencies
                )
                map[primitive.name] = node
            }
        }

        return PTPGraph(nodesByName = map)
    }
}
