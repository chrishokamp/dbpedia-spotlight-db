package org.dbpedia.spotlight.model

import collection.mutable

/**
 * @author Chris Hokamp
 */

trait InvertedIndexIndexer {

  def addResourceSet (tokenId: Int, docSet: mutable.Map[Int, Double])


}
