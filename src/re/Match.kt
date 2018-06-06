package re

data class Match(
        val start: Int,
        val end: Int,
        private val groups: Map<String, Group> = mapOf()) {

  operator fun get(name: String): Group? {
    return groups[name]
  }
}
