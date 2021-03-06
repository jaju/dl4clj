(ns ^{:doc "see http://deeplearning4j.org/doc/org/deeplearning4j/nn/conf/NeuralNetConfiguration.html"}
  dl4clj.nn.api.nn-conf
  (:import [org.deeplearning4j.nn.conf NeuralNetConfiguration])
  (:require [clojure.core.match :refer [match]]
            [dl4clj.utils :refer [obj-or-code?]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; getters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-l1-by-param
  [& {:keys [nn-conf var-name as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :var-name (:or (_ :guard string?)
                          (_ :guard seq?))}]
         (obj-or-code? as-code? `(.getL1ByParam ~nn-conf ~var-name))
         :else
         (.getL1ByParam nn-conf var-name)))

(defn get-l2-by-param
  [& {:keys [nn-conf var-name as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :var-name (:or (_ :guard string?)
                          (_ :guard seq?))}]
         (obj-or-code? as-code? `(.getL2ByParam ~nn-conf ~var-name))
         :else
         (.getL2ByParam nn-conf var-name)))

(defn get-learning-rate-by-param
  [& {:keys [nn-conf var-name as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :var-name (:or (_ :guard string?)
                          (_ :guard seq?))}]
         (obj-or-code? as-code? `(.getLearningRateByParam ~nn-conf ~var-name))
         :else
         (.getLearningRateByParam nn-conf var-name)))

(defn list-variables
  [& {:keys [nn-conf copy? as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :copy? (:or (_ :guard boolean?)
                       (_ :guard seq?))}]
         (obj-or-code? as-code? `(.variables ~nn-conf ~copy?))
         [{:nn-conf _
           :copy? _}]
         (.variables nn-conf copy?)
         [{:nn-conf (_ :guard seq?)}]
         (obj-or-code? as-code? `(.variables ~nn-conf))
         :else
         (.variables nn-conf)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; setters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-layer-param-lr!
  "sets the layer learning rate for the variable and returns the nn-conf"
  [& {:keys [nn-conf var-name as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :var-name (:or (_ :guard string?)
                          (_ :guard seq?))}]
         (obj-or-code? as-code? `(doto ~nn-conf (.setLayerParamLR ~var-name)))
         :else
         (doto nn-conf (.setLayerParamLR var-name))))

(defn set-learning-rate-by-param!
  ":rate (double), the learning rate for the variable supplied

  returns the nn-conf"
  [& {:keys [nn-conf var-name rate as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :var-name (:or (_ :guard string?)
                          (_ :guard seq?))
           :rate (:or (_ :guard number?)
                      (_ :guard seq?))}]
         (obj-or-code?
          as-code?
          `(doto ~nn-conf (.setLearningRateByParam ~var-name (double ~rate))))
         :else
         (doto nn-conf (.setLearningRateByParam var-name rate))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-variable!
  ":var-name (str), the name of the variable you want to add to the nn-conf"
  ;; i think this needs an update, see the nn-tests for confirmation
  [& {:keys [nn-conf var-name as-code?]
      :or {as-code? true}
      :as opts}]
  (match [opts]
         [{:nn-conf (_ :guard seq?)
           :var-name (:or (_ :guard string?)
                          (_ :guard seq?))}]
         (obj-or-code? as-code? `(doto ~nn-conf (.addVariable ~var-name)))
         :else
         (doto nn-conf (.addVariable var-name))))

(defn clear-variables!
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(doto ~nn-conf .clearVariables))
         :else
         (doto nn-conf .clearVariables)))

(defn from-json
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(NeuralNetConfiguration/fromJson ~nn-conf))
         :else
         (NeuralNetConfiguration/fromJson nn-conf)))

(defn from-yaml
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(NeuralNetConfiguration/fromYaml ~nn-conf))
         :else
         (NeuralNetConfiguration/fromYaml nn-conf)))

(defn mapper
  "Object mapper for serialization of configurations"
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(.mapper ~nn-conf))
         :else
         (.mapper nn-conf)))

(defn mapper-yaml
  "Object mapper for serialization of configurations"
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(.mapperYaml ~nn-conf))
         :else
         (.mapperYaml nn-conf)))

#_(defn reinit-mapper-with-subtypes
  "Reinitialize and return the Jackson/json ObjectMapper with additional named types.

  typez (coll), a collection of json named types"
  [& {:keys [typez as-code?]
      :or {as-code? true}}]
  (match [typez]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(.reinitMapperWithSubtypes ~typez))
         :else
         (.reinitMapperWithSubtypes typez)))

(defn to-json
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(.toJson ~nn-conf))
         :else
         (.toJson nn-conf)))

(defn build-nn
  [nn-conf & {:keys [as-code?]
              :or {as-code? true}}]
  (match [nn-conf]
         [(_ :guard seq?)]
         (obj-or-code? as-code? `(.build ~nn-conf))
         :else
         (.build nn-conf)))
