package org.dbpedia.spotlight.db.io

import java.io.{InputStream, FileInputStream, File}
import io.Source
import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import org.dbpedia.spotlight.db.WikipediaToDBpediaClosure
import org.dbpedia.spotlight.db.model.{ResourceStore, TokenStore}
import org.apache.commons.logging.LogFactory
import scala.Predef._
import scala.Array
import org.dbpedia.spotlight.exceptions.{DBpediaResourceNotFoundException, NotADBpediaResourceException}

import net.liftweb.json._



import java.lang.System
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

//import collection.immutable.HashMap

import collection.mutable.HashMap


/**
 * @author Joachim Daiber
 * @author Chris Hokamp - added lift-json parsing code for ESA disambiguation
 *
 *
 *
 */

object TokenOccurrenceSource {

  private val LOG = LogFactory.getLog(this.getClass)
  /*
  def fromJsonInputStream (tokenInputStream: InputStream, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore): Iterator[Triple[DBpediaResource, Array[Token], Array[Double]]] = {
    var i =0
    jsonTokenOccurrenceSource(tokenInputStream) map {
      case (wikiurl: String, tokens: Array[String], weights: Array[Int]) => {  }
    }
  */

  def fromJsonFile(tokenFile: File, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore) = {
    var input: InputStream = new FileInputStream(tokenFile)
    if (tokenFile.getName.endsWith(".bz2")) {
        input = new BZip2CompressorInputStream(input)
    }
    fromJsonInputStream(input, tokenStore, wikipediaToDBpediaClosure, resStore)
  }




  def jsonTokenOccurenceSource (jsonInputStream: InputStream): Iterator[Triple[String, Array[String], Array[Double]]] = {

    Source.fromInputStream(jsonInputStream) getLines() filter(!_.equals("")) map {
        line: String => {
          val json = parse(line)
          implicit val formats = DefaultFormats

          val wikiurl = (json \ "uri").extract[String]

          var tokens = Array[String]()
          var weights = Array[Double]()

          val elements: List[JObject] = (json \ "sorted").extract[List[JObject]]
          elements.foreach {
            (instance: JObject) => {
                val tok: Pair[String, Double] = getValues(instance)
                //println(tok._1)
                //println(tok._2)
                tokens :+= tok._1
                weights :+= tok._2
                //val token: String = tok._1
                //val weight: Double = tok._2
            }
          }
          Triple(wikiurl, tokens, weights)
        }
    }
  }


  def getValues (instance: JObject): Pair[String, Double] = {
    implicit val formats = DefaultFormats
    val tok = (instance \ "token").extract[String]
    val weight = (instance \ "weight").extract[Double]
    //println("instance="+instance)
    //println("token=" + tok)
    //println("weight="+weight)
    val pair = (tok, weight)
    pair
  }

  def fromJsonInputStream(tokenInputStream: InputStream, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore): Iterator[Triple[DBpediaResource, Array[Token], Array[Double]]] = {
    var i =0
    jsonTokenOccurenceSource((tokenInputStream)) map {
      case (wikiurl: String, tokens: Array[String], weights: Array[Double]) => {
        i += 1
        if (i % 10000 == 0)
          LOG.info("Read context for %d resources...".format(i))
        try {
          Triple(
            resStore.getResourceByName(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)),
            tokens.map{ token => tokenStore.getToken(token) },
            weights
          )
        } catch {
          case e: DBpediaResourceNotFoundException => Triple(null, null, null)
          case e: NotADBpediaResourceException     => Triple(null, null, null)
        }
      }
    }
  }

  def fromPigInputStream(tokenInputStream: InputStream, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore): Iterator[Triple[DBpediaResource, Array[Token], Array[Int]]] = {

    var i = 0
    plainTokenOccurrenceSource(tokenInputStream) map {
      case (wikiurl: String, tokens: Array[String], counts: Array[Int]) => {
        i += 1
        if (i % 10000 == 0)
          LOG.info("Read context for %d resources...".format(i))
        try {
          Triple(
            resStore.getResourceByName(wikipediaToDBpediaClosure.wikipediaToDBpediaURI(wikiurl)),
            tokens.map{ token => tokenStore.getToken(token) },
            counts
          )
        } catch {
          case e: DBpediaResourceNotFoundException => Triple(null, null, null)
          case e: NotADBpediaResourceException     => Triple(null, null, null)
        }
      }
    }

  }

  def fromPigFile(tokenFile: File, tokenStore: TokenStore, wikipediaToDBpediaClosure: WikipediaToDBpediaClosure, resStore: ResourceStore) = fromPigInputStream(new FileInputStream(tokenFile), tokenStore, wikipediaToDBpediaClosure, resStore)
  def plainTokenOccurrenceSource(tokenInputStream: InputStream): Iterator[Triple[String, Array[String], Array[Int]]] = {
    Source.fromInputStream(tokenInputStream) getLines() filter(!_.equals("")) map {
      line: String => {
        val Array(wikiurl, tokens) = line.trim().split('\t')
        var tokensA = Array[String]()
        var countsA = Array[Int]()

        tokens.tail.init.split("(\\[\"|\",|\\])").filter(pair => !pair.equals(",") && !pair.equals("")).grouped(2).foreach {
          case Array(a, b) => {
            tokensA :+= a
            countsA :+= b.toInt
          }
          print(".")
        }
        Triple(wikiurl, tokensA, countsA)
      }
    }
  }

  //Chris: temporarily changed to parse PigStorage output correctly - this is totally hackish at this point
  def pigStorageOccurrenceSource(tokenInputStream: InputStream): Iterator[Triple[String, Array[String], Array[Int]]] = {
    Source.fromInputStream(tokenInputStream) getLines() filter(x => !x.equals("") && !x.contains("{}") && !x.contains("2,3,7,4")) map {
      line: String => {
        //TEST
        //System.out.println("line is: " +line)
        val Array(wikiurl, tokens) = line.trim().split('\t')
        //System.out.println("line is: " + wikiurl)
        //System.out.println("line is: " + tokens)

        var tokensA = Array[String]()
        var countsA = Array[Int]()

        //var tempMap = new HashMap[String, Int]

        //parsing for PigStorage output: Example: http://en.wikipedia.org/wiki/Bishti {(kimik,17),(grupi,16),...}
        val sub = tokens.substring(2, tokens.length()-2)
        //System.out.println("sub is: " + sub)
        val tokAndCount: Array[String] = sub.split("\\),\\(");

        tokAndCount.foreach {
          case (a) => {
               //System.out.println("a is: " + a)

               //to handle tokens containing commas
               //val Array(t, c) = a.split(",")
               val i  = a.lastIndexOf(",")
               val t = a.substring(0, i)
               val c =  a.substring(i+1)

               tokensA :+= t
               //System.out.println("t is: " + t)
               countsA :+= c.toInt
               //System.out.println("c is: " + c)

          }
        }


        /*
        var tokensA = Array[String]()
        var countsA = Array[Int]()

        tokens.tail.init.split("(\\[\"|\",|\\])").filter(pair => !pair.equals(",") && !pair.equals("")).grouped(2).foreach {
          case Array(a, b) => {
            tokensA :+= a
            countsA :+= b.toInt
          }
          print(".")
        }
        */
        Triple(wikiurl, tokensA, countsA)
      }


    }
  }

}
