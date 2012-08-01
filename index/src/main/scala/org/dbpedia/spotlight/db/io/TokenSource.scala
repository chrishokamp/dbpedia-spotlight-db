package org.dbpedia.spotlight.db.io

import org.dbpedia.spotlight.io.OccurrenceSource
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, Token}
import org.dbpedia.spotlight.db.model.Tokenizer
import collection.mutable.HashMap
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.{InputStream, FileInputStream, File}
import org.apache.commons.logging.LogFactory
import java.util

import scala.Array


/**
 * @author Joachim Daiber, Chris Hokamp - additions to facilitate tfidf indexing
 *
 *
 **/

object TokenSource {

  private val LOG = LogFactory.getLog(this.getClass)

  def fromOccurrenceSource(os: OccurrenceSource, tokenizer: Tokenizer): java.util.Map[Token, Int] = {
    val tokenMap = HashMap[String, Int]()

    os.foreach {
      occ: DBpediaResourceOccurrence => {
        tokenizer.tokenize(occ.context) foreach {
          token: String => tokenMap.put(token, tokenMap.getOrElse(token, 0) + 1)
        }
      }
    }

    var id = -1
    tokenMap.map{
      case(token, count) => {
        id += 1
        (new Token(id, token, count), count)
      }
    }.toMap.asJava
  }

  def fromPigFile(tokenFile: File) = fromPigInputStream(new FileInputStream(tokenFile))
  def fromPigInputStream(tokenFile: InputStream) = {

    val tokenMap = HashMap[String, Int]()

    var i = 0
    TokenOccurrenceSource.plainTokenOccurrenceSource(tokenFile) foreach {
      p: Triple[String, Array[String], Array[Int]] => {

        i += 1
        if (i % 10000 == 0)
          LOG.info("Read context for %d resources...".format(i))

        (0 to p._2.size -1).foreach {
          i: Int => tokenMap.put(p._2(i), tokenMap.getOrElse(p._2(i), 0) + p._3(i))
        }
      }
    }

    var id = -1
    tokenMap.map{
      case(token, count) => {
        id += 1
        (new Token(id, token, count), count)
      }
    }.toMap.asJava

  }


  def dfFromPigFile(tokenFile: File) = fromPigInputStreamWithDocFreq(new FileInputStream(tokenFile))
  //In this method, count is the docFreq in the corpus
  def fromPigInputStreamWithDocFreq (tokenFile: InputStream): java.util.Map[Token, Int] = {
    //val tokenMap = HashMap[String, Int]()

    val docFreq = HashMap[String, Int]()

    var i = 0
    TokenOccurrenceSource.plainTokenOccurrenceSource(tokenFile) foreach {
      p: Triple[String, Array[String], Array[Int]] => {

        val docTokens = p._2.toSet

        i += 1
        if (i % 10000 == 0)
          LOG.info("Read context for %d resources...".format(i))
        /*
        (0 to p._2.size -1).foreach {
          i: Int => tokenMap.put(p._2(i), tokenMap.getOrElse(p._2(i), 0) + p._3(i))
        }
        */
        docTokens.foreach {
          tok: String => docFreq.put(tok, docFreq.getOrElse(tok, 0)+1)
        }
      }
      /*
      //TEST
      System.out.println("testing docFreq...")
      docFreq.map {
        case(token, count) => {
          if (token.equals("and") || token.equals("chris")) {
            System.out.println("token is: " + token + " freq is: " + count)
          }
        }
      }
      */
    }

    var id = -1
    docFreq.map{
      case(token, count) => {
        id += 1
        (new Token(id, token, count), count.toInt)
      }
    }.toMap.asJava

  }


}
