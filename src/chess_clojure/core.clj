(ns chess-clojure.core
  (:require [clojure.math.numeric-tower :refer [abs]]
            [primitive-math :as p]
            [clojure.string :refer [join]]))


(defn murmur3-32 [key-seq-of-32-bit-ints seed]
  (let [len (int (* 4 (count key-seq-of-32-bit-ints)))
        c1 (unchecked-int 0xcc9e2d51)
        c2 (unchecked-int 0x1b873593)
        r1 (unchecked-int 15)
        r2 (unchecked-int 13)
        m (unchecked-int 5)
        n (unchecked-int 0xe6546b64)
        hash (loop [ks key-seq-of-32-bit-ints
                    hash (int seed)]
               (if (seq ks)
                 (let [k (int (first ks))
                       k (p/* k c1)
                       k (p/bit-or (p/<< k r1) (p/>>> k (- 32 r1)))
                       k (p/* k c2)
                       hash (p/bit-xor hash k)
                       hash (p/bit-or (bit-shift-left hash r2) (p/>>> hash (- 32 r2)))
                       hash (p/+ (p/* hash m) n)]
                   (recur (rest ks) hash))
                 hash))
        hash (p/bit-xor hash len)
        hash (p/bit-xor hash (p/>>> hash 16))
        hash (p/* hash (unchecked-int 0x85ebca6b))
        hash (p/bit-xor hash (p/>>> hash 13))
        hash (p/* hash (unchecked-int 0xc2b2ae35))
        hash (p/bit-xor hash (p/>>> hash 16))]
    (unchecked-int hash)))


(defn clj-sequence-hash-combine [s element-hash-fn]
  (loop [s s
         h (int 1)]
    (if (seq s)
      (recur (rest s)
             (unchecked-int (p/+ (p/* h (int 31))
                                 (int (element-hash-fn (first s))))))
      h)))


(defn alt-integer-hash [i]
  (cond (<= Integer/MIN_VALUE i Integer/MAX_VALUE) (murmur3-32 [i] 0)
        (<= Long/MIN_VALUE i Long/MAX_VALUE) (murmur3-32
                                              [(p/>>> i 32)
                                               (p/bit-and i (long 0xffffffff))]
                                              0)
        :else (hash i)))


(defn alt-hash-0
  "(alt-hash-0 obj) is intended to return the same value as (hash obj)
for all integers, keywords, sets, and vectors, with nesting of
collections allowed.  Throws an exception if any other types are
encountered."
  [obj]
  (cond (integer? obj) (hash obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map alt-hash-0 obj)))
        (vector? obj) (clj-sequence-hash-combine obj alt-hash-0)
        :else (throw (IllegalArgumentException. (format "alt-hash-0 called with object of class %s" (class obj))))))


(defn alt-hash-1
  "alt-hash-1 is same as alt-hash-0 and hash, except ints and longs
are hashed using murmur3-32."
  [obj]
  (cond (integer? obj) (alt-integer-hash obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map alt-hash-1 obj)))
        (vector? obj) (clj-sequence-hash-combine obj alt-hash-1)
        :else (throw (IllegalArgumentException. (format "alt-hash-1 called with object of class %s" (class obj))))))


(defn alt-hash-2
  "alt-hash-2 is same as alt-hash-1, except vectors are hashed using
murmur3-32 on the sequence of hash values of its elements."
  [obj]
  (cond (integer? obj) (alt-integer-hash obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map alt-hash-2 obj)))
        (vector? obj) (murmur3-32 (map alt-hash-2 obj) 0)
        :else (throw (IllegalArgumentException. (format "alt-hash-1 called with object of class %s" (class obj))))))


(defn print-hash-val-header []
  (println "        # hash     avg       max     hash fn")
  (println "# items  vals   collisions collision name")
  (println "------- ------- ---------- --------- -----------"))


(defn print-hash-stats [coll hash-fn hash-fn-name]
  (let [hash-freq (frequencies (map hash-fn coll))
        freq-freq (frequencies (vals hash-freq))]
;    (println "\nHash values, and how many times they occur in the solution set:")
;    (doseq [hash-val (sort (keys hash-freq))]
;      (println (format "    0x%08x %7d" hash-val (hash-freq hash-val))))

;    (println "\nCollision bucket sizes, and how many collision buckets have that size:")
;    (doseq [collision-bucket-size (sort (keys freq-freq))]
;      (println (format "    %7d %7d"
;                       collision-bucket-size
;                       (freq-freq collision-bucket-size))))

    (println (format "%7d %7d %10.1f %9d %s"
                     (count coll)
                     (count hash-freq)
                     (/ (* 1.0 (count coll)) (count hash-freq))
;                     (/ (* 1.0 (reduce + (map (fn [[sz num-buckets]] (* sz num-buckets))
;                                              freq-freq)))
;                        (reduce + (vals freq-freq)))
                     (apply max (keys freq-freq))
                     hash-fn-name))))


(defn print-all-hash-stats [coll]
  (doseq [[hash-fn hash-fn-name] [[hash "hash"]
                                  [alt-hash-1 "alt-hash-1"]
                                  [alt-hash-2 "alt-hash-2"]]]
    (print-hash-stats coll hash-fn hash-fn-name)))


(defn check-alt-hash-0 [coll]
  (doseq [val coll]
    (when (not= (hash val) (alt-hash-0 val))
      (println (format "Found val with hash=0x%08x but alt-hash-0=0x%08x: %s"
                       (hash val) (alt-hash-0 val) val)))))


(defn takes? [piece [dx dy]]
  (case piece
    :K (and (<= dx 1) (<= dy 1))
    :Q (or (zero? dx) (zero? dy) (= dx dy))
    :R (or (zero? dx) (zero? dy))
    :B (= dx dy)
    :N (or (and (= dx 1) (= dy 2)) (and (= dx 2) (= dy 1)))))

(defn allows? [piece [px py] [x y]]
  (let [delta [(abs (- px x)) (abs (- py y))]]
    (and (not (and (= px x) (= py y))) (not (takes? piece delta)))))

(defn allowed? [candidate [x y] solution]
  (every?
    (fn [[piece pos]] 
      (and (allows? piece pos [x y]) (allows? candidate [x y] pos)))
    solution))

(declare solve)

(defn solve-wrapped [rows cols pieces]
  (if (empty? pieces)
    #{#{}}
    (set 
      (let [candidate (first pieces)]
        (for [solution (solve rows cols (rest pieces))
              x (range 0 cols)
              y (range 0 rows)
              :when (allowed? candidate [x y] solution)]
          (conj solution [candidate [x y]]))))))

(defn solve [rows cols pieces]
  (let [x (solve-wrapped rows cols pieces)]
    (println (format "Solve %s - e.g. %s" (seq pieces) (first x)))
    (print-all-hash-stats x)
    (check-alt-hash-0 x)
    x))

(defn -main [& args]
  (print-hash-val-header)
  (doseq [i (range 8 40 3)]
    (let [pairs (for [x (range 0 i), y (range 0 i)] [x y])]
      (println (format "All-pairs in [0 0] thru [%d %d]" (dec i) (dec i)))
      (print-all-hash-stats pairs)
      (check-alt-hash-0 pairs)))
  
  (println)
  (print-hash-val-header)
  (let [solution (solve
                  ;3 3 [:K :K :R]
                  ;4 4 [:R :R :N :N :N :N]
                  ;5 5 [:K :K :N :R :B :Q]  ; ~1 sec
                  5 6 [:K :K :N :R :B :Q]  ; ~10 sec
                  ;6 6 [:K :K :N :R :B :Q]  ; ~3-4 mins
                  ;6 7 [:K :K :N :R :B :Q]  ; longer
                  ;6 9 [:K :K :N :R :B :Q]
                  )]
    ; (println (join "\n" solution))
    (println (count solution))))
