(ns cq.block
  "Namespace for the block attack that's fashioned after the Obj-C and
  ruby versions."
  (:require [cq.logging :refer [log-execution-time!]]))

(def quip "fict o ncc bivteclnbklzn o lcpji ukl pt vzglcddp")

(def clue {\b \t})

(def lca (int \a))
(def lcz (int \z))
(def uca (int \A))
(def ucz (int \Z))

(def words
  "This is the map of all known plaintext words - organized by the length
  of the word to speed up the matching process. The result is a map where
  the key is the length, and the value is a sequence of words."
  (->> (slurp "resources/words")
       (.split #"\n")
       (group-by count)))

(defn possible?
  "Function to see if the cyphertext and plaintext have the same pattern of
  characters such that they could possibly match - given the right legend."
  [ct pt]
  (if (and (string? ct) (string? pt) (= (count ct) (count pt)))
    (let [pc (count (distinct (map str ct pt)))
          ctc (count (distinct ct))
          ptc (count (distinct pt))]
      (= pc ctc ptc))
    false))

(defn matches?
  "Function to take the provided Legend and map the cyphertext into a
  plaintext string and then see if the resulting word COULD BE the plaintext.
  This is not to say that the word is completely decoded - only that those
  characters that are decoded match the characters in the plaintext."
  [clue ct pt]
  (if (and (map? clue) (string? ct) (string? pt) (= (count ct) (count pt)))
    (let [miss? (fn [cc pc] (let [pp (get clue cc)] (and pp (not= pp pc))))]
      (not-any? identity (map miss? ct pt)))
    false))

(defn merge-clue
  "Function to attempt to merge the decoding implied in the cyphertext-to-plaintext
  conversion for the two words provided into the first argument - a map of the
  cyphertext caracters to plaintext characters. If successful, the function
  will return the updated map, otherwise nil."
  [clue ct pt]
  (if (and (map? clue) (string? ct) (string? pt) (= (count ct) (count pt)))
    (let [known (set (vals clue))
          pairs (distinct (map vector ct pt))
          bad? (fn [[cc pc]] (and (not= (get clue cc) pc)
                                  (or (get clue cc) (known pc))))]
      (if-not (some bad? pairs)
        (into clue pairs)))))

(defn decode
  "Function to take the cyphertext and legend and attempt to decode the one
  with the other. Characters that aren't in the legend will be left out so
  that it's clear from the length that the decoding wasn't complete."
  [clue ct]
  (if (and (string? ct) (map? clue))
    (let [is-alpha? (fn [c] (or (<= uca (int c) ucz) (<= lca (int c) lcz)))
          codex (fn [c] (if (is-alpha? c) (get clue c) c))]
      (apply str (map codex ct)))))

(defn attack
  "Function to do the block attack on the quip, broken down into a sequence
  of the cyphertext words, and their lists of possible plaintext words, as
  well as the index in that sequence to attack, and the clue (legend) to use.
  This will return the first match to the attack and that's it."
  [quip pieces idx clue]
  (if (and (coll? pieces) (map? clue))
    (let [{cw :cyphertext poss :possibles} (nth pieces idx)
          last? (= idx (dec (count pieces)))]
      (some identity (for [pt poss
                           :when (matches? clue cw pt)
                           :let [nc (merge-clue clue cw pt)]
                           :when nc]
                       (if last?
                         (decode nc quip)
                         (attack quip pieces (inc idx) nc)))))))

(log-execution-time! attack {:msg-fn (fn [ret q p i c] (format "word: %s" i))})

(defn solve
  "Find a set of words from the supplied word list that satifiy the quip pattern
  return the substituted words"
  [quip clue words]
  (let [pieces (sort-by #(count (:possibles %)) <
                 (for [cw (vec (.split quip " "))]
                   { :cyphertext cw
                     :possibles (filter #(possible? cw %) (get words (count cw))) }))]
    (attack quip pieces 0 clue)))

(log-execution-time! solve)
