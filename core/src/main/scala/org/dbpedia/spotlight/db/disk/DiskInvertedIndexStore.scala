package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.db.model.InvertedIndexStore
import org.dbpedia.spotlight.model.Token
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException



/**
 * @author Chris Hokamp
 */

class DiskInvertedIndexStore(file: String) /*extends InvertedIndexStore*/ {

  val jdbm = new JDBMStore[Int, Map[Int, Double]](file)

  def getResources (token: Token): Map[Int,Double] = {
      //TEST
      //println("the token is: " +token.name)
      try {
        val resources = jdbm.get(token.id)
        if (resources == null) {
          null
        }
        resources
      } catch {
        case e: NoSuchFieldError => null
      }
  }

  def getDocFreq (token: Token): Int = {
    val resources = jdbm.get(token.id)
    if (resources == null) {
      0
    } else {
      resources.size
    }
  }

    //TEST - TODO: to fix, add try/catch like in getResources above
    def printAll(i: Int) {
      val testMap = jdbm.get(i)
      if (testMap != null) {
        testMap.foreach {
          case null =>
          case (k: Int, v: Double) => {
            println ("k is : " +k + " v is: " + v)
         }
        }
      }
    }

}
