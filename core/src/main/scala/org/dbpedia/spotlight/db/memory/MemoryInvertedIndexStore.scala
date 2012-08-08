package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{DocFrequencyStore, InvertedIndexStore}
import org.dbpedia.spotlight.model.Token
import collection.mutable

/**
 * @author Chris Hokamp
 */
//TODO: remove abstract
class MemoryInvertedIndexStore
  extends MemoryStore
  with InvertedIndexStore
  with DocFrequencyStore {

  val docFreq = new mutable.HashMap[Int, Int]
  val index = new mutable.HashMap[Int, mutable.HashMap[Int, Double]]

  def size =  docFreq.size

  def getDocFreq (token: Token):  Int = {
    val i = token.id
    docFreq.getOrElse(i, 1) //one to avoid div by zero
  }

  def getResources (token: Token): mutable.Map[Int, Double] = {

     val id = token.id
     val resSet= index.get(id)
     resSet match {
       case None => null
       case Some(x) => val vect = x
       vect
     }
  }

}
