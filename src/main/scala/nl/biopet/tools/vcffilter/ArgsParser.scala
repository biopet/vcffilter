/*
 * Copyright (c) 2014 Sequencing Analysis Support Core - Leiden University Medical Center
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
import nl.biopet.tools.vcffilter.Args.Trio
import nl.biopet.utils.tool.{AbstractOptParser, ToolCommand}

import scala.io.Source

class ArgsParser(toolCommand: ToolCommand[Args])
    extends AbstractOptParser[Args](toolCommand) {
  opt[File]('I', "inputVcf") required () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(inputVcf = x)
  } text "Input vcf file"
  opt[File]('o', "outputVcf") required () maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(outputVcf = x)
  } text "Output vcf file"
  opt[File]("invertedOutputVcf") maxOccurs 1 valueName "<file>" action {
    (x, c) =>
      c.copy(invertedOutputVcf = Some(x))
  } text "inverted output vcf file"
  opt[Int]("minSampleDepth") unbounded () valueName "<int>" action { (x, c) =>
    c.copy(minSampleDepth = x)
  } text "Min value for DP in genotype fields"
  opt[Int]("minTotalDepth") unbounded () valueName "<int>" action { (x, c) =>
    c.copy(minTotalDepth = x)
  } text "Min value of DP field in INFO fields"
  opt[Int]("minAlternateDepth") unbounded () valueName "<int>" action {
    (x, c) =>
      c.copy(minAlternateDepth = x)
  } text "Min value of AD field in genotype fields"
  opt[Int]("minSamplesPass") unbounded () valueName "<int>" action { (x, c) =>
    c.copy(minSamplesPass = x)
  } text "Min number off samples to pass --minAlternateDepth, --minBamAlternateDepth and --minSampleDepth"
  opt[String]("resToDom") unbounded () valueName "<child:father:mother>" action {
    (x, c) =>
      c.copy(resToDom = new Trio(x) :: c.resToDom)
  } text "Only shows variants where child is homozygous and both parants hetrozygous"
  opt[String]("trioCompound") unbounded () valueName "<child:father:mother>" action {
    (x, c) =>
      c.copy(trioCompound = new Trio(x) :: c.trioCompound)
  } text "Only shows variants where child is a compound variant combined from both parants"
  opt[String]("deNovoInSample") maxOccurs 1 unbounded () valueName "<sample>" action {
    (x, c) =>
      c.copy(uniqueVariantInSample = x)
  } text "Only show variants that contain unique alleles in complete set for given sample"
  opt[String]("deNovoTrio") unbounded () valueName "<child:father:mother>" action {
    (x, c) =>
      c.copy(deNovoTrio = new Trio(x) :: c.deNovoTrio)
  } text "Only show variants that are denovo in the trio"
  opt[String]("trioLossOfHet") unbounded () valueName "<child:father:mother>" action {
    (x, c) =>
      c.copy(trioLossOfHet = new Trio(x) :: c.trioLossOfHet)
  } text "Only show variants where a loss of hetrozygosity is detected"
  opt[String]("mustHaveVariant") unbounded () valueName "<sample>" action {
    (x, c) =>
      c.copy(mustHaveVariant = x :: c.mustHaveVariant)
  } text "Given sample must have 1 alternative allele"
  opt[String]("mustNotHaveVariant") unbounded () valueName "<sample>" action {
    (x, c) =>
      c.copy(mustNotHaveVariant = x :: c.mustNotHaveVariant)
  } text "Given sample may not have alternative alleles"
  opt[String]("calledIn") unbounded () valueName "<sample>" action { (x, c) =>
    c.copy(calledIn = x :: c.calledIn)
  } text "Must be called in this sample"
  opt[String]("mustHaveGenotype") unbounded () valueName "<sample:genotype>" action {
    (x, c) =>
      c.copy(
        mustHaveGenotype = (x.split(":")(0),
                            GenotypeType
                              .valueOf(x.split(":")(1))) :: c.mustHaveGenotype)
  } validate { x =>
    if (x.split(":").length == 2 && GenotypeType
          .values()
          .map(_.toString)
          .contains(x.split(":")(1)))
      success
    else
      failure("--mustHaveGenotype should be in this format: sample:genotype")
  } text "Must have genotoype <genotype> for this sample. Genotype can be " + GenotypeType
    .values()
    .mkString(", ")
  opt[String]("diffGenotype") unbounded () valueName "<sample:sample>" action {
    (x, c) =>
      c.copy(
        diffGenotype = (x.split(":")(0), x.split(":")(1)) :: c.diffGenotype)
  } validate { x =>
    if (x.split(":").length == 2) success
    else failure("--notSameGenotype should be in this format: sample:sample")
  } text "Given samples must have a different genotype"
  opt[String]("filterHetVarToHomVar") unbounded () valueName "<sample:sample>" action {
    (x, c) =>
      c.copy(
        filterHetVarToHomVar = (x.split(":")(0), x.split(":")(1)) :: c.filterHetVarToHomVar)
  } validate { x =>
    if (x.split(":").length == 2) success
    else
      failure("--filterHetVarToHomVar should be in this format: sample:sample")
  } text "If variants in sample 1 are heterogeneous and alternative alleles are homogeneous in sample 2 variants are filtered"
  opt[Unit]("filterRefCalls") unbounded () action { (_, c) =>
    c.copy(booleanArgs = c.booleanArgs.copy(filterRefCalls = true))
  } text "Filter when there are only ref calls"
  opt[Unit]("filterNoCalls") unbounded () action { (_, c) =>
    c.copy(booleanArgs = c.booleanArgs.copy(filterNoCalls = true))
  } text "Filter when there are only no calls"
  opt[Unit]("uniqueOnly") unbounded () action { (_, c) =>
    c.copy(booleanArgs = c.booleanArgs.copy(uniqueOnly = true))
  } text "Filter when there more then 1 sample have this variant"
  opt[Unit]("sharedOnly") unbounded () action { (_, c) =>
    c.copy(booleanArgs = c.booleanArgs.copy(sharedOnly = true))
  } text "Filter when not all samples have this variant"
  opt[Double]("minQualScore") unbounded () action { (x, c) =>
    c.copy(minQualScore = Some(x))
  } text "Min qual score"
  opt[String]("id") unbounded () action { (x, c) =>
    c.copy(iDset = c.iDset + x)
  } text "Id that may pass the filter"
  opt[File]("idFile") unbounded () action { (x, c) =>
    c.copy(iDset = c.iDset ++ Source.fromFile(x).getLines())
  } text "File that contain list of IDs to get from vcf file"
  opt[Int]("minGenomeQuality") unbounded () action { (x, c) =>
    c.copy(minGenomeQuality = x)
  } text "The minimum value in the Genome Quality field."
  opt[String]("advancedGroups") unbounded () action { (x, c) =>
    c.copy(advancedGroups = x.split(",").toList :: c.advancedGroups)
  } text "All members of groups sprated with a ','"
}
