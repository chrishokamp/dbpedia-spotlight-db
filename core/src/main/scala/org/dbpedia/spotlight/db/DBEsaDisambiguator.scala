package org.dbpedia.spotlight.db

import model._
import org.dbpedia.spotlight.disambiguate.mixtures.Mixture
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.apache.commons.logging.LogFactory
import similarity.{TfidfSimilarity, TFICFSimilarity}
import org.dbpedia.spotlight.model._
import collection.mutable
import scala.math
import org.dbpedia.spotlight.exceptions.{InputException, SurfaceFormNotFoundException}

/**
 * @author Chris Hokamp
 */

class DBEsaDisambiguator (
  tokenStore: TokenStore,
  surfaceFormStore: SurfaceFormStore,
  resourceStore: ResourceStore,
  candidateMap: CandidateMapStore,
  //Chris: testing...
  invertedIndex: InvertedIndexStore,
  esaVectorStore: EsaVectorStore,
  //contextStore: ContextStore,
  //Chris: END TESTING
  tokenizer: Tokenizer,
  mixture: Mixture
  ) extends ParagraphDisambiguator {

  private val LOG = LogFactory.getLog(this.getClass)

  val similarity = new TfidfSimilarity()
  def getScores(text: Text, candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] = {

    val tokens = tokenizer.tokenize(text).map{ ts: String => tokenStore.getToken(ts) }
    val query = tokens.groupBy(identity).mapValues(_.size)
    val queryEsaVector = new mutable.HashMap[Int, Double]()
    query.foreach {
      case (token: Token, count: Int) => {
        //TODO: fix hard-coding of doc-count
        val idf = math.log(3000000/invertedIndex.getDocFreq(token))
        val tfidf: Double = count*idf
        val tokenDocVect = invertedIndex.getResources(token)
        if (tokenDocVect != null) {
          tokenDocVect.foreach {
            case (i: Int, weight: Double) => {
             queryEsaVector.put(i, (queryEsaVector.getOrElse(i, 0.0)) + (weight*tfidf))
           }
          }
        }
      }
    }

    val contextVectors = candidates.map{ candRes: DBpediaResource =>
      (candRes -> esaVectorStore.getDocVector(candRes))
    }.toMap
    //TODO: understand why this is necessary
    val mutableVecs = collection.mutable.Map(contextVectors.toSeq: _*)

    similarity.score(queryEsaVector, mutableVecs)
  }

  //Copied from DBTwoStepDisambiguator
  val MAX_CANDIDATES = 500
  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

    LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

    if (paragraph.occurrences.size == 0)
      return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

    // step1: get candidates for all surface forms
    var allCandidateResources = Set[DBpediaResource]();
    val occs = paragraph.occurrences.foldLeft(
      Map[SurfaceFormOccurrence, List[Candidate]]())(
      (acc, sfOcc) => {

        LOG.debug("Searching...")

        val candidateRes = try {
          val sf = surfaceFormStore.getSurfaceForm(sfOcc.surfaceForm.name)
          val cands = candidateMap.getCandidates(sf)
          LOG.debug("# candidates for: %s = %s.".format(sf, cands.size))

          if (cands.size > MAX_CANDIDATES) {
            LOG.debug("Reducing number of candidates to %d.".format(MAX_CANDIDATES))
            cands.toList.sortBy( _.prior ).reverse.take(MAX_CANDIDATES).toSet
          } else {
            cands
          }
        } catch {
          case e: SurfaceFormNotFoundException => Set[Candidate]()
        }

        allCandidateResources ++= candidateRes.map(_.resource)

        acc + (sfOcc -> candidateRes.toList)
      })


    // step2: query once for the paragraph context, get scores for each candidate resource
    val contextScores = getScores(paragraph.text, allCandidateResources)

    // pick the best k for each surface form
    occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc, aSfOcc) => {
      val candOccs = occs.getOrElse(aSfOcc, List[Candidate]())
        .map{ cand: Candidate => {
        val resOcc = new DBpediaResourceOccurrence(
          "",
          cand.resource,
          cand.surfaceForm,
          aSfOcc.context,
          aSfOcc.textOffset,
          Provenance.Undefined,
          0.0,
          0.0,
          contextScores.getOrElse(cand.resource, 0.0)
        )
        resOcc.setSimilarityScore(
          (1234.3989 * cand.prior) +
            0.9968 * resOcc.contextualScore +
            -0.0275
        )
        resOcc
      }
      }
        .filter{ o => !java.lang.Double.isNaN(o.similarityScore) }
        .sortBy( o => o.similarityScore )
        .reverse
        .take(k)

      acc + (aSfOcc -> candOccs)
    })

  }

  @throws(classOf[InputException])
  def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
    // return first from each candidate set
    bestK(paragraph, 5)
      .filter(kv =>
      kv._2.nonEmpty)
      .map( kv =>
      kv._2.head)
      .toList
  }

  def name = "Database-backed 2 Step ESA Disambiguator"


}
