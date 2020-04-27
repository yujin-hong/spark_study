package com.example.chapter5

import scala.collection.mutable.{HashMap, Map}
import java.io.{BufferedReader, InputStreamReader, Reader}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD



object BestSellerFinder {
  private def createSalesRDD(csvFile: String, sc: SparkContext) = {
    val logRDD = sc.textFile(csvFile)
    logRDD.map { record =>
      val splitRecord = record.split(",")
      val productId = splitRecord(2)
      val numOfSold = splitRecord(3).toInt
      (productId, numOfSold)
    }
  }

  private def createOver50SoldRDD(rdd: RDD[(String, Int)]) = {
    rdd.reduceByKey(_+_).filter(_._2>=50)
  }

  private def loadCSVIntoMap(productsCSVFile: String) = {
    var productsCSVReader: Reader = null

    try {
      val productsMap = new HashMap[String, (String, Int)]
      val hadoopConf = new Configuration()
      val fileSystem = FileSystem.get(hadoopConf)
      val inputStream = fileSystem.open(new Path(productsCSVFile))
      val productsCSVReader = new BufferedReader(new InputStreamReader(inputStream))
      var line = productsCSVReader.readLine

      while (line != null) {
        val splitLine = line.split(",")
        val productId = splitLine(0)
        val productName = splitLine(1)
        val unitPrice = splitLine(2).toInt
        productsMap(productId) = (productName, unitPrice)
        line = productsCSVReader.readLine
      }
      productsMap
    } finally {
      if(productsCSVReader != null) {
        productsCSVReader.close()
      }
    }
  }

  private def createResultRDD(
                             broadcastedMap: Broadcast[_ <: Map[String, (String, Int)]],
                             rdd: RDD[(String, Int)]
                             ) = {
    rdd.map {
      case(productId, amount) =>
        val productsMap = broadcastedMap.value
        val (productsName, unitPrice) = productsMap(productId)
        (productsName, amount, amount*unitPrice)
    }
  }

  def main(args: Array[String]) = {
    require(
      args.length>=4,
      "give four address"
    )

    val conf = new SparkConf
    val sc = new SparkContext(conf)

    try {
      val Array(salesCSVFile1, salesCSVFile2, productsCSVFile, outputPath) = args.take(4)

      val salesRDD1 = createSalesRDD(salesCSVFile1, sc)
      val salesRDD2 = createSalesRDD(salesCSVFile2, sc)

      val over50SoldRDD1 = createOver50SoldRDD(salesRDD1)
      val over50SoldRDD2 = createOver50SoldRDD(salesRDD2)

      val bothRDD = over50SoldRDD1.join(over50SoldRDD2)
      val sumRDD = bothRDD.map {
        case (id, (num1, num2)) =>
          (id, num1 + num2)
      }

      val productsMap = loadCSVIntoMap(productsCSVFile)
      val broadcastedMap = sc.broadcast(productsMap)

      val resultRDD = createResultRDD(broadcastedMap, sumRDD)
      resultRDD.saveAsTextFile(outputPath)
    } finally {
      sc.stop()
    }
  }
}

