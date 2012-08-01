package org.dbpedia.spotlight.model

/**
 * @author Chris Hokamp
 */

trait DocFreqIndexer {

  def addDocFreqToken (token: Token, df: Int)

  def addDocFreqTokens (tokenCounts: Array[Int])
}
