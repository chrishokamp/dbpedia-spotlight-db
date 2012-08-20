package org.dbpedia.spotlight.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.utils.io.ChunkedWriter;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.string.ModifiedWikiUtil;

import java.io.*;
import java.util.HashMap;

/**
 * @author Chris Hokamp
 *
 * Utility to convert DBpedia-Spotlight SurfaceFormOccurences to Hadoop sequence files
 *
 * Everything is currently hard-coded inside this class
 */
public class MahoutVectorUtil {

    private static final String outputFile = "seqFiles";
    private static final int DEFAULT_CHUNK_SIZE = 1984;
    //private static final int DEFAULT_CHUNK_SIZE = 3968;


    public static void saveSfOccAsSeqFile (SurfaceFormOccurrence sfOcc, Configuration conf, FileSystem fs, Path output) throws IOException{

        //get the text and URI name for the sfOcc
        //written out for clarity
        String sf = sfOcc.surfaceForm().name();
        String context = sfOcc.context().text();
        Text key = new Text(sf);
        Text value = new Text(context);

        //Path outputPath = new Path(output, outputDir);
        SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf,output, Text.class, Text.class);

        writer.append(key, value);
        writer.close();

    }


    //Save the query as a tfidf vector using patched version of mahout installed
    //on the local system
    public static String saveVectors (Path pathToVector) throws IOException, InterruptedException {

          //TEST
        File directory = new File("");

          //File directory = new File(".");
          String pathToSeq = pathToVector.toString();
          System.out.println("path to vector is: " + pathToSeq);
          //String pathToScript = directory.getAbsolutePath().toString()+"/bin/lsaQueryVectors.sh";
          String pathToDictionary = "/home/chris/data/mahout_data/seq2sparse_output/dictionary.file-0";
          String pathToFreq = "/home/chris/data/mahout_data/seq2sparse_output";
          String outputPath = directory.getAbsolutePath().toString()+"/queryTfidf";
          System.out.println("output path is: " + outputPath);
          String fullCommand = "/home/chris/projects/dbpedia-spotlight/bin/lsaQueryVectors.sh " + pathToSeq+ " " + outputPath + " "  + pathToDictionary + " " + pathToFreq;
          System.out.println ("fullCommand: " +fullCommand);
          Process p1 = Runtime.getRuntime().exec(fullCommand);

          p1.waitFor();

          //when this finishes, the vector should be ready in outputPath/tfidf-vectors/part-r-00000
          //TODO: test
          System.out.println("the save vectors method just finished...");
          return outputPath;

    }

    //this method creates a mahout vector from a HashMap where keys are column indexes and Values are tfidf weights

    public static Vector mahoutVectorFromString (String text, CorpusData thisCorpus) throws IOException {
        //(1) tokenize with LuceneAnalyzer
        //(2) calculate tfidf for each (non-zero) cell using CorpusData object
        //(3) create mahout vector using column index and tfidf value

        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
        String field = "token";
        TokenStream stream = analyzer.tokenStream(field, new StringReader(text));

        //First pass through the doc to get the termFreqs
        HashMap<String, Integer> termFreq = new HashMap<String, Integer>();
        while (stream.incrementToken()) {
            String token = stream.getAttribute(CharTermAttribute.class).toString();
            if (termFreq.containsKey(token)) {
                Integer count = termFreq.get(token)+1;
                termFreq.put(token, count);
            }
            else {
                termFreq.put(token, 1);
            }
        }

        Vector newVector = new RandomAccessSparseVector(thisCorpus.cardinality());
        for (String key : termFreq.keySet()) {

            if (thisCorpus.containsTerm(key)) {
                int index = thisCorpus.returnColumn(key);
                //System.out.println("key: " +key + " non-zero col: " + index);
                double docFreq = (double)thisCorpus.returnDocFreq(key);
                double termFrequency = (double)termFreq.get(key);
                double totalDocs = (double)thisCorpus.docCount();
                double tfidf = (Math.log(totalDocs/docFreq))*termFrequency;
                newVector.setQuick(index, tfidf);
            }
        }
        return newVector;

    }

    //create a mahout sequence file from the TSV file of aggregated occurences - bzip is not currently working
    public static void sequenceFileFromTSV (String tsvFileLocation, String outputLocation) throws IOException {
    //(1) load the file with an Input stream (buffered), and decompress
    //(2) ensure that the key class and value classes are the same as the input to seq2sparse (the output of seqdirectory)
    //Note: see ChunkedWriter in Mahout - org.apache.mahout.utils.io.ChunkedWriter
    //Key: Text Value: Text
    //Hadoop Classes:
        //Path input = new Path (tsvFileLocation);
        Path output = new Path(outputLocation);
        Configuration conf = new Configuration();
        ChunkedWriter writer = new ChunkedWriter(conf, DEFAULT_CHUNK_SIZE, output);

        //unzip the file and read line-by-line, passing key, value to ChunkedWriter
        FileInputStream in = new FileInputStream(tsvFileLocation);

        //BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
        //BufferedInputStream buffered = new BufferedInputStream(in);

        InputStreamReader charReader = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(charReader);

        String line = null;
        String key = null;
        String context = null;
        //int i = 0;
        while ((line = reader.readLine()) != null) {
            //read the line and split into URI and context (key and value)
            String[] keyVal = line.split("\\t");
            //Testing
            //if (keyVal.length==2) {
                key = keyVal[0];
            //TEST
            System.out.println("the key is: " + key);

            String [] nodes = key.split("/");
            String resourceName = nodes[4];

            //TEST
            resourceName = ModifiedWikiUtil.wikiEncode(resourceName);
             //END TEST
            context =  keyVal[1];
            writer.write(resourceName, context);


        }
        reader.close();
        writer.close();

    }
    //So that we can run this class from command line
    public static void main(String [] args) throws IOException {
        String inputTsvLocation = args[0];
        String outputLocation = args[1];
        sequenceFileFromTSV(inputTsvLocation, outputLocation);
    }

}
