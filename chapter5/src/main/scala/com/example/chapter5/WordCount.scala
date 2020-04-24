package com.example.chapter5

import org.apache.spark.{SparkConf, SparkContext}

object WordCount {
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
      wordAndCountRDD.collect.foreach(println)
    } finally {
      sc.stop()
    }
  }
}
