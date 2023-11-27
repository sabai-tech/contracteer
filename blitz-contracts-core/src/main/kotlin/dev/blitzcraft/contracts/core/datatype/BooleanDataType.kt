package dev.blitzcraft.contracts.core.datatype

import net.datafaker.Faker

class BooleanDataType: DataType<Boolean> {
  override fun nextValue(): Boolean = Faker().random().nextBoolean()

  override fun regexPattern(): String = "(true|false)"
}