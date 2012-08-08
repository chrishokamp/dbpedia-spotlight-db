package org.dbpedia.spotlight.model

import collection.mutable

/**
 * @author Chris Hokamp
 *         - indexes a token using its tfidf values from the docs in which it occurs (uses TokenOccurenceIndexer as model)
 *         - note that this is implicitly an inverted index of the corpus as well
 */

trait DocOccurrenceIndexer {

  def addDocOccurrence(resource: DBpediaResource, token: Token, weight: Double)

  def addDocOccurrence(resource: DBpediaResource, resourceWeights: mutable.Map[Int, Double])

  //def addDocOccurrences(occs: Map[Token, Map[Int, Double]])


  //def writeDocOccurrences()

  def createEsaVectorStore(n: Int)

}
