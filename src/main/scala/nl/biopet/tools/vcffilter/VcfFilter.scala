package nl.biopet.tools.vcffilter

import htsjdk.variant.variantcontext.writer.{AsyncVariantContextWriter, VariantContextWriterBuilder}
import htsjdk.variant.variantcontext.{GenotypeType, VariantContext}
import htsjdk.variant.vcf.VCFFileReader
import nl.biopet.tools.vcffilter.Args.Trio
import nl.biopet.utils.ngs.vcf
import nl.biopet.utils.tool.ToolCommand

import scala.collection.JavaConversions._

object VcfFilter extends ToolCommand[Args] {
  def emptyArgs: Args = Args()
  def argsParser = new ArgsParser(toolName)
  def main(args: Array[String]): Unit = {
    val cmdArgs = cmdArrayToArgs(args)

    logger.info("Start")

    val reader = new VCFFileReader(cmdArgs.inputVcf, false)
    val header = reader.getFileHeader
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setOutputFile(cmdArgs.outputVcf)
        .setReferenceDictionary(header.getSequenceDictionary)
        .build)
    writer.writeHeader(header)

    val invertedWriter = cmdArgs.invertedOutputVcf.collect {
      case x =>
        new VariantContextWriterBuilder()
          .setOutputFile(x)
          .setReferenceDictionary(header.getSequenceDictionary)
          .build
    }
    invertedWriter.foreach(_.writeHeader(header))

