package com.example.chapter5

import org.apache.spark.{SparkConf, SparkContext}

object WordCountTop3 {
  def main(args:Array[String])= {
    require(args.length >=1, "Please give the file directory")
    val conf = new SparkConf
    val sc = new SparkContext(conf)
    try {
      val filePath = args(0)
      val wordAndCountRDD = sc.textFile(filePath)
        .flatMap(_.split("[ ,.]"))
        .filter(_.matches("""\p{Alnum}+"""))
        .map((_, 1))
        .reduceByKey(_ + _)
      
      val top3Words = wordAndCountRDD.map {
        case (word, count) => (count, word)
      }.sortByKey(false).map {
        case (count, word) => (word, count)
      }.take(3)
      top3Words.foreach(println)
    } finally {
      sc.stop()
    }
  }
}

