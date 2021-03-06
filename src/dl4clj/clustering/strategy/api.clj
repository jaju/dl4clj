(ns dl4clj.clustering.strategy.api
  (:import [org.deeplearning4j.clustering.algorithm.strategy
            BaseClusteringStrategy FixedClusterCountStrategy
            OptimisationStrategy]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; getters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-distance-fn
  [clustering-strat]
  (.getDistanceFunction clustering-strat))

(defn get-initial-cluster-count
  [clustering-strat]
  (.getInitialClusterCount clustering-strat))

(defn get-optimization-phase-condition
  [clustering-strat]
  (.getOptimizationPhaseCondition clustering-strat))

(defn get-termination-condition
  [clustering-strat]
  (.getTerminationCondition clustering-strat))

(defn get-clustering-strategy-type
  [clustering-strat]
  (.getType clustering-strat))

(defn get-clustering-optimization-val
  ;; only for optimization strat
  [clustering-optim-strat]
  (.getClusteringOptimizationValue clustering-optim-strat))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; checkers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn allow-empty-clusters?
  [clustering-strat]
  (.isAllowEmptyClusters clustering-strat))

(defn strategy-of-this-type?
  [& {:keys [clustering-strat clustering-strat-type]}]
  (.isStrategyOfType clustering-strat clustering-strat-type))

(defn optimization-applicable-now?
  [& {:keys [clustering-strat iteration-history]}]
  (.isOptimizationApplicableNow clustering-strat iteration-history))

(defn optimization-defined?
  [clustering-strat]
  (.isOptimizationDefined clustering-strat))

(defn clustering-optimization-type?
  [& {:keys [clustering-optim optim-type]}]
  (.isClusteringOptimizationType clustering-optim optim-type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; setters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-initial-cluster-count!
  [& {:keys [base-cluster-strat cluster-count]}]
  (doto base-cluster-strat (.setInitialClusterCount cluster-count)))

(defn set-distance-fn!
  [& {:keys [clustering-strat distance-fn]}]
  (doto clustering-strat (.setDistanceFunction distance-fn)))

(defn set-allow-empty-clusters!
  [& {:keys [clustering-strat allow?]}]
  (doto clustering-strat (.setAllowEmptyClusters allow?)))

(defn end-when-dist-variation-rate-less-than!
  "sets the termination condition for the clustering strat using a
  convergence condition created from the supplied rate"
  [& {:keys [clustering-strat rate]}]
  (.endWhenDistributionVariationRateLessThan clustering-strat rate))

(defn end-when-iteration-count-equals!
  "sets the termination condition for the clustering strat using a
  fixed iteration count condition created using the suplied n"
  [& {:keys [clustering-strat n]}]
  (.endWhenIterationCountEquals clustering-strat n))

(defn set-optimization-phase-condition!
  ;; protected, dont think i will be able to call this method, but here just in case
  [& {:keys [clustering-strat optimization-phase-condition]}]
  (doto clustering-strat (.setOptimizationPhaseCondition optimization-phase-condition)))

(defn set-termination-condition!
  ;; protected, dont think i will be able to call this method, but here just in case
  [& {:keys [clustering-strat termination-condition]}]
  (doto clustering-strat (.setTerminationCondition termination-condition)))

(defn set-clustering-strat-type!
  ;; protected, dont think i will be able to call this method, but here just in case
  [& {:keys [clustering-strat clustering-strat-type]}]
  (doto clustering-strat (.setType clustering-strat-type)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; optimization strategy setters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-clustering-optimisation!
  "sets the clustering optimization based on the supplied type and value"
  [& {:keys [clustering-optim-strat optim-type value]}]
  (.optimize clustering-optim-strat optim-type value))

(defn optimize-when-iteration-count-multiple-of!
  "sets the clustering optimisation application condition to be a fixed iteration count condition"
  [& {:keys [clustering-optim-strat multiple]}]
  (.optimizeWhenIterationCountMultipleOf clustering-optim-strat multiple))

(defn optimize-when-point-dist-variation-rate-less-than!
  "sets the clustering optimisation application condition to be a convergence condition based on the variation rate"
  [& {:keys [clustering-optim-strat rate]}]
  (.optimizeWhenPointDistributionVariationRateLessThan clustering-optim-strat rate))
