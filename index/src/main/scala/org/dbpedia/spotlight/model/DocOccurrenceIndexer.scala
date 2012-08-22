package org.dbpedia.spotlight.model

import collection.mutable

/**
 * @author Chris Hokamp
 *         - indexes resources and stores a vector of weights
 */

trait DocOccurrenceIndexer {

  //def addDocOccurrence(resource: DBpediaResource, token: Token, weight: Double)

  def addDocOccurrence(resource: DBpediaResource, resourceWeights: mutable.Map[Int, Double])

  //def addDocOccurrences(occs: Map[Token, Map[Int, Double]])


  //def writeDocOccurrences()

  def createEsaVectorStore(n: Int)

}
