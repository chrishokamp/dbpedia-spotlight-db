package org.dbpedia.spotlight.model

/**
 * @author Chris Hokamp
 *         - indexes a token using its tfidf values from the docs in which it occurs (uses TokenOccurenceIndexer as model)
 */

trait DocOccurrenceIndexer {

  def addDocOccurrence(token: Token, resource: DBpediaResource, weight: Double)

  def addDocOccurrence(token: Token, resourceWeights: Map[Int, Double])

  def addDocOccurrences(occs: Map[Token, Map[Int, Double]])


  def writeDocOccurences()

  def createEsaVectorStore(n: Int)

}
