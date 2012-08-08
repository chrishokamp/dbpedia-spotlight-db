package org.dbpedia.spotlight.db.similarity

import org.apache.commons.logging.LogFactory
import collection.mutable
import org.dbpedia.spotlight.model.DBpediaResource
import scala.math
import collection.immutable.HashMap

/**
 * @author Chris Hokamp
 */

class TfidfSimilarity {

  private val LOG = LogFactory.getLog(this.getClass)

  //vector for query must already be built for this to work
  def score(query: mutable.Map[Int, Double], candidateVectors: mutable.Map[DBpediaResource, mutable.Map[Int, Double]]): mutable.Map[DBpediaResource, Double] = {
    val scores = mutable.HashMap[DBpediaResource, Double]()

    query.foreach {
      case (resource, weight) => {
        candidateVectors.keys foreach {cand: DBpediaResource => {
          val docVect = candidateVectors.getOrElse(cand, null)
          scores(cand) = scores.getOrElse(cand, 0.0) + (weight*docVect.getOrElse(resource, 0.0))}
        }
      }
    }
    val queryLen = eucLen(query)
    candidateVectors.keys foreach {cand: DBpediaResource =>
       val docLen = eucLen(candidateVectors(cand))
       scores(cand) = scores(cand) / (queryLen * docLen)
    }
    scores
  }


  def eucLen (vect: mutable.Map[Int, Double]): Double = {
    var sum: Double = 0.0
    vect.foreach { case(i: Int, d: Double) =>
      {sum += (d*d)}
    }
    val len = math.sqrt(sum)
    len
  }
  /*
  def eucLen (vect: Map[Int, Double]): Double = {
    var sum: Double = 0.0
    vect.foreach { case(i: Int, d: Double) =>
      {sum += (d*d)}
    }
    val len = math.sqrt(sum)
    len
  }
  */

}
