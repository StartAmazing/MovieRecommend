package com.ll.datacleaner

import com.ll.caseclass._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._
import org.apache.spark.sql._
import org.apache.spark.sql.hive._
import org.apache.spark.sql.SaveMode

object ETL {

  def main(args: Array[String]): Unit = {
    //本地模式运行
    val localClusterURL = "local[2]"
    //集群模式运行
    val clusterMasterURL = "spark://master:7077"
    val conf = new SparkConf().setAppName("ETL")
      .set("spark.testing.memory", "2147480000")
      .setMaster(localClusterURL)
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val hc = new HiveContext(sc)

    import sqlContext.implicits._

    //设置RDD的partition的数量一般以集群分配给CPU核数的整数倍为宜
    val minPartitions = 12
    //通过case class来定义Links的数据结构，数据的schema

    val links: DataFrame = sc.textFile("hdfs://master:9000/MovieRecommendData/data/links.txt", minPartitions)
      .filter(!_.endsWith(","))
      .map(x => {
        val link: Array[String] = x.split(",")
        val links = Links(link(0).trim().toInt, link(1).trim().toInt, link(2).trim().toInt)
        links
      })
      .toDF()

    //写入到数据仓库（用hive构建的）中
    links.write.mode(SaveMode.Overwrite).parquet("hdfs://master:9000/MovieRecommendData/tmp/links")
    hc.sql("drop table if exists links")

    hc.sql("create table if not exists links(movieId int,imdId int,tmdId int) stored as parquet")

    hc.sql("load data inpath 'hdfs://master:9000/MovieRecommendData/tmp/links' overwrite into table links")


  }
}
