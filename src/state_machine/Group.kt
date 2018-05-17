package state_machine

data class Group<TAG>(val name: TAG, val start: Int, var end: Int)
