(ns ^{:doc "see: https://deeplearning4j.org/doc/org/deeplearning4j/spark/impl/multilayer/SparkDl4jMultiLayer.html"}
    dl4clj.spark.dl4j-multi-layer
  (:import [org.deeplearning4j.spark.impl.multilayer SparkDl4jMultiLayer])
  (:require [dl4clj.utils :refer [contains-many? obj-or-code?]]
            [clojure.core.match :refer [match]]))

;; update to deaulting to code

(defn new-spark-multi-layer-network
  "Creates a multi layer network which can be trained via spark.

  :spark-context (sc), the spark context
   org.apache.spark.api.java.JavaSparkContext or org.apache.spark.SparkContext

  :mln (model or conf), this can be a multi-layer-config or a multi-layer-network
   - see: dl4clj.nn.multilayer.multi-layer-network and dl4clj.nn.conf.builders.multi-layer-builders"
  [& {:keys [spark-context mln training-master as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:spark-context (_ :guard seq?)
           :mln (_ :guard seq?)
           :training-master (_ :guard seq?)}]
         (obj-or-code? as-code? `(SparkDl4jMultiLayer. ~spark-context ~mln ~training-master))
         :else
         (SparkDl4jMultiLayer. spark-context mln training-master)))

(defn set-collect-training-stats?!
  "Set whether training statistics should be collected for debugging purposes.

  :collect? (boolean), if we collect or not

  returns the spark-mln"
  [& {:keys [spark-mln collect? as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:spark-mln (_ :guard seq?)
           :collect? (:or (_ :guard boolean?)
                          (_ :guard seq?))}]
         (obj-or-code? as-code? `(doto ~spark-mln (.setCollectTrainingStats ~collect?)))
         :else
         (doto spark-mln (.setCollectTrainingStats collect?))))
