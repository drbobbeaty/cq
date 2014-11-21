(ns cq.core)

(def quip "fict o ncc bivteclnbklzn o lcpji ukl pt vzglcddp")

(def clue {\b \t})

(def words (->> (slurp "resources/words")
                (.split #"\n")))

(defn pattern [words]
  (loop [p [] d {} n 0 f (first words) r (rest words)]
    (if f
      (if (contains? d f)
        (recur (conj p (d f)) d n (first r) (rest r))
        (recur (conj p n) (assoc d f n) (inc n) (first r) (rest r)))
      p)))

(defn words-by-pattern [words]
  (group-by pattern words))

(def wbp (words-by-pattern words))

(defn add-substitutions [code plain substitutions]
  (reduce (fn [s x] (assoc s (x 0) (x 1)))
          substitutions
          (map vector code plain)))

(defn conforms? [codeword substitutions plainword]
  (let [pattern (pattern codeword)]
    (reduce (fn [s x]
              (if s
                (and
                  (if (contains? substitutions (nth codeword x))
                    (= (substitutions (nth codeword x)) (nth plainword x))
                    true)
                  (let [newsub (add-substitutions codeword plainword substitutions)
                        vs (vals newsub)
                        svs (into #{} vs)]
                    (= (count vs) (count svs))))
                false)) true pattern)))

(defn candidates
  ([codeword pattern substitutions wbp]
    (let [words (wbp pattern)]
      (into [] (filter #(conforms? codeword substitutions %) words))))
  ([codeword substitutions wbp]
    (let [pattern (pattern codeword)]
      (candidates codeword pattern substitutions wbp))))

(defn apply-substitution [code substitution]
  (apply str (map #((assoc substitution \space \space) %) code)))

;; state:
;; {
;;   :status (:forward | :stopped | :end)
;;   :left [... [cw sub cur-cand cur-sub [cand-pool]]]
;;   :right [... next-cw2 next-cw1]
;; }

(defn forward-step
  "move state forward one step
  state must be primed with one back item
  retutns
    next state
    :end if at end
    :stopped if can't advance"
  [state wbp]
  (let [{:keys [left right status]} state
        [cw sub cur-cand cur-sub cand-pool] (peek left)
        next-cw (peek right)]
    (cond
      ;; we're at the end
      (nil? next-cw) {:status :end :left left :right right}
      ;; advance state one step
      :else (let [next-cand-pool (candidates next-cw cur-sub wbp)
                  next-cur-cand (peek next-cand-pool)]
              (if (nil? next-cur-cand)
                {:status :stopped :left left :right right}
                {:status :forward
                 :left (conj left [next-cw
                                   cur-sub
                                   next-cur-cand
                                   (add-substitutions next-cw next-cur-cand cur-sub)
                                   (pop next-cand-pool)])
                 :right (pop right)})))))

(defn forward
  "advance forward to the end
  state must be primed with one back item
  returns
    state at :end
    :stopped if can't progress to the end"
  [state wbp]
  (loop [cur-state state]
    (let [next-state (forward-step cur-state wbp)]
      (cond
        (= (next-state :status) :end) next-state
        (= (next-state :status) :stopped) next-state
        :else (recur next-state)))))

(defn backtrack
  "Takes a :stopped state back to the previous candidate choice and then advances forward to the end
    returns
      state at :end
      :stopped if can't progress to the end
      :no-solution if you are out of previous choice points"
  [state wbp]
  (let [{:keys [left right status]} state
        [cw sub cur-cand cur-sub cand-pool] (peek left)]
    (cond
      (nil? cw) {:status :no-solution :left left :right right}
      (empty? cand-pool) (backtrack {:left (pop left)
                                     :right (conj right cw)}
                                    wbp)
      :else (let [newstate {:left (conj (pop left) [cw
                                                    sub
                                                    (peek cand-pool)
                                                    (add-substitutions cw (peek cand-pool) sub)
                                                    (pop cand-pool)])
                            :right right}]
              (forward newstate wbp)))))

(defn solve
  "Find a set of words from the supplied word list that satifiy the quip pattern
  return
    the substituted words"
  [quip clue words]
  (let [wbp (words-by-pattern words)
        codewords (into [] (sort-by count (vec (.split #" " quip))))
        cw (peek codewords)
        cw-pat (pattern cw)
        cands (candidates cw cw-pat clue wbp)]
    (if (empty? cands)
      :no-solution
      (time (let [initial-state {:status :forward
                                 :left [[cw
                                         clue
                                         (peek cands)
                                         (add-substitutions cw (peek cands) clue)
                                         (pop cands)]]
                                 :right (pop codewords)}]
              (loop [cur-state (forward initial-state wbp)]
                (cond
                  (= (cur-state :status) :stopped) (recur (backtrack cur-state wbp))
                  (= (cur-state :status) :no-solution) :no-solution
                  :else (apply-substitution quip ((peek (cur-state :left)) 3)))))))))

;(time (solve "fict o ncc bivteclnbklzn o lcpji ukl pt vzglcddp"
;             {\b \t}
;             words))
;
;(time (solve "when i see thunderstorms i reach for an umbrella"
;             {}
;             words))
;
;(time (solve "potato onion carrot"
;             {}
;             words))
