package nl.biopet.tools.vcffilter

import java.io.File

import htsjdk.variant.variantcontext.GenotypeType

case class Args(inputVcf: File = null,
                outputVcf: File = null,
                invertedOutputVcf: Option[File] = None,
                minQualScore: Option[Double] = None,
                minSampleDepth: Int = -1,
                minTotalDepth: Int = -1,
                minAlternateDepth: Int = -1,
                minSamplesPass: Int = 1,
                mustHaveVariant: List[String] = Nil,
                mustNotHaveVariant: List[String] = Nil,
                calledIn: List[String] = Nil,
                mustHaveGenotype: List[(String, GenotypeType)] = Nil,
                uniqueVariantInSample: String = null,
                resToDom: List[Args.Trio] = Nil,
                trioCompound: List[Args.Trio] = Nil,
                deNovoTrio: List[Args.Trio] = Nil,
                trioLossOfHet: List[Args.Trio] = Nil,
                booleanArgs: Args.BooleanArgs = Args.BooleanArgs(),
                diffGenotype: List[(String, String)] = Nil,
                filterHetVarToHomVar: List[(String, String)] = Nil,
                iDset: Set[String] = Set(),
                minGenomeQuality: Int = 0,
                advancedGroups: List[List[String]] = Nil)

object Args {
  case class BooleanArgs(uniqueOnly: Boolean = false,
                         sharedOnly: Boolean = false,
                         filterRefCalls: Boolean = false,
                         filterNoCalls: Boolean = false)

  case class Trio(child: String, father: String, mother: String) {
    def this(arg: String) = {
      this(arg.split(":")(0), arg.split(":")(1), arg.split(":")(2))
    }
  }
}