package cluster_builder

import com.github.tototoshi.csv._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.PCA
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.udf

object ClusterBuilder {
  private val headValue = udf((arr: org.apache.spark.ml.linalg.Vector) => arr.toArray(0))
  private val tailValue = udf((arr: org.apache.spark.ml.linalg.Vector) => arr.toArray(1))

  def build(df: DataFrame, optimalNumOfClusters: Int, _type: String, columns: Seq[String]): (DataFrame, DataFrame) = {
    val kMeans = new KMeans().setK(optimalNumOfClusters).setSeed(1L)
    val model = kMeans.fit(df)
    val clusterCenters = model.clusterCenters
    var writer = CSVWriter.open(s"src/main/resources/centers/${_type}_centers.csv", append = false)
    writer.writeRow(columns)
    writer = CSVWriter.open(s"src/main/resources/centers/${_type}_centers.csv", append = true)
    for (i <- clusterCenters.indices) {
      println(s"cluster:: cluster-$i, ${clusterCenters(i)}")
      writer.writeRow(clusterCenters(i).toArray)
    }
    val predictions = model.transform(df)
    // pca
    val pca = new PCA().setInputCol("features").setOutputCol("pca_features").setK(2)
    val pcaModel = pca.fit(predictions)
    val pcaResult = pcaModel.transform(predictions)
    println(pcaResult.columns.mkString("Array(", ", ", ")"))
    pcaResult.printSchema()


    val plotData = pcaResult.select("Player", "pca_features", "prediction")
      .withColumn("x", headValue(pcaResult("pca_features")))
      .withColumn("y", tailValue(pcaResult("pca_features")))
      .select("Player", "x", "y", "prediction")
    plotData.show(truncate = false)
    println("cluster:: end")
    (predictions, plotData)
  }
}
