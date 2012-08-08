package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{DBpediaResource, Token}
import collection.mutable

/**
 * @author Chris Hokamp
 *
 * - pass in a token, return a vector where columns are associations with DBpediaResources
 *
 * - this is implicitly an inverted index, because it stores doc indexes for each token
 *
 */

trait EsaVectorStore {
  def getDocVector(resource: DBpediaResource): mutable.Map[Int, Double]

}
