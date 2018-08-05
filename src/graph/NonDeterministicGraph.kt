package graph

import kotlin.coroutines.experimental.buildSequence

data class NonDeterministicGraph<VERTEX, EDGE>(val graph: Graph<VERTEX, Pair<EDGE, VERTEX>>) : Graph<VERTEX, EDGE> {
  override fun paths(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean,
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX>,
          orderEdges: (Set<EDGE>) -> Sequence<EDGE>
  ): Sequence<Path<VERTEX, EDGE>> {
    return graph.paths(
            starts,
            { path -> filter(toPath(path)) },
            orderVertices,
            { pairs -> orderEdgePairs(pairs, orderVertices, orderEdges) }
    ).map { path -> toPath(path) }
  }

  override fun paths(
          filter: (Path<VERTEX, EDGE>) -> Boolean,
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX>,
          orderEdges: (Set<EDGE>) -> Sequence<EDGE>
  ): Sequence<Path<VERTEX, EDGE>> {
    return graph.paths(
            { path -> filter(toPath(path)) },
            orderVertices,
            { pairs -> orderEdgePairs(pairs, orderVertices, orderEdges) }
    ).map { path -> toPath(path) }
  }

  override fun breadthFirstSearch(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean,
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX>,
          orderEdges: (Set<EDGE>) -> Sequence<EDGE>
  ): Sequence<Path<VERTEX, EDGE>> {
    return graph.breadthFirstSearch(
            starts,
            { path -> filter(toPath(path)) },
            orderVertices,
            { pairs -> orderEdgePairs(pairs, orderVertices, orderEdges) }
    ).map { path -> toPath(path) }
  }

  override fun depthFirstSearch(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean,
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX>,
          orderEdges: (Set<EDGE>) -> Sequence<EDGE>
  ): Sequence<Path<VERTEX, EDGE>> {
    return graph.depthFirstSearch(
            starts,
            { path -> filter(toPath(path)) },
            orderVertices,
            { pairs -> orderEdgePairs(pairs, orderVertices, orderEdges) }
    ).map { path -> toPath(path) }
  }

  override fun closure(starts: Set<VERTEX>, filter: (Path<VERTEX, EDGE>) -> Boolean): Set<VERTEX> {
    return graph.closure(starts, { path -> filter(toPath(path)) })
  }

  fun epsilonClosure(starts: Set<VERTEX>) = closure(starts, { p -> p.edge == null })

  private fun <VERTEX, EDGE> toPath(p: Path<VERTEX, Pair<EDGE, VERTEX>>): Path<VERTEX, EDGE> {
    return Path(p.start, p.edge.first, p.end)
  }

  private fun <EDGE> orderEdgePairs(
          pairs: Set<Pair<EDGE, VERTEX>>,
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX>,
          orderEdges: (Set<EDGE>) -> Sequence<EDGE>
  ) = buildSequence {
    val pairsMap = mutableMapOf<EDGE, Set<VERTEX>>()
    for ((edge, end) in pairs)
      pairsMap[edge] = pairsMap[edge].orEmpty() union setOf(end)
    for (edge in orderEdges(pairsMap.keys)) {
      for (end in orderVertices(pairsMap[edge]!!)) {
        yield(Pair(edge, end))
      }
    }
  }

  class New<VERTEX, EDGE> {
    val graph = SimpleGraph.New<VERTEX, Pair<EDGE, VERTEX>>()

    operator fun set(origin: VERTEX, destinations: Map<Pair<EDGE, VERTEX>, VERTEX>) = apply {
      graph[origin] = destinations
    }

    operator fun get(vertex: VERTEX): Map<Pair<EDGE, VERTEX>, VERTEX>? {
      return graph[vertex]
    }

    fun path(start: VERTEX, edge: EDGE, end: VERTEX) = apply {
      path(Path(start, edge, end))
    }

    fun path(path: Path<VERTEX, EDGE>) = apply {
      // TODO: think of a clearer syntax for this
      // Just like `object.field.subfield = value` is possible, this should also be possible:
      //   `vertices[start][edge] = vertices[start][edge] or {} union {end}
      val edges = graph[path.start].orEmpty().toMutableMap()
      edges[Pair(path.edge, path.end)] = path.end
      graph[path.start] = edges
      graph[path.end] = graph[path.end].orEmpty()
    }

    fun build() = NonDeterministicGraph(graph.build())
  }
}

