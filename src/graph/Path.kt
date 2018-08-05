package graph

data class Path<VERTEX, EDGE>(
        val start: VERTEX,
        val edge: EDGE,
        val end: VERTEX
)
