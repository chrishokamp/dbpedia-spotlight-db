#!/bin/bash
#This script prepares a corpus (collection of docs) to
# be used for LSA disambiguation
#Run in the following manner: ./prepareLsa.sh /path/to/input/ /path/for/output <numDimensions> <numReducers>
#Example: ./prepareLsa.sh /home/chris/data/mahout_data/test_dir/ /home/chris/data/mahout_data/output 50 1
#Mahout Jobs run by this script:
#1 seqdirectory
#2 output of seqdirectory --> seq2sparse
#3 rowid
#4 ssvd
#5 transpose V (the term matrix)

input_dir=$1
output_dir=$2
dimensions=$3
reducers=$4

#we need an initial input dir and output dir
#Working  - this has been replaced by MahoutVectorUtil
#mahout seqdirectory --input $input_dir  --output $output_dir 

#output file's name is the input to seq2sparse
#mahout seq2sparse --input $input_dir --output $output_dir

#mahout rowid --input $output_dir/tfidf-vectors --output $output_dir

#this prepares V_t to become an inverted index of terms
mahout ssvd --rank $dimensions --vHalfSigma --reduceTasks $reducers --input $output_dir/matrix --output $output_dir

#The rest of this script is used for "pure LSA"
#mahout ssvd --rank $dimensions --reduceTasks $reducers --input $output_dir/matrix --output $output_dir
#Look at the dictionary file to get numCols (the token count)
#columns=$(mahout seqdumper -i $output_dir/dictionary.file-0 | grep "Count: " | sed 's/Count: //')

#Now we need to transpose the V matrix to prepare to fold in vectors
#mahout transpose --numRows $columns --numCols $dimensions --input $output_dir/V

#mahout outputs the transpose directory with a (seemingly random) int after it
#so, find the name of the transpose file 
#transpose_dir=$(ls $output_dir | grep transpose)
#echo the transpose dir is: $transpose_dir
#echo path to transpose file: $output_dir/$transpose_dir/part-00000

# the temp file may be left over
rm -r temp

