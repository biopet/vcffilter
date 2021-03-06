/*
 * Copyright (c) 2014 Biopet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.biopet.tools.vcffilter

import java.io.File

import htsjdk.variant.variantcontext.GenotypeType
import htsjdk.variant.vcf.VCFFileReader
import nl.biopet.utils.test.tools.ToolTest
import nl.biopet.tools.vcffilter.Args.Trio
import org.testng.annotations.Test

import scala.collection.JavaConversions._
import scala.util.Random

class VcfFilterTest extends ToolTest[Args] {
  def toolCommand: VcfFilter.type = VcfFilter
  import VcfFilter._

  @Test
  def testNoArgs(): Unit = {
    intercept[IllegalArgumentException] {
      VcfFilter.main(Array())
    }
  }

  val veppedPath: String = resourcePath("/VEP_oneline.vcf")
  val starPath: String = resourcePath("/star_genotype.vcf.gz")
  val vepped = new File(veppedPath)
  val star = new File(starPath)
  val rand = new Random()

  @Test
  def testMinCalled(): Unit = {
    val reader = new VCFFileReader(resourceFile("/test.vcf"), false)
    val record = reader.iterator().next()
    reader.close()

    VcfFilter.minCalled(record, 0) shouldBe true
    VcfFilter.minCalled(record, 1) shouldBe true
    VcfFilter.minCalled(record, 2) shouldBe true
    VcfFilter.minCalled(record, 3) shouldBe false
  }

  @Test
  def testOutputTypeVcf(): Unit = {
    val tmp = File.createTempFile("VcfFilter", ".vcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath)
    main(arguments)
  }

  @Test
  def testOutputTypeBcf(): Unit = {
    val tmp = File.createTempFile("VcfFilter", ".bcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath)
    main(arguments)
  }

  @Test
  def testOutputTypeVcfGz(): Unit = {
    val tmp = File.createTempFile("VcfFilter", ".vcf.gz")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments: Array[String] = Array("-I", veppedPath, "-o", tmpPath)
    main(arguments)
  }

  @Test
  def testMustHaveGenotypes(): Unit = {

    /**
      * This should simply not raise an exception
      */
    val tmp = File.createTempFile("VCfFilter", ".vcf")
    tmp.deleteOnExit()
    val arguments: Array[String] =
      Array("-I",
            veppedPath,
            "-o",
            tmp.getAbsolutePath,
            "--mustHaveGenotype",
            "Sample_101:HET")
    main(arguments)

    val size = new VCFFileReader(tmp, false).size
    size shouldBe 1

    val tmp2 = File.createTempFile("VcfFilter", ".vcf.gz")
    tmp2.deleteOnExit()
    val arguments2: Array[String] = Array("-I",
                                          veppedPath,
                                          "-o",
                                          tmp2.getAbsolutePath,
                                          "--mustHaveGenotype",
                                          "Sample_101:HOM_VAR")
    main(arguments2)

    val size2 = new VCFFileReader(tmp2, false).size
    size2 shouldBe 0

  }

  @Test
  def testHasGenotype(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasGenotype(record, List(("Sample_101", GenotypeType.HET))) shouldBe true
    hasGenotype(record, List(("Sample_101", GenotypeType.HOM_VAR))) shouldBe false
    hasGenotype(record, List(("Sample_101", GenotypeType.HOM_REF))) shouldBe false
    hasGenotype(record, List(("Sample_101", GenotypeType.NO_CALL))) shouldBe false
    hasGenotype(record, List(("Sample_101", GenotypeType.MIXED))) shouldBe false

    hasGenotype(record, List(("Sample_103", GenotypeType.HET))) shouldBe false
    hasGenotype(record, List(("Sample_103", GenotypeType.HOM_VAR))) shouldBe false
    hasGenotype(record, List(("Sample_103", GenotypeType.HOM_REF))) shouldBe true
    hasGenotype(record, List(("Sample_103", GenotypeType.NO_CALL))) shouldBe false
    hasGenotype(record, List(("Sample_103", GenotypeType.MIXED))) shouldBe false

    hasGenotype(record,
                List(("Sample_103", GenotypeType.HOM_REF),
                     ("Sample_101", GenotypeType.HET))) shouldBe true
    hasGenotype(record,
                List(("Sample_103", GenotypeType.HET),
                     ("Sample_101", GenotypeType.HOM_REF))) shouldBe false
  }

  @Test
  def testMinQualScore(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    minQualscore(record, 2000) shouldBe false
    minQualscore(record, 1000) shouldBe true

  }

  @Test
  def testHasNonRefCalls(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasNonRefCalls(record) shouldBe true
  }

  @Test
  def testHasCalls(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasCalls(record) shouldBe true
  }

  @Test
  def testHasMinDP(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasMinTotalDepth(record, 100) shouldBe true
    hasMinTotalDepth(record, 200) shouldBe false
  }

  @Test
  def testHasMinSampleDP(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    hasMinSampleDepth(record, 30) shouldBe true
    hasMinSampleDepth(record, 30, 2) shouldBe true
    hasMinSampleDepth(record, 30, 3) shouldBe true
    hasMinSampleDepth(record, 40) shouldBe true
    hasMinSampleDepth(record, 40, 2) shouldBe true
    hasMinSampleDepth(record, 40, 3) shouldBe false
    hasMinSampleDepth(record, 50) shouldBe false
    hasMinSampleDepth(record, 50, 2) shouldBe false
    hasMinSampleDepth(record, 50, 3) shouldBe false
  }

  @Test
  def testHasMinSampleAD(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    minAlternateDepth(record, 0, 3) shouldBe true
    minAlternateDepth(record, 10, 2) shouldBe true
    minAlternateDepth(record, 10, 3) shouldBe false
    minAlternateDepth(record, 20) shouldBe true
    minAlternateDepth(record, 20, 2) shouldBe false
  }

  @Test
  def testHasMinGQ(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    minGenomeQuality(record, 99) shouldBe true
    minGenomeQuality(record, 99, 2) shouldBe true
    minGenomeQuality(record, 99, 3) shouldBe true
  }

  @Test
  def testMustHaveVariant(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    mustHaveVariant(record, List("Sample_101")) shouldBe true
    mustHaveVariant(record, List("Sample_101", "Sample_102")) shouldBe true
    mustHaveVariant(record, List("Sample_101", "Sample_102", "Sample_103")) shouldBe false

    an[IllegalArgumentException] shouldBe thrownBy(
      mustHaveVariant(record, List("notExistant")))

    val starReader = new VCFFileReader(star, false)
    starReader
      .iterator()
      .foreach(x => mustHaveVariant(x, List("Sample_101")) shouldBe false)
  }

  @Test
  def testMustNotHaveVariant(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    mustNotHaveVariant(record, List("Sample_101")) shouldBe false
    mustNotHaveVariant(record, List("Sample_101", "Sample_102")) shouldBe false
    mustNotHaveVariant(record, List("Sample_101", "Sample_102", "Sample_103")) shouldBe false
    mustNotHaveVariant(record, List("Sample_103")) shouldBe true

    an[IllegalArgumentException] shouldBe thrownBy(
      mustHaveVariant(record, List("notExistant")))

    val starReader = new VCFFileReader(star, false)
    starReader
      .iterator()
      .foreach(x => mustHaveVariant(x, List("Sample_101")) shouldBe false)
  }

  @Test
  def testSameGenotype(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    notSameGenotype(record, "Sample_101", "Sample_102") shouldBe false
    notSameGenotype(record, "Sample_101", "Sample_103") shouldBe true
    notSameGenotype(record, "Sample_102", "Sample_103") shouldBe true
  }

  @Test
  def testfilterHetVarToHomVar(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    filterHetVarToHomVar(record, "Sample_101", "Sample_102") shouldBe true
    filterHetVarToHomVar(record, "Sample_101", "Sample_103") shouldBe true
    filterHetVarToHomVar(record, "Sample_102", "Sample_103") shouldBe true
  }

  @Test
  def testDeNovo(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    uniqueVariantInSample(record, "Sample_101") shouldBe false
    uniqueVariantInSample(record, "Sample_102") shouldBe false
    uniqueVariantInSample(record, "Sample_103") shouldBe false
  }

  @Test
  def testResToDom(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()
    val trio = Trio("Sample_101", "Sample_102", "Sample_103")

    resToDom(record, List(trio)) shouldBe false
  }

  @Test
  def testTrioCompound: Boolean = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()
    val trio = Trio("Sample_101", "Sample_102", "Sample_103")

    trioCompound(record, List(trio))
  }

  @Test
  def testDeNovoTrio: Boolean = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()
    val trio = Trio("Sample_101", "Sample_102", "Sample_103")

    denovoTrio(record, List(trio))
  }

  @Test
  def testInIDSet(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    inIdSet(record, Set("rs199537431")) shouldBe true
    inIdSet(record, Set("dummy")) shouldBe false
  }

  @Test
  def testAdvancedGroup(): Unit = {
    val reader = new VCFFileReader(vepped, false)
    val record = reader.iterator().next()

    advancedGroupFilter(record, Nil) shouldBe true
    advancedGroupFilter(record, List(List("Sample_101", "Sample_102"))) shouldBe true
    advancedGroupFilter(record, List(List("Sample_102", "Sample_103"))) shouldBe false
    advancedGroupFilter(record,
                        List(List("Sample_102", "Sample_103"),
                             List("Sample_101", "Sample_102"))) shouldBe false
    advancedGroupFilter(
      record,
      List(List("Sample_102"), List("Sample_101", "Sample_102"))) shouldBe true
  }

  @Test
  def testInfoFieldMustMatch(): Unit = {
    val reader = new VCFFileReader(resourceFile("/test.vcf"), false)
    val record = reader.iterator().next()
    reader.close()

    VcfFilter.infoFieldMustMatch(record, "CSQ", "bla".r) shouldBe true
    VcfFilter.infoFieldMustMatch(record, "CSQ", "bla2".r) shouldBe true
    VcfFilter.infoFieldMustMatch(record, "CSQ", "bla3".r) shouldBe false
    VcfFilter.infoFieldMustMatch(record, "CSQ", ".*l.*".r) shouldBe true
    VcfFilter.infoFieldMustMatch(record, "CSQ", ".*q.*".r) shouldBe false
  }
}
