(ns cq.test.block
  (:require [clojure.test :refer :all]
            [cq.block :refer :all]))

(deftest possible-test
  (are [ct pt ans] (= ans (possible? ct pt))
    nil nil false
    "fissqz" "rabbit" true
    "fisfqz" "rabbit" false
    "fissqzs" "rabbit" false))

(deftest matches-test
  (are [clue ct pt ans] (= ans (matches? clue ct pt))
    nil nil nil false
    {\g \f, \y \o, \l \d} "gyyl" "food" true
    {\g \f, \y \o, \l \d} "gyrl" "fold" true
    {\g \f, \y \o, \l \d, \r \w} "gyrl" "fold" false))

(deftest merge-clue-test
  (are [clue ct pt ans] (= ans (merge-clue clue ct pt))
    nil nil nil nil
    {\g \f, \y \o, \l \d} "dbi" "rat" {\g \f, \y \o, \l \d, \d \r, \b \a, \i \t}
    {\g \f, \y \o, \l \d} "dbi" "hog" nil
    {\g \f, \y \o, \l \d} "lyi" "dog" {\g \f, \y \o, \l \d, \i \g}))

(deftest decode-test
  (are [clue ct ans] (= ans (decode clue ct))
    nil nil nil
    {\f \g, \x \o} "fx" "go"
    {\f \g, \x \o} "fix" "go"
    {\g \f, \y \o, \l \d} "gyyl" "food"
    {\g \f, \y \o, \l \d} "yg gyyl" "of food"))

(deftest solve-test
  (testing "a quip"
    (is (= (time (solve "fict o ncc bivteclnbklzn o lcpji ukl pt vzglcddp"
                        {\b \t}
                        words))
           "when i see thunderstorms i reach for an umbrella"))))
