package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{DocFrequencyStore, InvertedIndexStore}
import org.dbpedia.spotlight.model.Token

/**
 * @author Chris Hokamp
 */
//TODO: remove abstract
abstract class MemoryInvertedIndexStore
  extends MemoryStore
  with InvertedIndexStore
  with DocFrequencyStore {

  var docFreq = Array[Int]()

  def size =  docFreq.size

  def getDocFreq (token: Token):  Int = {
    val i = token.id
    docFreq(i)
  }

}
