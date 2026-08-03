package dev.contracteer.core.datatype

import org.junit.jupiter.api.Test

class CycleGuardTest {

  @Test
  fun `runs body and returns its result when proxy is not yet visited`() {
    // given
    val guard = CycleGuard<Int>()
    val proxy = ProxyDataType("X")

    // when
    val result = guard.visit(proxy, onCycle = { -1 }, body = { 42 })

    // then
    assert(result == 42)
  }

  @Test
  fun `returns onCycle when the same proxy is re-entered`() {
    // given
    val guard = CycleGuard<Int>()
    val proxy = ProxyDataType("X")

    // when
    val result = guard.visit(proxy, onCycle = { -1 }, body = {
      guard.visit(proxy, onCycle = { -99 }, body = { 42 })
    })

    // then
    assert(result == -99)
  }

  @Test
  fun `removes proxy from visited set after body completes so siblings see no cycle`() {
    // given
    val guard = CycleGuard<Int>()
    val proxy = ProxyDataType("X")

    // when
    val first  = guard.visit(proxy, onCycle = { -1 }, body = { 1 })
    val second = guard.visit(proxy, onCycle = { -1 }, body = { 2 })

    // then
    assert(first == 1)
    assert(second == 2)
  }

  @Test
  fun `removes proxy from visited set even when body throws`() {
    // given
    val guard = CycleGuard<Int>()
    val proxy = ProxyDataType("X")

    // when
    val thrown = runCatching {
      guard.visit(proxy, onCycle = { -1 }, body = { throw RuntimeException("boom") })
    }
    val recovered = guard.visit(proxy, onCycle = { -1 }, body = { 99 })

    // then
    assert(thrown.isFailure)
    assert(recovered == 99)
  }

  @Test
  fun `uses identity equality so distinct proxies sharing a name are not a cycle`() {
    // given
    val guard = CycleGuard<String>()
    val a = ProxyDataType("Same")
    val b = ProxyDataType("Same")

    // when
    val result = guard.visit(a, onCycle = { "cycle" }, body = {
      guard.visit(b, onCycle = { "cycle" }, body = { "body" })
    })

    // then
    assert(result == "body")
  }
}
