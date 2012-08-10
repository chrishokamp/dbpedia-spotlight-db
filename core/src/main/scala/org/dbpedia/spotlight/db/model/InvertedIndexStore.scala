package org.dbpedia.spotlight.db.model

import org.dbpedia.spotlight.model.{DBpediaResource, Token}
import collection.mutable

/**
 * @author Chris Hokamp
 */

trait InvertedIndexStore {
  //TODO: confirm logic behind making this mutable
  def getResources (token: Token): mutable.HashMap[Int, Double]
  def getDocFreq (token: Token): Int


}
