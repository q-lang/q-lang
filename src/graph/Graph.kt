package graph

fun <T> defaultOrder(elements: Set<T>): Sequence<T> {
  return elements.asSequence()
}

interface Graph<VERTEX, EDGE> {
  fun paths(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean = { _ -> true },
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX> = { defaultOrder(it) },
          orderEdges: (Set<EDGE>) -> Sequence<EDGE> = { defaultOrder(it) }
  ): Sequence<Path<VERTEX, EDGE>>

  fun paths(
          filter: (Path<VERTEX, EDGE>) -> Boolean = { _ -> true },
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX> = { defaultOrder(it) },
          orderEdges: (Set<EDGE>) -> Sequence<EDGE> = { defaultOrder(it) }
  ): Sequence<Path<VERTEX, EDGE>>

  fun breadthFirstSearch(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean = { _ -> true },
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX> = { defaultOrder(it) },
          orderEdges: (Set<EDGE>) -> Sequence<EDGE> = { defaultOrder(it) }
  ): Sequence<Path<VERTEX, EDGE>>

  fun depthFirstSearch(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean = { _ -> true },
          orderVertices: (Set<VERTEX>) -> Sequence<VERTEX> = { defaultOrder(it) },
          orderEdges: (Set<EDGE>) -> Sequence<EDGE> = { defaultOrder(it) }
  ): Sequence<Path<VERTEX, EDGE>>

  fun closure(
          starts: Set<VERTEX>,
          filter: (Path<VERTEX, EDGE>) -> Boolean = { _ -> true }
  ): Set<VERTEX>
}
