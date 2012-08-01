package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{DBpediaResource, Token}

/**
 * @author Chris Hokamp
 *
 * - pass in a token, return a vector where columns are associations with DBpediaResources
 *
 * - this is implicitly an inverted index, because it stores doc indexes for each token
 *
 */

trait EsaVectorStore {
  def getResourceVector(token: Token): Map[DBpediaResource, Double]

}