    var counterTotal = 0
    var counterLeft = 0
    for (record <- reader) {
      if (cmdArgs.minQualScore.forall(minQualscore(record, _)) &&
        (!cmdArgs.booleanArgs.filterRefCalls || hasNonRefCalls(record)) &&
        (!cmdArgs.booleanArgs.filterNoCalls || hasCalls(record)) &&
        (!cmdArgs.booleanArgs.uniqueOnly || hasUniqeSample(record)) &&
        (!cmdArgs.booleanArgs.sharedOnly || allSamplesVariant(record)) &&
        hasMinTotalDepth(record, cmdArgs.minTotalDepth) &&
        hasMinSampleDepth(record, cmdArgs.minSampleDepth, cmdArgs.minSamplesPass) &&
        minAlternateDepth(record, cmdArgs.minAlternateDepth, cmdArgs.minSamplesPass) &&
        minGenomeQuality(record, cmdArgs.minGenomeQuality, cmdArgs.minSamplesPass) &&
        (cmdArgs.mustHaveVariant.isEmpty || mustHaveVariant(record, cmdArgs.mustHaveVariant)) &&
        (cmdArgs.mustNotHaveVariant.isEmpty || mustNotHaveVariant(record,
          cmdArgs.mustNotHaveVariant)) &&
        calledIn(record, cmdArgs.calledIn) &&
        hasGenotype(record, cmdArgs.mustHaveGenotype) &&
        (cmdArgs.diffGenotype.isEmpty || cmdArgs.diffGenotype.forall(x =>
          notSameGenotype(record, x._1, x._2))) &&
        (
          cmdArgs.filterHetVarToHomVar.isEmpty ||
            cmdArgs.filterHetVarToHomVar.forall(x => filterHetVarToHomVar(record, x._1, x._2))
          ) &&
        uniqueVariantInSample(record, cmdArgs.uniqueVariantInSample) &&
        denovoTrio(record, cmdArgs.deNovoTrio) &&
        denovoTrio(record, cmdArgs.trioLossOfHet, onlyLossHet = true) &&
        resToDom(record, cmdArgs.resToDom) &&
        trioCompound(record, cmdArgs.trioCompound) &&
        advancedGroupFilter(record, cmdArgs.advancedGroups) &&
        (cmdArgs.iDset.isEmpty || inIdSet(record, cmdArgs.iDset))) {
        writer.add(record)
        counterLeft += 1
      } else
        invertedWriter.foreach(_.add(record))
      counterTotal += 1
      if (counterTotal % 100000 == 0)
        logger.info(s"$counterTotal variants processed, $counterLeft passed filter")
    }
    logger.info(s"$counterTotal variants processed, $counterLeft passed filter")
    reader.close()
    writer.close()
    invertedWriter.foreach(_.close())
    logger.info("Done")
  }

  /**
    * Checks if given samples are called
    *
    * @param record VCF record
    * @param samples Samples that need this sample to be called
    * @return false when filters fail
    */
  def calledIn(record: VariantContext, samples: List[String]): Boolean = {
    if (!samples.forall(record.getGenotype(_).isCalled)) false
    else true
  }

  /**
    * Checks if given genotypes for given samples are there
    *
    * @param record VCF record
    * @param samplesGenotypes samples and their associated genotypes to be checked (of format sample:genotype)
    * @return false when filter fails
    */
  def hasGenotype(record: VariantContext,
                  samplesGenotypes: List[(String, GenotypeType)]): Boolean = {
    samplesGenotypes.forall { x =>
      record.getGenotype(x._1).getType == x._2
    }
  }

  /**
    * Checks if record has atleast minQualScore
    *
    * @param record VCF record
    * @param minQualScore Minimal quality score
    * @return false when filters fail
    */
  def minQualscore(record: VariantContext, minQualScore: Double): Boolean = {
    record.getPhredScaledQual >= minQualScore
  }

  /** returns true record contains Non reference genotypes */
  def hasNonRefCalls(record: VariantContext): Boolean = {
    record.getGenotypes.exists(g => !g.isHomRef)
  }

  /** returns true when record has calls */
  def hasCalls(record: VariantContext): Boolean = {
    record.getGenotypes.exists(g => !g.isNoCall)
  }

  /** Checks if there is a variant in only 1 sample */
  def hasUniqeSample(record: VariantContext): Boolean = {
    record.getSampleNames.exists(uniqueVariantInSample(record, _))
  }

  /** Checks if all samples are a variant */
  def allSamplesVariant(record: VariantContext): Boolean = {
    record.getGenotypes.forall(g =>
      !g.isNonInformative && g.getAlleles.exists(a => a.isNonReference && !a.isNoCall))
  }

  /** returns true when DP INFO field is atleast the given value */
  def hasMinTotalDepth(record: VariantContext, minTotalDepth: Int): Boolean = {
    record.getAttributeAsInt("DP", -1) >= minTotalDepth
  }

  /**
    * Checks if DP genotype field have a minimal value
    *
    * @param record VCF record
    * @param minSampleDepth minimal depth
    * @param minSamplesPass Minimal number of samples to pass filter
    * @return true if filter passed
    */
  def hasMinSampleDepth(record: VariantContext,
                        minSampleDepth: Int,
                        minSamplesPass: Int = 1): Boolean = {
    record.getGenotypes.count(genotype => {
      val DP = if (genotype.hasDP) genotype.getDP else -1
      DP >= minSampleDepth
    }) >= minSamplesPass
  }

  /**
    * Checks if non-ref AD genotype field have a minimal value
    *
    * @param record VCF record
    * @param minAlternateDepth minimal depth
    * @param minSamplesPass Minimal number of samples to pass filter
    * @return true if filter passed
    */
  def minAlternateDepth(record: VariantContext,
                        minAlternateDepth: Int,
                        minSamplesPass: Int = 1): Boolean = {
    record.getGenotypes.count(genotype => {
      val AD = if (genotype.hasAD) List(genotype.getAD: _*) else Nil
      if (AD.nonEmpty && minAlternateDepth >= 0) AD.tail.count(_ >= minAlternateDepth) > 0
      else true
    }) >= minSamplesPass
  }

  /**
    * Checks if genome quality field has minimum value
    *
    * @param record VCF record
    * @param minGQ smallest GQ allowed
    * @param minSamplesPass number of samples to consider
    * @return
    */
  def minGenomeQuality(record: VariantContext, minGQ: Int, minSamplesPass: Int = 1): Boolean = {
    record.getGenotypes.count(
      x =>
        if (minGQ == 0) true
        else if (!x.hasGQ) false
        else if (x.getGQ >= minGQ) true
        else false) >= minSamplesPass
  }

  /**
    * Checks if given samples does have a variant hin this record
    *
    * @param record VCF record
    * @param samples List of samples that should have this variant
    * @return true if filter passed
    */
  def mustHaveVariant(record: VariantContext, samples: List[String]): Boolean = {
    samples.foreach { s =>
      if (!record.getSampleNames.toList.contains(s)) {
        throw new IllegalArgumentException(s"Sample name $s does not exist in VCF file")
      }
    }
    !samples
      .map(record.getGenotype)
      .exists(a => a.isHomRef || a.isNoCall || vcf.isCompoundNoCall(a))
  }

  /**
    * Checks if given samples does have a variant hin this record
    *
    * @param record VCF record
    * @param samples List of samples that should have this variant
    * @return true if filter passed
    */
  def mustNotHaveVariant(record: VariantContext, samples: List[String]): Boolean = {
    samples.foreach { s =>
      if (!record.getSampleNames.toList.contains(s)) {
        throw new IllegalArgumentException(s"Sample name $s does not exist in VCF file")
      }
    }
    samples
      .map(record.getGenotype)
      .forall(a => a.isHomRef || a.isNoCall || vcf.isCompoundNoCall(a))
  }

  /** Checks if given samples have the same genotype */
  def notSameGenotype(record: VariantContext, sample1: String, sample2: String): Boolean = {
    val genotype1 = record.getGenotype(sample1)
    val genotype2 = record.getGenotype(sample2)
    if (genotype1.sameGenotype(genotype2)) false
    else true
  }

  /** Checks if sample1 is hetrozygous and if sample2 is homozygous for a alternative allele in sample1 */
  def filterHetVarToHomVar(record: VariantContext, sample1: String, sample2: String): Boolean = {
    val genotype1 = record.getGenotype(sample1)
    val genotype2 = record.getGenotype(sample2)
    if (genotype1.isHet && !genotype1.getAlleles.forall(_.isNonReference)) {
      for (allele <- genotype1.getAlleles if allele.isNonReference) {
        if (genotype2.getAlleles.forall(_.basesMatch(allele))) return false
      }
    }
    true
  }

  /** Checks if given sample have alternative alleles that are unique in the VCF record */
  def uniqueVariantInSample(record: VariantContext, sample: String): Boolean = {
    if (sample == null) return true
    val genotype = record.getGenotype(sample)
    if (genotype.isNoCall) return false
    if (genotype.getAlleles.forall(_.isReference)) return false
    for (allele <- genotype.getAlleles if allele.isNonReference) {
      for (g <- record.getGenotypes if g.getSampleName != sample) {
        if (g.getAlleles.exists(_.basesMatch(allele))) return false
      }
    }
    true
  }

  /** Return true when variant is homozygous in the child and hetrozygous in parants */
  def resToDom(record: VariantContext, trios: List[Trio]): Boolean = {
    for (trio <- trios) {
      val child = record.getGenotype(trio.child)

      if (child.isHomVar && child.getAlleles.forall(allele => {
        record.getGenotype(trio.father).countAllele(allele) == 1 &&
          record.getGenotype(trio.mother).countAllele(allele) == 1
      })) return true
    }
    trios.isEmpty
  }

  /** Returns true when variant a compound variant in the child and hetrozygous in parants */
  def trioCompound(record: VariantContext, trios: List[Trio]): Boolean = {
    for (trio <- trios) {
      val child = record.getGenotype(trio.child)

      if (child.isHetNonRef && child.getAlleles.forall(allele => {
        record.getGenotype(trio.father).countAllele(allele) >= 1 &&
          record.getGenotype(trio.mother).countAllele(allele) >= 1
      })) return true
    }
    trios.isEmpty
  }

  /** Returns true when child got a deNovo variant */
  def denovoTrio(record: VariantContext,
                 trios: List[Trio],
                 onlyLossHet: Boolean = false): Boolean = {
    for (trio <- trios) {
      val child = record.getGenotype(trio.child)
      val father = record.getGenotype(trio.father)
      val mother = record.getGenotype(trio.mother)

      for (allele <- child.getAlleles) {
        val childCount = child.countAllele(allele)
        val fatherCount = father.countAllele(allele)
        val motherCount = mother.countAllele(allele)

        if (onlyLossHet) {
          if (childCount == 2 && ((fatherCount == 2 && motherCount == 0) ||
            (fatherCount == 0 && motherCount == 2))) return true
        } else {
          if (childCount == 1 && fatherCount == 0 && motherCount == 0) return true
          else if (childCount == 2 && (fatherCount == 0 || motherCount == 0)) return true
        }
      }
    }
    trios.isEmpty
  }

  /** Returns true when VCF record contains a ID from the given list */
  def inIdSet(record: VariantContext, idSet: Set[String]): Boolean = {
    record.getID.split(",").exists(idSet.contains)
  }

  /**
    * returns true when for all groups all or none members have a variants,
    * records with partial groups are discarded
    */
  def advancedGroupFilter(record: VariantContext, groups: List[List[String]]): Boolean = {
    val samples = record.getGenotypes
      .map(a => a.getSampleName -> (a.isHomRef || a.isNoCall || vcf.isCompoundNoCall(a)))
      .toMap

    val g: List[Option[Boolean]] = groups.map { group =>
      val total = group.size
      val count = group.count(samples(_))
      if (count == 0) Some(false)
      else if (total == count) Some(true)
      else None
    }

    !g.contains(None)
  }
}
