package org.dbpedia.spotlight.db.similarity

import org.dbpedia.spotlight.model.{DBpediaResource, Token}
import collection.{mutable, immutable}
import util.Random

/**
 * @author Chris Hokamp
 *  - return a random score
 */
class RandomSimilarity extends ContextSimilarity {


  def score(query: java.util.Map[Token, Int], candidateContexts: immutable.Map[DBpediaResource, java.util.Map[Token, Int]]): mutable.Map[DBpediaResource, Double] = {

    //val allDocs = candidateContexts.values
    val scores = mutable.HashMap[DBpediaResource, Double]()

    candidateContexts.keys foreach { candRes: DBpediaResource => {
      scores(candRes) = RandomGaussian.getGaussian
    }
    }
    scores
  }

  object RandomGaussian {
    val mMean = 100.0f
    val mVariance = 5.0f

    var fRandom = new Random

    def getGaussian: Double = {
      mMean + fRandom.nextGaussian() * mVariance
    }
  }
}
