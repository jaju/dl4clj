(ns dl4clj.nn-tests
  (:require [dl4clj.nn.conf.builders.layers :as layer]
            [dl4clj.nn.conf.builders.nn :as nn]
            [dl4clj.nn.conf.distributions :refer :all]
            [dl4clj.nn.conf.input-pre-processor :refer :all]
            [dl4clj.nn.conf.layers.input-type-util :refer :all]
            [dl4clj.nn.conf.layers.layer-validation :refer :all]
            [dl4clj.nn.conf.step-fns :refer :all]
            [dl4clj.nn.conf.variational.dist-builders :refer :all]
            [dl4clj.nn.conf.variational.dists-interface :refer :all]
            [dl4clj.nn.api.classifier :as classifier]
            [dl4clj.nn.api.distribution :as dist]
            [dl4clj.nn.api.input-pre-processors :as ipp]
            [dl4clj.nn.api.layer :as layer-api]
            [dl4clj.nn.api.layer-specific-fns :as lsf]
            [dl4clj.nn.api.model :as model]
            [dl4clj.nn.api.multi-layer-conf :as multi-layer-conf]
            [dl4clj.nn.api.multi-layer-network :as mln]
            [dl4clj.nn.api.nn-conf :as nn-conf]
            [dl4clj.constants :refer :all]
            [dl4clj.nn.layers.layer-creation :as layer-creation]
            [dl4clj.nn.multilayer.multi-layer-network :as multi-layer-network]
            [dl4clj.nn.transfer-learning.fine-tune-conf :as fine-tune-conf]
            [dl4clj.nn.transfer-learning.helper :as tl-helper]
            [dl4clj.nn.transfer-learning.transfer-learning :as tl]

            [dl4clj.nn.updater.layer-updater :refer :all]
            [dl4clj.nn.updater.multi-layer-updater :refer :all]


            ;; possibly not core
            [dl4clj.nn.params.param-initializers :as param-init]
            [dl4clj.nn.gradient.default-gradient :as gradient]
            [dl4clj.nn.layers.layer-creation :as layer-creation]
            [dl4clj.nn.updater.layer-updater :as layer-updater]
            [dl4clj.nn.updater.multi-layer-updater :as mln-updater]


            ;; helper fns
            [dl4clj.utils :refer [array-of get-labels]]
            [nd4clj.linalg.factory.nd4j :refer [indarray-of-zeros]]
            [dl4clj.datasets.default-datasets :refer [new-mnist-ds]]
            [dl4clj.datasets.iterators :refer [new-mnist-data-set-iterator]]
            [dl4clj.datasets.api.datasets :refer [get-features get-example]]
            [dl4clj.eval.evaluation :refer [new-classification-evaler]]
            [dl4clj.eval.api.eval :refer [eval-classification! get-stats
                                          eval-model-whole-ds]]
            [clojure.test :refer :all])
  (:import [org.deeplearning4j.nn.conf NeuralNetConfiguration$Builder]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helper fn for layer creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; this might get replaced...
(defn quick-nn-conf
  [layer]
  (nn/builder :optimization-algo     :stochastic-gradient-descent
              :iterations            1
              :default-learning-rate 0.006
              :lr-policy-decay-rate  0.2
              :build?                true
              :as-code?              false
              :layers                layer))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Things required for making layers/nn-confs/multi-layer-confs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; distributions to sample weights from
;; dl4clj.nn.conf.distribution.distribution
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest distributions-test
  (testing "the creation of distributions"
    ;; uniform
    (is (= org.deeplearning4j.nn.conf.distribution.UniformDistribution
           (type (distribution {:uniform {:lower 0.2 :upper 0.4}}))))
    (is (= '(dl4clj.nn.conf.distributions/distribution {:uniform {:lower 0.2 :upper 0.4}})
           (new-uniform-distribution :lower 0.2 :upper 0.4)))
    ;; normal
    (is (= org.deeplearning4j.nn.conf.distribution.NormalDistribution
           (type (distribution {:normal {:mean 0 :std 1}}))))
    (is (= '(dl4clj.nn.conf.distributions/distribution {:normal {:mean 0 :std 1}})
           (new-normal-distribution :mean 0 :std 1)))

    ;; guassian (same as normal)
    (is (= org.deeplearning4j.nn.conf.distribution.GaussianDistribution
           (type (distribution {:gaussian {:mean 0.0 :std 1}}))))
    (is (= '(dl4clj.nn.conf.distributions/distribution {:gaussian {:mean 0.0 :std 1}})
           (new-gaussian-distribution :mean 0.0 :std 1)))
    ;; binomial
    (is (= org.deeplearning4j.nn.conf.distribution.BinomialDistribution
           (type (distribution
                  {:binomial {:number-of-trials       1
                              :probability-of-success 0.5}}))))
    (is (= '(dl4clj.nn.conf.distributions/distribution {:binomial {:number-of-trials       1
                                                                   :probability-of-success 0.5}})
           (new-binomial-distribution :probability-of-success 0.5
                                      :number-of-trials 1)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reconstruction distribution
;; dl4clj.nn.conf.variational.dist-builders
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest reconstruction-distribution-test
  (testing "the creation of reconstruction distributions for vae's"
    ;; bernoulli
    (is (= org.deeplearning4j.nn.conf.layers.variational.BernoulliReconstructionDistribution
           (type (distributions {:bernoulli {:activation-fn :sigmoid}}))))
    (is (= org.deeplearning4j.nn.conf.layers.variational.BernoulliReconstructionDistribution
           (type (distributions {:bernoulli {}}))))
    (is (= '(dl4clj.nn.conf.variational.dist-builders/distributions
             {:bernoulli {:activation-fn :sigmoid}})
           (new-bernoulli-reconstruction-distribution :activation-fn :sigmoid)))

    ;; exponential reconstruction
    (is (= org.deeplearning4j.nn.conf.layers.variational.ExponentialReconstructionDistribution
           (type (distributions {:exponential {:activation-fn :relu}}))))
    (is (= org.deeplearning4j.nn.conf.layers.variational.ExponentialReconstructionDistribution
           (type (distributions {:exponential {}}))))
    (is (= '(dl4clj.nn.conf.variational.dist-builders/distributions
             {:exponential {:activation-fn :relu}})
           (new-exponential-reconstruction-distribution :activation-fn :relu)))

    ;; gaussian
    (is (= org.deeplearning4j.nn.conf.layers.variational.GaussianReconstructionDistribution
           (type (distributions {:gaussian {:activation-fn :relu}}))))
    (is (= org.deeplearning4j.nn.conf.layers.variational.GaussianReconstructionDistribution
           (type (distributions {:gaussian {}}))))
    (is (= '(dl4clj.nn.conf.variational.dist-builders/distributions
             {:gaussian {:activation-fn :relu}})
           (new-gaussian-reconstruction-distribution :activation-fn :relu)))

    ;; composite
    (is (= org.deeplearning4j.nn.conf.layers.variational.CompositeReconstructionDistribution
           (type (distributions
                  {:composite
                   {:distributions-to-add
                    [{:bernoulli {:activation-fn :sigmoid
                                  :dist-size     5}}
                     {:exponential {:activation-fn :sigmoid
                                    :dist-size     3}}
                     {:gaussian {:activation-fn :hard-tanh
                                 :dist-size     1}}
                     {:bernoulli {:activation-fn :sigmoid
                                  :dist-size     4}}]}}))))
    (is (= '(dl4clj.nn.conf.variational.dist-builders/distributions
             {:composite
              {:distributions-to-add
               [{:bernoulli {:activation-fn :sigmoid
                             :dist-size     5}}
                {:exponential {:activation-fn :sigmoid
                               :dist-size     3}}
                {:gaussian {:activation-fn :hard-tanh
                            :dist-size     1}}
                {:bernoulli {:activation-fn :sigmoid
                             :dist-size     4}}]}})
           (new-composite-reconstruction-distribution
            [{:bernoulli {:activation-fn :sigmoid
                          :dist-size     5}}
             {:exponential {:activation-fn :sigmoid
                            :dist-size     3}}
             {:gaussian {:activation-fn :hard-tanh
                         :dist-size     1}}
             {:bernoulli {:activation-fn :sigmoid
                          :dist-size     4}}])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; step functions for use in nn-conf creation
;; dl4clj.nn.conf.step-fns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest step-fn-test
  (testing "the creation of step fns"
    (is (= '(dl4clj.nn.conf.step-fns/step-fn :default-step-fn)
           (new-default-step-fn)))
    (is (= org.deeplearning4j.nn.conf.stepfunctions.DefaultStepFunction
           (type (step-fn :default-step-fn))))

    (is (= '(dl4clj.nn.conf.step-fns/step-fn :gradient-step-fn)
           (new-gradient-step-fn)))
    (is (= org.deeplearning4j.nn.conf.stepfunctions.GradientStepFunction
           (type (step-fn :gradient-step-fn))))

    (is (= org.deeplearning4j.nn.conf.stepfunctions.NegativeDefaultStepFunction
           (type (step-fn :negative-default-step-fn))))
    (is (= '(dl4clj.nn.conf.step-fns/step-fn :negative-default-step-fn)
           (new-negative-default-step-fn)))

    (is (= org.deeplearning4j.nn.conf.stepfunctions.NegativeGradientStepFunction
           (type (step-fn :negative-gradient-step-fn))))
    (is (= '(dl4clj.nn.conf.step-fns/step-fn :negative-gradient-step-fn)
           (new-negative-gradient-step-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; input pre-processors test
;; dl4clj.nn.conf.input-pre-processor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest pre-processors-test
  (testing "the creation of input preprocessors for use in multi-layer-conf"
    ;; binominal sampling pre-processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.BinomialSamplingPreProcessor
           (type (pre-processors {:binominal-sampling-pre-processor {}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:binominal-sampling-pre-processor {}})
           (new-binominal-sampling-pre-processor)))

    ;; unit variance processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.UnitVarianceProcessor
           (type (pre-processors {:unit-variance-processor {}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:unit-variance-processor {}})
           (new-unit-variance-processor)))

    ;; Rnn -> Cnn pre-processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.RnnToCnnPreProcessor
           (type (pre-processors {:rnn-to-cnn-pre-processor
                                  {:input-height 1 :input-width 1
                                   :num-channels 1}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:rnn-to-cnn-pre-processor {:input-height 1 :input-width 1
                                         :num-channels 1}})
           (new-rnn-to-cnn-pre-processor :input-height 1 :input-width 1
                                         :num-channels 1)))

    ;; zero mean and unit variance
    (is (= org.deeplearning4j.nn.conf.preprocessor.ZeroMeanAndUnitVariancePreProcessor
           (type (pre-processors {:zero-mean-and-unit-variance-pre-processor {}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:zero-mean-and-unit-variance-pre-processor {}})
           (new-zero-mean-and-unit-variance-pre-processor)))

    ;; zero mean pre-pre processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.ZeroMeanPrePreProcessor
           (type (pre-processors {:zero-mean-pre-pre-processor {}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:zero-mean-pre-pre-processor {}})
           (new-zero-mean-pre-pre-processor)))

    ;; cnn -> feed foward pre processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor
           (type (pre-processors {:cnn-to-feed-forward-pre-processor {:input-height 1
                                                                      :input-width 1
                                                                      :num-channels 1}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:cnn-to-feed-forward-pre-processor {:input-height 1
                                                  :input-width 1
                                                  :num-channels 1}})
           (new-cnn-to-feed-forward-pre-processor :input-height 1
                                                  :input-width 1
                                                  :num-channels 1)))

    ;; cnn -> rnn pre processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.CnnToRnnPreProcessor
           (type (pre-processors {:cnn-to-rnn-pre-processor {:input-height 1
                                                             :input-width 1
                                                             :num-channels 1}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:cnn-to-rnn-pre-processor {:input-height 1
                                         :input-width 1
                                         :num-channels 1}})
           (new-cnn-to-rnn-pre-processor :input-height 1 :input-width 1
                                         :num-channels 1)))

    ;; feed forward -> cnn pre processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.FeedForwardToCnnPreProcessor
           (type (pre-processors {:feed-forward-to-cnn-pre-processor {:input-height 1
                                                                      :input-width 1
                                                                      :num-channels 1}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:feed-forward-to-cnn-pre-processor {:input-height 1
                                                  :input-width 1
                                                  :num-channels 1}})
           (new-feed-forward-to-cnn-pre-processor :input-height 1
                                                  :input-width 1
                                                  :num-channels 1)))

    ;; rnn -> feed forward pre processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.RnnToFeedForwardPreProcessor
           (type (pre-processors {:rnn-to-feed-forward-pre-processor {}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:rnn-to-feed-forward-pre-processor {}})
           (new-rnn-to-feed-forward-pre-processor)))

    ;; feed forward -> rnn pre processor
    (is (= org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor
           (type (pre-processors {:feed-forward-to-rnn-pre-processor {}}))))
    (is (= '(dl4clj.nn.conf.input-pre-processor/pre-processors
             {:feed-forward-to-rnn-pre-processor {}})
           (new-feed-forward-to-rnn-pre-processor)))

    ;; combine multiple pre-processors
    (is (= '(org.deeplearning4j.nn.conf.preprocessor.ComposableInputPreProcessor.
            (dl4clj.utils/array-of
             :data
             [(dl4clj.nn.conf.input-pre-processor/pre-processors
               {:zero-mean-pre-pre-processor {}})
              (dl4clj.nn.conf.input-pre-processor/pre-processors
               {:binominal-sampling-pre-processor {}})]
             :java-type org.deeplearning4j.nn.conf.InputPreProcessor))
           (new-composable-input-pre-processor
            :pre-processors [(new-zero-mean-pre-pre-processor)
                             (new-binominal-sampling-pre-processor)])))
    (is (= org.deeplearning4j.nn.conf.preprocessor.ComposableInputPreProcessor
           (type (org.deeplearning4j.nn.conf.preprocessor.ComposableInputPreProcessor.
                  (dl4clj.utils/array-of
                   :data
                   [(dl4clj.nn.conf.input-pre-processor/pre-processors
                     {:zero-mean-pre-pre-processor {}})
                    (dl4clj.nn.conf.input-pre-processor/pre-processors
                     {:binominal-sampling-pre-processor {}})]
                   :java-type org.deeplearning4j.nn.conf.InputPreProcessor)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; creation of default gradients
;; dl4clj.nn.gradient.default-gradient
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest default-gradient-test
  (testing "the creation and manipulation of gradients"
    (let [grad-with-var (gradient/set-gradient-for!
                         :grad (gradient/new-default-gradient)
                         :variable "foo"
                         :new-gradient (indarray-of-zeros
                                        :rows 2 :columns 2))]
      (is (= org.deeplearning4j.nn.gradient.DefaultGradient
             (type (gradient/new-default-gradient))))
      (is (= org.deeplearning4j.nn.gradient.DefaultGradient
             (type grad-with-var)))
      ;; I don't think this test is reliable bc it assumes cpu
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (gradient/gradient :grad grad-with-var))))
      (is (= java.util.LinkedHashMap
             (type (gradient/gradient-for-variable grad-with-var))))
      ;; gradient order was not explictly set
      (is (= nil
             (type (gradient/flattening-order-for-variables :grad grad-with-var
                                                            :variable "foo")))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; param initializers
;; dl4clj.nn.params.param-initializers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest param-initializers-test
  (testing "the creation of param initializers"
    (is (= org.deeplearning4j.nn.params.BatchNormalizationParamInitializer
           (type (param-init/new-batch-norm-initializer))))
    (is (= org.deeplearning4j.nn.params.CenterLossParamInitializer
           (type (param-init/new-center-loss-initializer))))
    (is (= org.deeplearning4j.nn.params.ConvolutionParamInitializer
           (type (param-init/new-convolution-initializer))))
    (is (= org.deeplearning4j.nn.params.DefaultParamInitializer
           (type (param-init/new-default-initializer))))
    (is (= org.deeplearning4j.nn.params.EmptyParamInitializer
           (type (param-init/new-empty-initializer))))
    (is (= org.deeplearning4j.nn.params.GravesBidirectionalLSTMParamInitializer
           (type (param-init/new-bidirectional-lstm-initializer))))
    (is (= org.deeplearning4j.nn.params.GravesLSTMParamInitializer
           (type (param-init/new-lstm-initializer))))
    (is (= org.deeplearning4j.nn.params.PretrainParamInitializer
           (type (param-init/new-pre-train-initializer))))
    (is (= org.deeplearning4j.nn.params.VariationalAutoencoderParamInitializer
           (type (param-init/new-vae-initializer))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; any layer builder
;; dl4clj.nn.conf.builders.builders
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; activation layer

#_(quick-nn-conf {:activation-layer
                {:n-in 10 :n-out 2 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo" :learning-rate 0.1
                 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :momentum 0.2 :momentum-after {0 0.3 1 0.4}
                 :updater :nesterovs :weight-init :distribution}})

(deftest activation-layer-test
  (testing "the creation of a activation layer from a nn-conf"
    (let [conf {:activation-layer
                {:n-in 10 :n-out 2 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo" :learning-rate 0.1
                 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :momentum 0.2 :momentum-after {0 0.3 1 0.4}
                 :updater :nesterovs :weight-init :distribution}}]
      (is (= :activation (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.ActivationLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest center-loss-output-layer-test
  (testing "the creation of a ceneter loss output layer from a nn-conf"
    (let [conf {:center-loss-output-layer
                {:alpha 0.1 :gradient-check? false :lambda 0.1
                 :loss-fn :mse :layer-name "foo1"
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam
                 :weight-init :distribution}}]
      (is (= :center-loss-output-layer (layer-creation/layer-type
                                        {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.training.CenterLossOutputLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest output-layer-test
  (testing "the creation of a output layer from a nn-conf"
    (let [conf {:output-layer
                {:n-in 10 :n-out 2 :loss-fn :mse
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo2"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :rho 0.7 :updater :adadelta
                 :weight-init :distribution}}]
      (is (= :output (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.OutputLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest rnn-output-layer-test
  (testing "the creation of a rnn output layer from a nn-conf"
    (let [conf {:rnn-output-layer
                {:n-in 10 :n-out 2 :loss-fn :mse
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo3"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :rms-decay 0.7 :updater :rmsprop
                 :weight-init :distribution}}]
      (is (= :rnnoutput (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.recurrent.RnnOutputLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest autoencoder-layer-test
  (testing "the creation of a autoencoder layer from a nn-conf"
    (let [conf {:auto-encoder
                {:n-in 10 :n-out 2 :pre-train-iterations 2
                 :loss-fn :mse :visible-bias-init 0.1
                 :corruption-level 0.7 :sparsity 0.4
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo4"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam
                 :weight-init :distribution}}]
      (is (= :auto-encoder (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.feedforward.autoencoder.AutoEncoder
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest rbm-layer-test
  (testing "the creation of a rbm layer from a nn-conf"
    (let [conf {:rbm {:n-in 10 :n-out 2 :loss-fn :mse
                      :pre-train-iterations 1 :visible-bias-init 0.7
                      :hidden-unit :softmax :visible-unit :identity
                      :k 2 :sparsity 0.6
                      :activation-fn :relu
                      :adam-mean-decay 0.2 :adam-var-decay 0.1
                      :bias-init 0.7 :bias-learning-rate 0.1
                      :dist {:normal {:mean 0 :std 1}}
                      :drop-out 0.2 :epsilon 0.3
                      :gradient-normalization :none
                      :gradient-normalization-threshold 0.9
                      :layer-name "foo5"
                      :learning-rate 0.1 :learning-rate-policy :inverse
                      :learning-rate-schedule {0 0.2 1 0.5}
                      :updater :adam :weight-init :distribution}}]
      (is (= :rbm (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.feedforward.rbm.RBM
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest graves-bidirectional-lstm-layer-test
  (testing "the creation of a bidirectional lstm layer from a nn-conf"
    (let [conf {:graves-bidirectional-lstm
                {:n-in 10 :n-out 2 :forget-gate-bias-init 0.2
                 :gate-activation-fn :relu
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo6"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam :weight-init :distribution}}]
      (is (= :graves-bidirectional-lstm (layer-creation/layer-type
                                         {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.recurrent.GravesBidirectionalLSTM
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest graves-lstm-layer-test
  (testing "the creation of a lstm layer from a nn-conf"
    (let [conf {:graves-lstm
                {:n-in 10 :n-out 2 :forget-gate-bias-init 0.2
                 :gate-activation-fn :relu
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo7"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :momentum 0.2 :momentum-after {0 0.3 1 0.4}
                 :updater :nesterovs :weight-init :distribution}}]
      (is (= :graves-lstm (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.recurrent.GravesLSTM
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest batch-normalization-layer-test
  (testing "the creation of a batch normalization layer from a nn-conf"
    (let [conf {:batch-normalization
                {:n-in 10 :n-out 2 :beta 0.5
                 :decay 0.3 :eps 0.1 :gamma 0.1
                 :mini-batch? false :lock-gamma-beta? true
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo8"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :rho 0.7 :updater :adadelta
                 :weight-init :distribution}}]
      (is (= :batch-normalization (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.normalization.BatchNormalization
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest convolution-layer-test
  (testing "the creation of a convolution layer from a nn-conf"
    (let [conf {:convolutional-layer
                {:n-in 10 :n-out 2
                 :kernel-size [2 2] :padding [2 2] :stride [2 2]
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo9"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam :weight-init :distribution}}
          conf-1d {:convolution-1d-layer
                   {:n-in 10 :n-out 2
                    :kernel-size 6 :stride 3 :padding 3
                    :activation-fn :relu
                    :bias-init 0.7 :bias-learning-rate 0.1
                    :dist {:normal {:mean 0 :std 1}}
                    :drop-out 0.2 :epsilon 0.3
                    :gradient-normalization :none
                    :gradient-normalization-threshold 0.9
                    :layer-name "foo10"
                    :learning-rate 0.1 :learning-rate-policy :inverse
                    :learning-rate-schedule {0 0.2 1 0.5}
                    :rms-decay 0.7 :updater :rmsprop
                    :weight-init :distribution}}]
      (is (= :convolution (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.convolution.ConvolutionLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf)))))

      (is (= :convolution1d (layer-creation/layer-type {:nn-conf (quick-nn-conf conf-1d)})))
      (is (= org.deeplearning4j.nn.layers.convolution.Convolution1DLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf-1d))))))))

(deftest dense-layer-test
  (testing "creation of a dense layer from a nn-conf"
    (let [conf {:dense-layer {:n-in 10 :n-out 2
                              :activation-fn :relu
                              :adam-mean-decay 0.2 :adam-var-decay 0.1
                              :bias-init 0.7 :bias-learning-rate 0.1
                              :dist {:normal {:mean 0 :std 1}}
                              :drop-out 0.2 :epsilon 0.3
                              :gradient-normalization :none
                              :gradient-normalization-threshold 0.9
                              :layer-name "foo11"
                              :learning-rate 0.1 :learning-rate-policy :inverse
                              :learning-rate-schedule {0 0.2 1 0.5}
                              :updater :adam :weight-init :distribution}}]
      (is (= :dense (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.feedforward.dense.DenseLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest embedding-layer-test
  (testing "the creation of a embedding layer from a nn-conf"
    (let [conf {:embedding-layer
                {:n-in 10 :n-out 2
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo12"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :rms-decay 0.7 :updater :rmsprop
                 :weight-init :distribution}}]
      (is (= :embedding (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.feedforward.embedding.EmbeddingLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest local-response-normalization-layer-test
  (testing "the creation of a local response normalization layer from a nn-conf"
    (let [conf {:local-response-normalization
                {:alpha 0.2 :beta 0.2 :k 0.2 :n 1
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo13"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam :weight-init :distribution}}]
      (is (= :local-response-normalization (layer-creation/layer-type
                                            {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.normalization.LocalResponseNormalization
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest subsampling-layer-test
  (testing "the creation of a subsampling layer from a nn-conf"
    (let [conf {:subsampling-layer
                {:kernel-size [2 2] :stride [2 2] :padding [2 2]
                 :pooling-type :sum
                 :build? true
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo14"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :momentum 0.2 :momentum-after {0 0.3 1 0.4}
                 :updater :nesterovs :weight-init :distribution}}
          conf-1d {:subsampling-1d-layer
                   {:kernel-size 2 :stride 2 :padding 2
                    :pooling-type :sum
                    :build? true
                    :activation-fn :relu
                    :adam-mean-decay 0.2 :adam-var-decay 0.1
                    :bias-init 0.7 :bias-learning-rate 0.1
                    :dist {:normal {:mean 0 :std 1}}
                    :drop-out 0.2 :epsilon 0.3
                    :gradient-normalization :none
                    :gradient-normalization-threshold 0.9
                    :layer-name "foo15"
                    :learning-rate 0.1 :learning-rate-policy :inverse
                    :learning-rate-schedule {0 0.2 1 0.5}
                    :updater :adam :weight-init :distribution}}]

      (is (= :subsampling (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.convolution.subsampling.SubsamplingLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf)))))
      (is (= :subsampling1d (layer-creation/layer-type {:nn-conf (quick-nn-conf conf-1d)})))
      (is (= org.deeplearning4j.nn.layers.convolution.subsampling.Subsampling1DLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf-1d))))))))

(deftest loss-layer-test
  (testing "the creation of a loss layer from a nn-conf"
    (let [conf {:loss-layer {:loss-fn :mse
                             :activation-fn :relu
                             :adam-mean-decay 0.2 :adam-var-decay 0.1
                             :bias-init 0.7 :bias-learning-rate 0.1
                             :dist {:normal {:mean 0 :std 1}}
                             :drop-out 0.2 :epsilon 0.3
                             :gradient-normalization :none
                             :gradient-normalization-threshold 0.9
                             :layer-name "foo16"
                             :learning-rate 0.1 :learning-rate-policy :inverse
                             :learning-rate-schedule {0 0.2 1 0.5}
                             :updater :adam :weight-init :distribution}}]
      (is (= :loss (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.LossLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest dropout-layer-test
  (testing "the creation of a dropout layer from a nn-conf"
    (let [conf {:dropout-layer
                {:n-in 2 :n-out 10
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo17"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam :weight-init :distribution}}]
      (is (= :dropout (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.DropoutLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest global-pooling-layer-test
  (testing "the creation of a global pooling layer from a nn-conf, also shows off layer validation
\n this triggers warnings because you can't set the updater so the values for updaters are automatically set to nil"
    (let [conf {:global-pooling-layer
                {:pooling-dimensions [3 2]
                 :collapse-dimensions? true
                 :pnorm 2
                 :pooling-type :pnorm
                 :activation-fn :relu
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo18"
                 :updater :rmsprop
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :weight-init :distribution}}]
      (println "\n example layer validation warnings \n")
      (is (= org.deeplearning4j.nn.layers.pooling.GlobalPoolingLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest zero-padding-layer-test
  (testing "the creation of a zero padding layer from a nn-conf"
    (let [conf {:zero-padding-layer
                {:pad-top 1 :pad-bot 2 :pad-left 3 :pad-right 4
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo19"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam :weight-init :distribution}}]
      (is (= :zero-padding (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.convolution.ZeroPaddingLayer
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest variational-autoencoder-layer-test
  (testing "the creation of a vae layer from a nn-conf"
    (let [conf {:variational-auto-encoder
                {:n-in 5 :n-out 10 :loss-fn :mse
                 :pre-train-iterations 1 :visible-bias-init 2
                 :decoder-layer-sizes [5 9]
                 :encoder-layer-sizes [7 2]
                 :reconstruction-distribution {:composite
                                               {:distributions-to-add
                                                [{:gaussian {:activation-fn :tanh
                                                             :dist-size 5}}
                                                 {:bernoulli {:dist-size 1}}]}}
                 :vae-loss-fn {:output-activation-fn :sigmoid :loss-fn :mse}
                 :num-samples 2 :pzx-activation-function :tanh
                 :activation-fn :relu
                 :adam-mean-decay 0.2 :adam-var-decay 0.1
                 :bias-init 0.7 :bias-learning-rate 0.1
                 :dist {:normal {:mean 0 :std 1}}
                 :drop-out 0.2 :epsilon 0.3
                 :gradient-normalization :none
                 :gradient-normalization-threshold 0.9
                 :layer-name "foo20"
                 :learning-rate 0.1 :learning-rate-policy :inverse
                 :learning-rate-schedule {0 0.2 1 0.5}
                 :updater :adam :weight-init :distribution}}]
      (is (= :variational-autoencoder (layer-creation/layer-type {:nn-conf (quick-nn-conf conf)})))
      (is (= org.deeplearning4j.nn.layers.variational.VariationalAutoencoder
             (type (layer-creation/new-layer :nn-conf (quick-nn-conf conf))))))))

(deftest frozen-layer-test
  (testing "the creation of a frozen layer from an existing layer"
    (let [layer-conf {:dense-layer
                      {:n-in 10 :n-out 2
                       :activation-fn :relu
                       :adam-mean-decay 0.2 :adam-var-decay 0.1
                       :bias-init 0.7 :bias-learning-rate 0.1
                       :dist {:normal {:mean 0 :std 1}}
                       :drop-out 0.2 :epsilon 0.3
                       :gradient-normalization :none
                       :gradient-normalization-threshold 0.9
                       :layer-name "foo11"
                       :learning-rate 0.1 :learning-rate-policy :inverse
                       :learning-rate-schedule {0 0.2 1 0.5}
                       :updater :adam :weight-init :distribution}}]
      (is (= org.deeplearning4j.nn.layers.FrozenLayer
             (type (layer-creation/new-frozen-layer
                    (model/set-param-table!
                     :model (layer-creation/new-layer
                             :nn-conf (quick-nn-conf layer-conf))
                     :param-table-map {"foo" (indarray-of-zeros :rows 1)}))))))))



;;;;;;;; needs to be updated to the new nn/builder
;; need to add tests for all the helper fns like layer helper and multi layer helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; nn-conf-builder
;; dl4clj.nn.conf.builders.nn-conf-builder
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest nn-test
  (testing "the helper fns for builder"
    ;; layer builder helper
    (is (= '(.layer "im a builder" (dl4clj.utils/eval-and-build (str "im a layer created by a fn")))
           (nn/layer-builder-helper "im a builder" '(str "im a layer created by a fn"))))
    (is (= '(.layer
             "im a builder"
             (dl4clj.utils/eval-and-build
              (dl4clj.nn.conf.builders.layers/builder
               {:some-single-layer {:some-layer-config "some config value"}})))
           (nn/layer-builder-helper
            "im a builder"
            {:some-single-layer {:some-layer-config "some config value"}})))
    (is (= '(doto (.list "im a builder")
              (.layer
               0
               (dl4clj.utils/eval-and-build
                (dl4clj.nn.conf.builders.layers/builder
                 {:first-layer {:some-config "some value"}})))
              (.layer
               1
               (dl4clj.utils/eval-and-build
                (dl4clj.nn.conf.builders.layers/builder
                 {:second-layer {:other-config "other value"}}))))
           (nn/layer-builder-helper
            "im a builder"
            {0 {:first-layer {:some-config "some value"}}
             1 {:second-layer {:other-config "other value"}}})))
    (is (= '(doto (.list "im a builder")
              (.layer
               0
               (dl4clj.utils/eval-and-build
                (im-a-fn-call "with" "some" "args")))
              (.layer
               1
               (dl4clj.utils/eval-and-build
                (dl4clj.nn.conf.builders.layers/builder
                 {:layer-2 {:some-config "some value"}}))))
           (nn/layer-builder-helper
            "im a builder"
            {0 '(im-a-fn-call "with" "some" "args")
             1 {:layer-2 {:some-config "some value"}}})))
    (is (= '(doto (.list "im a builder")
              (.layer
               0
               (dl4clj.utils/eval-and-build
                (im-a-fn-call "with" "some" "args")))
              (.layer
               1
               (dl4clj.utils/eval-and-build
                (im-another-fn-call :keyword "args"))))
           (nn/layer-builder-helper
            "im a builder"
            {0 '(im-a-fn-call "with" "some" "args")
             1 '(im-another-fn-call :keyword "args")})))
    ;; multi layer builder helper
    ;; no mln opts supplied, just returns the builder passed
    (is (= "some builder"
           (nn/multi-layer-builder-helper {} "some builder" "layers")))
    ;; we use the layers to determine what builder to use
    (is (= '(doto "some list builder" (.backprop true))
           (nn/multi-layer-builder-helper {:backprop? true}
                                          "some list builder"
                                          {0 {:dense-layer {:n-in 10}}
                                           1 (layer/dense-layer-builder :n-in 10)})))
    ;; here we have to use the constructor for the MultiLayerConfigBuilder
    ;; because we don't have a list builder, we just have a nn-conf
    (let [layer-fn-call-code (nn/multi-layer-builder-helper
                              {:backprop? true}
                              "a nn conf builder"
                              (layer/dense-layer-builder :n-in 10))
          layer-config-code (nn/multi-layer-builder-helper
                             {:backprop? true}
                             "a nn conf builder"
                             {:dense-layer {:n-in 10}})
          nil-layers (nn/multi-layer-builder-helper
                      {:backprop? true}
                      "a nn conf builder"
                      nil)
          [_ mln-builder] layer-fn-call-code
          [_ mln-b] layer-config-code
          [_ mln] nil-layers
          ]
      (is (= '(org.deeplearning4j.nn.conf.MultiLayerConfiguration$Builder.)
             mln-builder))
      (is (= '(org.deeplearning4j.nn.conf.MultiLayerConfiguration$Builder.)
             mln-b))
      (is (= '(org.deeplearning4j.nn.conf.MultiLayerConfiguration$Builder.)
             mln))))
  (testing "the creation of neural network configurations"
    (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration
           (type
            (nn/builder
             :iterations 1
             :lr-policy-decay-rate 0.3
             :max-num-line-search-iterations 6
             :mini-batch? true
             :minimize? true
             :use-drop-connect? false
             :optimization-algo :lbfgs
             :lr-score-based-decay-rate 0.7
             :regularization? true
             :seed 123
             :step-fn :gradient-step-fn
             :convolution-mode :strict
             :lr-policy-power 0.1
             :default-learning-rate-policy :poly
             :build? true
             :as-code? false
             ))))
    (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration$Builder
           (type
            (nn/builder
             :iterations 1
             :lr-policy-decay-rate 0.3
             :lr-policy-power 0.4
             :default-learning-rate-policy :poly
             :max-num-line-search-iterations 6
             :mini-batch? true
             :minimize? true
             :use-drop-connect? false
             :optimization-algo :lbfgs
             :lr-score-based-decay-rate 0.7
             :regularization? true
             :seed 123
             :step-fn :default-step-fn
             :convolution-mode :strict
             :build? false
             :as-code? false))))
    (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
           (type
            (nn/builder :default-activation-fn :relu
                        :step-fn :negative-gradient-step-fn
                        :default-updater :none
                        :use-drop-connect? true
                        :default-drop-out 0.2
                        :default-weight-init :xavier-uniform
                        :build? true
                        :default-gradient-normalization :renormalize-l2-per-layer
                        :as-code? false
                        :layers {0 {:dense-layer {:n-in 100
                                                  :n-out 1000
                                                  :layer-name "first layer"
                                                  :activation-fn :tanh
                                                  :gradient-normalization :none}}
                                 1 {:dense-layer {:n-in 1000
                                                  :n-out 10
                                                  :layer-name "second layer"
                                                  :gradient-normalization :none}}}))))
    (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration$ListBuilder
           (type
            (nn/builder :default-activation-fn :relu
                        :step-fn :negative-gradient-step-fn
                        :default-updater :none
                        :use-drop-connect? true
                        :default-drop-out 0.2
                        :default-weight-init :xavier-uniform
                        :build? false
                        :default-gradient-normalization :renormalize-l2-per-layer
                        :as-code? false
                        :layers {0 {:dense-layer {:n-in 100
                                                  :n-out 1000
                                                  :layer-name "first layer"
                                                  :activation-fn :tanh
                                                  :gradient-normalization :none}}
                                 1 {:dense-layer {:n-in 1000
                                                  :n-out 10
                                                  :layer-name "second layer"
                                                  :activation-fn :tanh
                                                  :gradient-normalization :none}}}))))
    (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration
           ;; show that the activation fn changes
           (type
            (nn/builder :default-activation-fn :relu
                        :step-fn :negative-gradient-step-fn
                        :default-updater :none
                        :use-drop-connect? true
                        :default-drop-out 0.2
                        :default-weight-init :xavier-uniform
                        :default-gradient-normalization :renormalize-l2-per-layer
                        :build? true
                        :as-code? false
                        :layers {:dense-layer {:n-in 100
                                               :n-out 1000
                                               :layer-name "first layer"
                                               :activation-fn :tanh
                                               :gradient-normalization :none}}))))
    (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration
           ;; show that fns can be passed
           (type
            (nn/builder :default-activation-fn :relu
                        :step-fn :negative-gradient-step-fn
                        :default-updater :none
                        :use-drop-connect? true
                        :default-drop-out 0.2
                        :default-weight-init :xavier-uniform
                        :default-gradient-normalization :renormalize-l2-per-layer
                        :build? true
                        :as-code? false
                        :layers (layer/dense-layer-builder :n-in 100
                                                           :n-out 1000
                                                           :layer-name "first layer"
                                                           :activation-fn :tanh
                                                           :gradient-normalization :none)))))
    ;; we can also pass our multi layer args to nn/builder for single or multi layer confs

    (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
         (type
          (nn/builder :default-activation-fn :relu
                      :step-fn :negative-gradient-step-fn
                      :default-updater :none
                      :use-drop-connect? true
                      :default-drop-out 0.2
                      :default-weight-init :xavier-uniform
                      :default-gradient-normalization :renormalize-l2-per-layer
                      :build? true
                      :as-code? false
                      :layers {:dense-layer {:n-in 100
                                             :n-out 1000
                                             :layer-name "first layer"
                                             :activation-fn :tanh
                                             :gradient-normalization :none}}
                      ;; multi layer args
                      :backprop? true
                      :input-pre-processors {0 {:zero-mean-pre-pre-processor {}}
                                             1 (new-unit-variance-processor)}))))
    (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
           (type
            (nn/builder :default-activation-fn :relu
                        :step-fn :negative-gradient-step-fn
                        :default-updater :none
                        :use-drop-connect? true
                        :default-drop-out 0.2
                        :default-weight-init :xavier-uniform
                        :default-gradient-normalization :renormalize-l2-per-layer
                        :build? true
                        :as-code? false
                        ;; multi layer arg
                        :backprop? true
                        :layers {0 {:dense-layer {:n-in 100
                                                  :n-out 1000
                                                  :layer-name "first layer"
                                                  :activation-fn :tanh
                                                  :gradient-normalization :none}}
                                 1 (layer/dense-layer-builder :n-in 10)}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; multi-layer-config-builder test
;; dl4clj.nn.conf.builders.multi-layer-builders
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest multi-layer-builder-test
  (testing "the creation of mutli layer nn's from single layer confs"
    (let [nn-conf (nn/builder :default-activation-fn :relu
                              :step-fn :negative-gradient-step-fn
                              :default-updater :none
                              :use-drop-connect? true
                              :default-drop-out 0.2
                              :default-weight-init :xavier-uniform
                              :default-gradient-normalization :renormalize-l2-per-layer
                              :build? false
                              :as-code? true
                              :layers {:dense-layer {:n-in 10
                                                     :n-out 100
                                                     :layer-name "another layer"
                                                     :activation-fn :tanh
                                                     :gradient-normalization :none}})]
      ;; with a list builder, a built nn-conf, and all opts
      (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
             (type
              (nn/mln-from-nn-confs :confs [nn-conf]
                                    :backprop? true
                                    :backprop-type :standard
                                    :input-pre-processors {0 {:zero-mean-pre-pre-processor {}}
                                                           1 {:unit-variance-processor {}}}
                                    :input-type {:feed-forward {:size 100}}
                                    :pretrain? false
                                    :build? true))))
      (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
             (type
              (nn/mln-from-nn-confs :confs [nn-conf nn-conf]
                                    :backprop? true
                                    :backprop-type :standard
                                    :input-pre-processors {0 {:zero-mean-pre-pre-processor {}}
                                                           1 {:unit-variance-processor {}}}
                                    :input-type {:feed-forward {:size 100}}
                                    :pretrain? false
                                    :build? true))))
      (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
             (type
              (nn/mln-from-nn-confs :confs [nn-conf nn-conf]
                                    :build? true)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; multi-layer-network test
;; dl4clj.nn.multilayer.multi-layer-network
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest multi-layer-creation-test
  (testing "the creation of multi layer networks from objects and from code"
    (let [mln-conf-code (nn/builder :seed 123
                                    :optimization-algo :stochastic-gradient-descent
                                    :iterations 1
                                    :default-learning-rate 0.006
                                    :default-updater :nesterovs
                                    :default-momentum 0.9
                                    :regularization? true
                                    :default-l2 1e-4
                                    :build? true
                                    :default-gradient-normalization :renormalize-l2-per-layer
                                    :layers {0 (layer/dense-layer-builder
                                                :n-in 784
                                                :n-out 1000
                                                :layer-name "first layer"
                                                :activation-fn :relu
                                                :weight-init :xavier)
                                             1 {:output-layer {:n-in 1000
                                                               :n-out 10
                                                               :layer-name "second layer"
                                                               :activation-fn :soft-max
                                                               :weight-init :xavier}}}
                                    :backprop? true
                                    :pretrain? false
                                    :as-code? true)

          mln-conf-obj (nn/builder :seed 123
                                   :optimization-algo :stochastic-gradient-descent
                                   :iterations 1
                                   :default-learning-rate 0.006
                                   :default-updater :nesterovs
                                   :default-momentum 0.9
                                   :regularization? true
                                   :default-l2 1e-4
                                   :build? true
                                   :default-gradient-normalization :renormalize-l2-per-layer
                                   :layers {0 (layer/dense-layer-builder
                                               :n-in 784
                                               :n-out 1000
                                               :layer-name "first layer"
                                               :activation-fn :relu
                                               :weight-init :xavier)
                                            1 {:output-layer {:n-in 1000
                                                              :n-out 10
                                                              :layer-name "second layer"
                                                              :activation-fn :soft-max
                                                              :weight-init :xavier}}}
                                   :backprop? true
                                   :pretrain? false
                                   :as-code? false)

          mln-as-code (multi-layer-network/new-multi-layer-network :conf mln-conf-code
                                                                   :as-code? true)

          mln-from-obj (multi-layer-network/new-multi-layer-network :conf mln-conf-obj)]
      (is (= '(org.deeplearning4j.nn.multilayer.MultiLayerNetwork.
               (.build
                (doto
                    (doto
                        (.list
                         (doto
                             (org.deeplearning4j.nn.conf.NeuralNetConfiguration$Builder.)
                           (.l2 1.0E-4)
                           (.regularization true)
                           (.gradientNormalization
                            (dl4clj.constants/value-of
                             {:gradient-normalization :renormalize-l2-per-layer}))
                           (.updater (dl4clj.constants/value-of {:updater :nesterovs}))
                           (.seed 123)
                           (.momentum 0.9)
                           (.iterations 1)
                           (.learningRate 0.006)
                           (.optimizationAlgo
                            (dl4clj.constants/value-of
                             {:optimization-algorithm :stochastic-gradient-descent}))))
                      (.layer
                       0
                       ;; fn call to create layer
                       (dl4clj.utils/eval-and-build
                        (doto
                            (org.deeplearning4j.nn.conf.layers.DenseLayer$Builder.)
                          (.nOut 1000)
                          (.activation (dl4clj.constants/value-of {:activation-fn :relu}))
                          (.weightInit (dl4clj.constants/value-of {:weight-init :xavier}))
                          (.nIn 784)
                          (.name "first layer"))))
                      (.layer
                       1
                       ;; pass config map to create layer
                       (dl4clj.utils/eval-and-build
                        (dl4clj.nn.conf.builders.layers/builder
                         {:output-layer
                          {:n-in 1000,
                           :n-out 10,
                           :layer-name "second layer",
                           :activation-fn :soft-max,
                           :weight-init :xavier}}))))
                  (.backprop true))))
             mln-as-code))
      (is (= (type mln-from-obj) (type (eval mln-as-code)))))))




(deftest multi-layer-network-method-test
  (testing "multi layer network methods"
    (let [mln-conf (nn/builder :seed 123
                               :optimization-algo :stochastic-gradient-descent
                               :iterations 1
                               :default-learning-rate 0.006
                               :default-updater :nesterovs
                               :default-momentum 0.9
                               :regularization? true
                               :default-l2 1e-4
                               :build? true
                               :default-gradient-normalization :renormalize-l2-per-layer
                               :layers {0 (layer/dense-layer-builder
                                           :n-in 784
                                           :n-out 1000
                                           :layer-name "first layer"
                                           :activation-fn :relu
                                           :weight-init :xavier)
                                        1 {:output-layer {:n-in 1000
                                                          :n-out 10
                                                          :layer-name "second layer"
                                                          :activation-fn :soft-max
                                                          :weight-init :xavier}}}
                               :backprop? true
                               :pretrain? false
                               :as-code? false)
          mln (multi-layer-network/new-multi-layer-network :conf mln-conf)
          init-mln (multi-layer-network/initialize! :mln mln :ds (new-mnist-ds))
          mnist-iter (new-mnist-data-set-iterator :batch-size 128 :train? true :seed 123)
          input (get-features (get-example :ds (new-mnist-ds) :idx 0))
          eval (new-classification-evaler :n-classes 10)
          init-no-ds (model/init! :model mln)
          _ (println "\n example evaluation stats \n")
          evaled (eval-model-whole-ds :mln init-no-ds :iter mnist-iter :evaler eval)]
      ;;other-input (get-mln-input :mln init-mln)
      ;;^ this currently crashes all of emacs
      ;; need a fitted mln for (is (= "" (type (get-epsilon :mln ...))))
      ;; might need a fitted mln for this too (fine-tune! :mln init-no-ds)
      ;; was getting an unexpected error: Mis matched lengths: [600000] != [10]
      ;; need to init a mln with a recurrent layer to test
      ;; rnn-... fns
      (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork (type mln)))
      ;; these tests will have to be updated to include other NDArray types so wont break with gpus
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (mln/activate-selected-layers
                    :from 0 :to 1 :mln mln :input input))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (mln/activate-from-prev-layer
                    :current-layer-idx 0 :mln mln :input input :training? true))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (mln/activate-from-prev-layer
                    :current-layer-idx 1 :mln mln :training? true
                    ;; input to second layer is output of first
                    :input (mln/activate-from-prev-layer
                            :current-layer-idx 0 :mln mln :input input :training? true)))))
      (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork
             (type (mln/clear-layer-mask-arrays! mln))))
      (is (= java.util.ArrayList
             (type (mln/compute-z :mln init-mln :training? true :input input))))
      (is (= java.util.ArrayList
             (type (mln/compute-z :mln init-mln :training? true))))
      (is (= java.lang.String (type (get-stats :evaler evaled))))
      (is (= org.deeplearning4j.eval.Evaluation
             (type (multi-layer-network/evaluate-classification :mln init-no-ds :iter mnist-iter))))
      (is (= org.deeplearning4j.eval.Evaluation
             (type
              (multi-layer-network/evaluate-classification :mln init-no-ds :iter mnist-iter
                                       :labels-list ["0" "1" "2" "3" "4" "5" "6" "7" "8" "9"]))))
      (is (= org.deeplearning4j.eval.RegressionEvaluation
             (type (multi-layer-network/evaluate-regression :mln init-no-ds :iter mnist-iter))))
      (is (= java.util.ArrayList
             (type (mln/feed-forward :mln init-mln :input input))))
      (is (= java.util.ArrayList
             (type (mln/feed-forward :mln init-mln))))
      (is (= java.util.ArrayList (type (mln/feed-forward-to-layer :mln init-mln :layer-idx 0 :train? true))))
      (is (= java.util.ArrayList (type (mln/feed-forward-to-layer :mln init-mln :layer-idx 0 :input input))))
      (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration (type (mln/get-default-config init-mln))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray (type (mln/get-input init-mln))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray (type (get-labels init-mln))))
      (is (= org.deeplearning4j.nn.layers.feedforward.dense.DenseLayer
             (type (mln/get-layer :mln init-mln :layer-idx 0))))
      (is (= ["first layer" "second layer"] (mln/get-layer-names mln)))
      (is (= (type (array-of :data [] :java-type org.deeplearning4j.nn.api.Layer))
             (type (mln/get-layers init-mln))))
      (is (= org.deeplearning4j.nn.conf.MultiLayerConfiguration
             (type (mln/get-layer-wise-config init-mln))))
      ;; we never set a mask
      (is (= nil (mln/get-mask init-mln)))
      (is (= 2 (mln/get-n-layers init-mln)))
      (is (= org.deeplearning4j.nn.layers.OutputLayer (type (mln/get-output-layer init-mln))))
      (is (= org.deeplearning4j.nn.updater.MultiLayerUpdater (type (mln/get-updater init-mln))))
      (println "\n example summary of a Multi Layer Network \n")
      (is (= (type mln) (type (multi-layer-network/initialize-layers! :mln mln :input input))))
      (is (true? (mln/is-init-called? mln)))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (multi-layer-network/output :mln init-mln :input input))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (multi-layer-network/output :mln init-no-ds :iter mnist-iter))))
      (is (= (type mln) (type (mln/print-config mln))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (multi-layer-network/reconstruct :mln mln
                                :layer-output (first (mln/feed-forward-to-layer
                                                      :layer-idx 0 :mln mln :input input))
                                :layer-idx 1))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (multi-layer-network/score-examples :mln init-no-ds :iter mnist-iter
                                   :add-regularization-terms? false))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (multi-layer-network/score-examples :mln init-mln :dataset (new-mnist-ds)
                                   :add-regularization-terms? false))))
      (is (= java.lang.String (type (multi-layer-network/summary init-mln))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (mln/z-from-prev-layer :mln init-mln :input input
                                   :current-layer-idx 0 :training? true)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fine tuning/transfer learning
;; dl4clj.nn.transfer-learning.fine-tune-conf
;; dl4clj.nn.transfer-learning.helper
;; dl4clj.nn.transfer-learning.transfer-learning
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest transfer-learning-tests
  (testing "the transfer learning fns"
    (let [nn-conf (nn-conf-builder :default-activation-fn :relu
                                   :step-fn :negative-gradient-step-fn
                                   :default-updater :none
                                   :use-drop-connect? false
                                   :default-weight-init :xavier-uniform
                                   :default-gradient-normalization :none
                                   :build? true
                                   :layer {:dense-layer{:n-in 10
                                                        :n-out 100
                                                        :layer-name "some layer"
                                                        :activation-fn :tanh
                                                        :gradient-normalization :none}})
          fine-tune-conf (new-fine-tune-conf :activation-fn :relu
                                             :n-iterations 2
                                             :regularization? false
                                             :seed 1234)
          l-builder (nn-conf-builder :seed 123
                                     :optimization-algo :stochastic-gradient-descent
                                     :iterations 1
                                     :default-learning-rate 0.006
                                     :default-updater :nesterovs
                                     :default-momentum 0.9
                                     :regularization? false
                                     :build? false
                                     :default-gradient-normalization :none
                                     :layers {0 {:dense-layer {:n-in 784
                                                               :n-out 1000
                                                               :layer-name "first layer"
                                                               :activation-fn :relu
                                                               :weight-init :xavier}}
                                              1 {:output-layer {:n-in 1000
                                                                :n-out 10
                                                                :layer-name "second layer"
                                                                :activation-fn :soft-max
                                                                :weight-init :xavier}}})
          mln-conf (multi-layer-config-builder :list-builder l-builder
                                               :backprop? true
                                               :pretrain? false
                                               :build? true)
          mln (init! :model (new-multi-layer-network :conf mln-conf))
          helper (new-helper :mln mln :frozen-til 0)
          featurized (featurize :helper helper :data-set (get-example :ds (new-mnist-ds)
                                                                      :idx 0))
          featurized-input (get-features featurized)
          tlb (transfer-learning-builder
               :mln (init!
                     :model
                     (new-multi-layer-network
                      :conf
                      (multi-layer-config-builder
                       :list-builder (nn-conf-builder
                                      :seed 123
                                      :optimization-algo :stochastic-gradient-descent
                                      :iterations 1
                                      :default-learning-rate 0.006
                                      :default-updater :nesterovs
                                      :default-momentum 0.9
                                      :regularization? false
                                      :build? false
                                      :default-gradient-normalization :none
                                      :layers {0 {:dense-layer {:n-in 10
                                                                :n-out 100
                                                                :layer-name "first layer"
                                                                :activation-fn :relu
                                                                :weight-init :xavier}}
                                               1 {:activation-layer {:n-in 100
                                                                     :n-out 10
                                                                     :layer-name "second layer"
                                                                     :activation-fn :soft-max
                                                                     :weight-init :xavier}}
                                               2 {:output-layer {:n-in 10
                                                                 :n-out 1
                                                                 :layer-name "output layer"
                                                                 :activation-fn :soft-max
                                                                 :weight-init :xavier}}})
                       :backprop? true
                       :pretrain? false
                       :build? true)))
               :build? false)]
      #_(clojure.pprint/pprint
       (dl4clj.nn.transfer-learning.transfer-learning/builder
         :mln
         (dl4clj.nn.multilayer.multi-layer-network/new-multi-layer-network
          :as-code? true
          :conf
          (dl4clj.nn.conf.builders.nn/builder :layers {0 {:dense-layer {:n-in 100
                                                                        :n-out 10
                                                                        :layer-name "first layer"
                                                                        :activation-fn :tanh
                                                                        :weight-init :relu}}
                                                       1 (dl4clj.nn.conf.builders.layers/dense-layer-builder
                                                          :n-in 10
                                                          :n-out 10
                                                          :layer-name "second layer"
                                                          :activation-fn :tanh
                                                          :gradient-normalization :none)}))
         :fine-tune-conf  (dl4clj.nn.transfer-learning.fine-tune-conf/new-fine-tune-conf
                           :activation-fn :relu
                           :n-iterations 2
                           :regularization? false
                           :seed 1234)
         :remove-last-n-layers 1
         :replacement-layer {:layer-idx 0 :n-out 101 :weight-init :relu}
         :add-layers {2 {:dense-layer {:n-in 100
                                       :n-out 10
                                       :layer-name "third layer"
                                       :activation-fn :tanh
                                       :weight-init :relu}}
                      4 (dl4clj.nn.conf.builders.layers/output-layer-builder
                         :n-in 10
                         :n-out 10
                         :layer-name "5th layer"
                         :activation-fn :tanh
                         :gradient-normalization :none)
                      5 {:dense-layer {:n-in 100
                                       :n-out 10
                                       :layer-name "last layer"
                                       :activation-fn :tanh
                                       :weight-init :relu}}
                      3 {:dense-layer {:n-in 100
                                       :n-out 10
                                       :layer-name "4th layer"
                                       :activation-fn :tanh
                                       :weight-init :relu}}}
         :as-code? true
         :input-pre-processor {:layer-idx 0
                               :pre-processor {:unit-variance-processor {}}}))
      ;; dl4clj.nn.transfer-learning.fine-tune-conf
      (is (= org.deeplearning4j.nn.transferlearning.FineTuneConfiguration
             (type fine-tune-conf)))
      (is (= org.deeplearning4j.nn.transferlearning.FineTuneConfiguration$Builder
             (type (new-fine-tune-conf :activation-fn :relu
                                       :n-iterations 2
                                       :regularization? true
                                       :seed 123
                                       :build? false))))
      (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration
             (type (applied-to-nn-conf! :fine-tune-conf fine-tune-conf
                                        :nn-conf nn-conf))))
      (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration$Builder
             (type (nn-conf-from-fine-tune-conf :fine-tune-conf fine-tune-conf))))
      (is (= org.deeplearning4j.nn.conf.NeuralNetConfiguration
             (type (nn-conf-from-fine-tune-conf :fine-tune-conf fine-tune-conf
                                                :build? true))))
      ;; dl4clj.nn.transfer-learning.helper
      (is (= org.deeplearning4j.nn.transferlearning.TransferLearningHelper
             (type (new-helper :mln mln))))
      (is (= org.deeplearning4j.nn.transferlearning.TransferLearningHelper
             (type helper)))
      (is (= org.nd4j.linalg.dataset.DataSet (type featurized)))
      (is (= org.deeplearning4j.nn.transferlearning.TransferLearningHelper
             (type (fit-featurized! :helper helper :data-set featurized))))
      (is (= org.nd4j.linalg.cpu.nativecpu.NDArray
             (type (output-from-featurized :helper helper :featurized-input featurized-input))))
      (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork
             (type (unfrozen-mln helper))))

      ;; dl4clj.nn.transfer-learning.transfer-learning
      (is (= org.deeplearning4j.nn.multilayer.MultiLayerNetwork
             (type (transfer-learning-builder
                    :mln mln
                    :fine-tune-conf fine-tune-conf
                    :remove-output-layer? true
                    :replacement-layer {:layer-idx 0 :n-out 1001 :weight-init :xavier-uniform}
                    :remove-last-n-layers 1
                    :add-layer (dl4clj.nn.conf.builders.builders/output-layer-builder
                            :n-in 100
                            :n-out 10
                            :layer-name "another layer"
                            :activation-fn :tanh
                            :gradient-normalization :none)
                    :set-feature-extractor-idx 0
                    :input-pre-processor {:layer-idx 0 :pre-processor (new-unit-variance-processor)}))))
      ;; testing add-layers
      (is (= ["first layer" "replacement another layer" "replacement second layer"]
             (get-layer-names
              (transfer-learning-builder :tlb tlb
                                         :fine-tune-conf fine-tune-conf
                                         :remove-last-n-layers 2
                                         :add-layers {1 (dl4clj.nn.conf.builders.builders/activation-layer-builder
                                                         :n-in 10
                                                         :n-out 100
                                                         :layer-name "replacement another layer"
                                                         :activation-fn :tanh
                                                         :gradient-normalization :none)
                                                      2 {:output-layer {:n-in 100
                                                                        :n-out 10
                                                                        :layer-name "replacement second layer"
                                                                        :activation-fn :soft-max
                                                                        :weight-init :xavier}}}))))
      ;; testing add-layer
      (is (= ["first layer" "another layer"]
             (get-layer-names
              (transfer-learning-builder
               :mln mln
               :fine-tune-conf fine-tune-conf
               :remove-output-layer? true
               :add-layer (dl4clj.nn.conf.builders.builders/output-layer-builder
                           :n-in 100
                           :n-out 10
                           :layer-name "another layer"
                           :activation-fn :tanh
                           :gradient-normalization :none)
               :set-feature-extractor-idx 0
               :input-pre-processor {:layer-idx 0 :pre-processor (new-unit-variance-processor)})))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dl4clj.nn.updater.layer-updater
;; dl4clj.nn.updater.multi-layer-updater
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest updater-tests
  (testing "the creation of model updaters"
    (let [mln (as-> (nn-conf-builder
                     :seed 123
                     :optimization-algo :stochastic-gradient-descent
                     :iterations 1
                     :default-learning-rate 0.006
                     :default-updater :nesterovs
                     :default-momentum 0.9
                     :regularization? false
                     :build? false
                     :default-gradient-normalization :none
                     :layers {0
                              {:dense-layer {:n-in 10
                                             :n-out 100
                                             :layer-name "first layer"
                                             :activation-fn :relu
                                             :weight-init :xavier}}
                              1 {:activation-layer {:n-in 100
                                                    :n-out 10
                                                    :layer-name "second layer"
                                                    :activation-fn :soft-max
                                                    :weight-init :xavier}}
                              2 {:output-layer {:n-in 10
                                                :n-out 1
                                                :layer-name "output layer"
                                                :activation-fn :soft-max
                                                :weight-init :xavier}}})
                  conf
                (multi-layer-config-builder :list-builder conf
                                            :backprop? true
                                            :pretrain? false
                                            :build? true)
                (new-multi-layer-network :conf conf)
                (init! :model conf))
          layer-updater (new-layer-updater)
          conf-with-param (as-> (nn-conf-builder
                                 :regularization? true
                                 :build? true
                                 :layer {:graves-lstm
                                         {:n-in 10 :n-out 2 :forget-gate-bias-init 0.2
                                          :gate-activation-fn :relu
                                          :activation-fn :relu
                                          :bias-init 0.7 :bias-learning-rate 0.1
                                          :dist {:normal {:mean 0 :std 1}}
                                          :drop-out 0.2 :epsilon 0.3
                                          :gradient-normalization :renormalize-l2-per-layer
                                          :l2 0.1 :l2-bias 1
                                          :gradient-normalization-threshold 0.9
                                          :layer-name "foo"
                                          :learning-rate 0.1 :learning-rate-policy :inverse
                                          :learning-rate-schedule {0 0.6 1 0.5}
                                          :momentum 0.2  :momentum-after {0 0.3 1 0.4}
                                          :updater :nesterovs :weight-init :distribution}})
                              l
                            ;; add this part to add-variable!
                            (do (.variables l (.add (.variables l false) "baz"))
                                l)
                            (add-variable! :nn-conf l :var-name "baz")
                            (set-learning-rate-by-param! :nn-conf l :var-name "foo" :rate 0.2))
          layer (new-layer :nn-conf conf-with-param)

          ;;(new-layer :nn-conf l)
          ;; try initializing the layer instead of calling new layer for setting l2byparam
          ;; was able to set that in a mln above
          ;; multi-layer-network-creation-test
          ]
      (is (= org.deeplearning4j.nn.updater.LayerUpdater (type layer-updater)))
      (is (= org.deeplearning4j.nn.updater.MultiLayerUpdater
             (type (new-multi-layer-updater :mln mln))))
      (is (= (type layer)
             (type (:layer (apply-lrate-decay-policy! :updater layer-updater
                                                      :layer layer
                                                      :iteration 1
                                                      :variable "foo"
                                                      :decay-policy :score)))))
      (is (= (type layer)
             (type (:layer (apply-momentum-decay-policy! :updater layer-updater
                                                         :layer layer :iteration 1
                                                         :variable "foo")))))
      (is (= (type layer)
             (type (:layer (pre-apply! :updater layer-updater
                                       :layer layer :iteration 1
                                       :gradient (new-default-gradient))))))
      (is (= {} (get-updater-for-variable layer-updater)))
      ;; cant get this to work, cant add things to the l2ByParam hash map for some damn reason
      ;; thats what this is trying to do under the hood
      ;; https://github.com/deeplearning4j/deeplearning4j/blob/master/deeplearning4j-nn/src/main/java/org/deeplearning4j/nn/conf/NeuralNetConfiguration.java
      #_(is (= "" (post-apply! :updater layer-updater
                               :layer layer :gradient-array (rand [2])
                               :param "foo" :mini-batch-size 10)))
      )))



(comment

  (dl4clj.nn.conf.layers.shared-fns/instantiate
 :layer (dl4clj.nn.conf.builders.builders/activation-layer-builder
         :n-in 10
         :n-out 100
         :layer-name "another layer"
         :activation-fn :tanh
         :gradient-normalization :none)
 :conf
 (nn-conf-builder :global-activation-fn :relu
                  :step-fn :negative-gradient-step-fn
                  :updater :none
                  :use-drop-connect true
                  :drop-out 0.2
                  :weight-init :xavier-uniform
                  :gradient-normalization :renormalize-l2-per-layer
                  :build? true
                  :layer (dl4clj.nn.conf.builders.builders/activation-layer-builder
                          :n-in 10
                          :n-out 100
                          :layer-name "another layer"
                          :activation-fn :tanh
                          :gradient-normalization :none))
 :listeners (dl4clj.optimize.listeners.listeners/new-score-iteration-listener)
 #_(dl4clj.utils/array-of :data (dl4clj.optimize.listeners.listeners/new-score-iteration-listener)
                                   :java-type org.deeplearning4j.optimize.api.IterationListener)
 :layer-idx 0
 :layer-param-view (nd4clj.linalg.factory.nd4j/rand [10])
 :initialize-params? true))
