(ns dl4clj.clustering.strategy.strategies
  (:import [org.deeplearning4j.clustering.algorithm.strategy FixedClusterCountStrategy
            OptimisationStrategy])
  (:require [dl4clj.utils :refer [generic-dispatching-fn]]))

(defmulti clustering-strategy generic-dispatching-fn)

(defmethod clustering-strategy :fixed-cluster-count [opts]
  (let [conf (:fixed-cluster-count opts)
        {cluster-count :cluster-count
         distance-fn :distance-fn} conf]
    (FixedClusterCountStrategy/setup cluster-count distance-fn)))

(defmethod clustering-strategy :optimization [opts]
  (let [conf (:optimization opts)
        {cluster-count :cluster-count
         distance-fn :distance-fn} conf]
    (OptimisationStrategy/setup cluster-count distance-fn)))

(defn new-fixed-cluster-count-strategy
  ""
  [& {:keys [cluster-count distance-fn]
      :as opts}]
  (clustering-strategy {:fixed-cluster-count opts}))

(defn new-optimization-strategy
  ""
  [& {:keys [cluster-count distance-fn]
      :as opts}]
  (clustering-strategy {:optimization opts}))
