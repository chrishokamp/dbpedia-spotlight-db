package org.dbpedia.spotlight.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Chris Hokamp
 *
 * This class represents the corpus information: term-->colIndex, and term-->docFrequency
 */

public class CorpusData {
    HashMap<String, Integer> columnIndex;
    HashMap<Integer, Integer> docFrequency;
    int docCount;
    int termCount = 0;

    public CorpusData(String dictionaryFileLocation, String frequencyFileLocation, int docs) {
        columnIndex = buildColumnIndex(dictionaryFileLocation);
        docFrequency = buildDocFrequencyIndex (frequencyFileLocation);
        docCount = docs;
    }

    public boolean containsTerm (String term) {
        if (columnIndex.containsKey(term)) {
            return true;
        }
        else {
            return false;
        }
    }

    public int returnColumn (String term) {
        if (columnIndex.containsKey(term)) {
            return columnIndex.get(term).intValue();
        }
        else {
            return -1;
        }
    }

    public int returnDocFreq (Integer index) {
        if (docFrequency.containsKey(index)) {
            return docFrequency.get(index).intValue();
        }
        else {
            return -1;
        }
    }

    public int returnDocFreq (String term) {
        if (columnIndex.containsKey(term)) {
            Integer index = columnIndex.get(term);
            return docFrequency.get(index).intValue();
        }
        else {
            return -1;
        }
    }

    public int docCount () {
        return docCount;
    }

    //This is specifically for working with Mahout output
    private HashMap<String, Integer> buildColumnIndex (String location) {
        File input = new File(location);
        HashMap<String, Integer> cols = new HashMap<String, Integer>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            cols = new HashMap<String, Integer>();
            String line;

            while ((line = reader.readLine()) != null) {
                termCount++; //increment cardinality
                String [] parts = line.split(" ");
                String untrimmed = parts[1];
                String key = untrimmed.substring(0,untrimmed.length()-1);
                //System.out.println("key is: " + key);
                Integer val = Integer.parseInt(parts[3].substring(0)); //cut off beginning whitespace


                //System.out.println("val is: " + val);
                cols.put(key, val);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return cols;
    }

    public int cardinality () {
        return termCount;
    }


    //This is specifically for working with Mahout output
    private HashMap<Integer, Integer> buildDocFrequencyIndex (String location) {
        File input = new File(location);
        HashMap<Integer, Integer> docFrequency = new HashMap<Integer, Integer>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                String [] parts = line.split(" ");
                String untrimmed = parts[1];
                Integer key = Integer.parseInt(untrimmed.substring(0,untrimmed.length()-1));

                Integer val = Integer.parseInt(parts[3].substring(0)); //cut off beginning whitespace

                //System.out.println("key is: " + key);
                //System.out.println("val is: " + val);
                docFrequency.put(key, val);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return docFrequency;
    }



}
