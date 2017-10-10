package nl.biopet.tools.vcffilter

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

object VcfFilterTest extends BiopetTest {
  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      ToolTemplate.main(Array())
    }
  }
}
