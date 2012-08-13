package org.dbpedia.spotlight.model

import collection.mutable

/**
 * @author Chris Hokamp
 */

trait InvertedIndexIndexer {

  def addResourceSet (tokenId: Int, docSet: mutable.HashMap[Int, Double])
  def addResource (tokenId: Int, doc: (Int, Double))
  def setDocFrequency (tokenId: Int, docFreq: Int)

}
