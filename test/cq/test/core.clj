(ns cq.test.core
  (:require [clojure.test :refer :all]
            [cq.core :refer :all]))

(deftest test1
  (testing "a quip"
    (is (= (time (solve "fict o ncc bivteclnbklzn o lcpji ukl pt vzglcddp"
                        {\b \t}
                        words))
           "when i see thunderstorms i reach for an umbrella"))))
