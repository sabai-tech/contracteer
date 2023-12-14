package dev.blitzcraft.contracts.core.datatype

import kotlin.random.Random

class BooleanDataType: DataType<Boolean> {
  override fun nextValue(): Boolean = Random.nextBoolean()

  override fun regexPattern(): String = "(true|false)"
}