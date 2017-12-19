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
