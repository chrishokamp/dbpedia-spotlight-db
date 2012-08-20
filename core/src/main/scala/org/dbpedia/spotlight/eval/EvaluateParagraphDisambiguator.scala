/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.dbpedia.spotlight.eval

import filter.occurrences.{RedirectResolveFilter, UriWhitelistFilter}
import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.model._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.disambiguate.{CuttingEdgeDisambiguator, TwoStepDisambiguator, ParagraphDisambiguator}
import java.io.{PrintWriter, File}
//import org.dbpedia.spotlight.corpus.AidaCorpus

/**
 * Evaluation for disambiguators that take one paragraph at a time, instead of one occurrence at a time.
 *
 * @author pablomendes
 */
object EvaluateParagraphDisambiguator {

  private val LOG = LogFactory.getLog(this.getClass)

  //TESTING with redirect resolution
  def filter(bestK: Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]) : Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
    bestK
  }

  def getRank(correctOccurrence: DBpediaResourceOccurrence, bestK: List[DBpediaResourceOccurrence]) = {
    LOG.debug("Ranking for: %s -> %s".format(correctOccurrence.surfaceForm, correctOccurrence.resource))
    LOG.debug("K=%s".format(bestK.size));
    var rank, i = 0
    //LOG.debug("                : prior \t context \t final \t uri")
    LOG.debug("                : context \t uri")
    for (predictedOccurrence <- bestK) {
      i = i + 1
      if (correctOccurrence.resource equals predictedOccurrence.resource) {
        rank = i
        //LOG.debug("  **     correct: %.5f \t %.5f \t %.5f \t %s".format(predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource))
        LOG.debug("  **     correct: %.5f \t %s".format(predictedOccurrence.contextualScore, predictedOccurrence.resource))
      }
      else {
        //LOG.debug("       spotlight: %.5f \t %.5f \t %.5f \t %s".format(predictedOccurrence.resource.prior, predictedOccurrence.contextualScore, predictedOccurrence.similarityScore, predictedOccurrence.resource))
        LOG.debug("       spotlight: %.5f \t %s".format(predictedOccurrence.contextualScore, predictedOccurrence.resource))
      }
    }
    if (rank == 0)
      LOG.debug("  **   not found: %.5s \t %.5s \t %.5s \t %s".format("NA", "NA", "NA", correctOccurrence.resource))
    LOG.debug("Rank: %s".format(rank))
    rank
  }
                                                                                                                                   //TODO: change back to OccurrenceFilter
  def evaluate(testSource: AnnotatedTextSource, disambiguator: ParagraphDisambiguator, outputs: List[OutputGenerator], occFilters: List[OccurrenceFilter]) {
    val startTime = System.nanoTime()

    var i = 0;
    var nZeros = 0
    var nCorrects = 0
    var nOccurrences = 0
    var nOriginalOccurrences = 0
    val paragraphs = testSource.toList
    var totalParagraphs = paragraphs.size
    //testSource.view(10000,15000)
    val mrrResults = paragraphs.map(a => {
      i = i + 1
      LOG.info("Paragraph %d/%d: %s.".format(i, totalParagraphs, a.id))
      val paragraph = Factory.Paragraph.from(a)
      nOriginalOccurrences = nOriginalOccurrences + a.occurrences.toTraversable.size

      var acc = 0.0
      try {
        val bestK = filter(disambiguator.bestK(paragraph, 100))

        //TODO: add the other filters to discount disambiguations, nils, etc...
        val goldOccurrences = occFilters.foldLeft(a.occurrences.toTraversable){ (o,f) => f.filterOccs(o) }

        goldOccurrences.foreach( correctOccurrence => {
          nOccurrences = nOccurrences + 1

        val disambResult = new DisambiguationResult(correctOccurrence,                                                     // correct
                                                    bestK.getOrElse(Factory.SurfaceFormOccurrence.from(correctOccurrence), // predicted
                                                    List[DBpediaResourceOccurrence]()))

        outputs.foreach(_.write(disambResult))

          val invRank = if (disambResult.rank>0) (1.0/disambResult.rank) else  0.0
          if (disambResult.rank==0)  {
            nZeros = nZeros + 1
          } else if (disambResult.rank==1)  {
            nCorrects = nCorrects + 1
          }
          acc = acc + invRank
        });
        outputs.foreach(_.flush)
      } catch {
        case e: Exception => LOG.error(e)
      }
      val mrr = if (a.occurrences.size==0) 0.0 else acc / a.occurrences.size
      LOG.info("Mean Reciprocal Rank (MRR) = %.5f".format(mrr))
      mrr
    })
    val endTime = System.nanoTime()
    LOG.info("********************")
    LOG.info("Corpus: %s".format(testSource.name))
    LOG.info("Number of occs: %d (original), %d (processed)".format(nOriginalOccurrences,nOccurrences))
    LOG.info("Disambiguator: %s".format(disambiguator.name))
    LOG.info("Correct URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences))
    LOG.info("Accuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences))
    LOG.info("Global MRR: %s".format(mrrResults.sum / mrrResults.size))
    LOG.info("Elapsed time: %s sec".format( (endTime-startTime) / 1000000000))
    LOG.info("********************")

    val disambigSummary = "Corpus: %s".format(testSource.name) +
      "\nNumber of occs: %d (original), %d (processed)".format(nOriginalOccurrences,nOccurrences) +
      "\nDisambiguator: %s".format(disambiguator.name)+
      "\nCorrect URI not found = %d / %d = %.3f".format(nZeros,nOccurrences,nZeros.toDouble/nOccurrences)+
      "\nAccuracy = %d / %d = %.3f".format(nCorrects,nOccurrences,nCorrects.toDouble/nOccurrences) +
      "\nGlobal MRR: %s".format(mrrResults.sum / mrrResults.size)+
      "\nElapsed time: %s sec".format( (endTime-startTime) / 1000000000);

    outputs.foreach(_.summary(disambigSummary))

    outputs.foreach(_.flush)
  }

  def main(args: Array[String]) {
    //val indexDir: String = args(0)  //"e:\\dbpa\\data\\index\\index-that-works\\Index.wikipediaTraining.Merged."
    val config = new SpotlightConfiguration(args(0));

    //TODO: testing for redirect resolution
    val redirectTCFileName  = if (args.size>1) args(1) else "data/redirects_tc.tsv" //produced by ExtractCandidateMap
    val conceptURIsFileName  = if (args.size>2) args(2) else "data/conceptURIs.list" //produced by ExtractCandidateMap

    val occFilters = List(UriWhitelistFilter.fromFile(new File(conceptURIsFileName)), RedirectResolveFilter.fromFile(new File(redirectTCFileName)))

    val testFileName: String = args(1) //"e:\\dbpa\\data\\index\\dbpedia36data\\test\\test100k.tsv"
    val output = new PrintWriter(testFileName + ".pareval.log")

    //val default : Disambiguator = new DefaultDisambiguator(config)
    //val test : Disambiguator = new GraphCentralityDisambiguator(config)

    val factory = new SpotlightFactory(config)
    val disambiguators = Set(new CuttingEdgeDisambiguator(factory),
      new TwoStepDisambiguator(factory)
    )

    val paragraphs = AnnotatedTextSource
      .fromOccurrencesFile(new File(testFileName))


    // Read some text to test.
    //TODO: Chris -  commented for testing
    //disambiguators.foreach(d => evaluate(paragraphs, d, output))

    output.close
  }
}