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


(defn sequence-hash-combine [s multiplier element-hash-fn]
  (let [multiplier (int multiplier)]
    (loop [s s
           h (int 1)]
      (if (seq s)
        (recur (rest s)
               (unchecked-int (p/+ (p/* h multiplier)
                                   (unchecked-int (element-hash-fn (first s))))))
        h))))


(defn clj-sequence-hash-combine [s element-hash-fn]
  (sequence-hash-combine s 31 element-hash-fn))


(defn engelberg-sequence-hash-combine-2013-10-29
  "Recommended hash for vectors and sequences from Mark Engelberg's
hashing executive summary document as of Oct 29 2013."
  [s element-hash-fn]
  (unchecked-int (sequence-hash-combine s 524287 element-hash-fn)))


(defn engelberg-sequence-hash-combine-2013-10-30
  "Recommended hash for vectors and sequences from Mark Engelberg's
hashing executive summary document as of Oct 30 2013."
  [s element-hash-fn]
  (unchecked-int (sequence-hash-combine s 122949829 element-hash-fn)))


(defn engelberg-sequence-hash-combine-2013-10-31
  "Recommended hash for vectors and sequences from Mark Engelberg's
hashing executive summary document as of Oct 31 2013."
  [s element-hash-fn]
  (unchecked-int (sequence-hash-combine s (int -1640531527) element-hash-fn)))


(defn alt-integer-hash [i]
  (cond (<= Integer/MIN_VALUE i Integer/MAX_VALUE) (murmur3-32 [i] 0)
        (<= Long/MIN_VALUE i Long/MAX_VALUE) (murmur3-32
                                              [(p/>>> i 32)
                                               (p/bit-and i (long 0xffffffff))]
                                              0)
        :else (hash i)))


(defn engelberg-long-hash-munge-2013-10-29
  "Recommended hash for longs from Mark Engelberg's hashing executive
summary document as of Oct 29 2013."
  [i]
  (if (<= Long/MIN_VALUE i Long/MAX_VALUE)
    (let [a (unchecked-long i)
          a (p/bit-xor a (p/<<  a 21))
          a (p/bit-xor a (p/>>> a 35))
          a (p/bit-xor a (p/<<  a  4))
          a (p/bit-xor a (p/>>> a 32))]
      (unchecked-int a))
    (hash i)))


(defn engelberg-long-hash-munge-2013-10-30
  "Recommended hash for longs from Mark Engelberg's hashing executive
summary document as of Oct 30 2013."
  [i]
  (if (<= Long/MIN_VALUE i Long/MAX_VALUE)
    (let [a (unchecked-long i)
          a (p/bit-xor a (p/<<  a 13))
          a (p/bit-xor a (p/>>> a  7))
          a (p/bit-xor a (p/<<  a 17))
          a (p/bit-xor a (p/>>> a 32))]
      (unchecked-int a))
    (hash i)))


(defn engelberg-xor-shift-32-2013-10-29
  "Recommended modification to hash values of set elements before they
are added together, from Mark Engelberg's hashing executive summary
document as of Oct 29 2013."
  [i]
  (let [i (unchecked-int i)
        int32-mask (long 0xffffffff)
        ;; Make a long that has no sign extension
        i (p/bit-and int32-mask (long i))
        ;; Be careful to preserve no sign extension bits creeping into
        ;; intermediate calculations.
        i (p/bit-and int32-mask (p/bit-xor i (p/<<  i  3)))
        i (p/bit-and int32-mask (p/bit-xor i (p/>>> i  1)))
        i (p/bit-and int32-mask (p/bit-xor i (p/<<  i 14)))]
    (unchecked-int i)))


(defn engelberg-xor-shift-32-2013-10-30
  "Recommended modification to hash values of set elements before they
are added together, from Mark Engelberg's hashing executive summary
document as of Oct 30 2013."
  [i]
  (let [i (unchecked-int i)
        int32-mask (long 0xffffffff)
        ;; Make a long that has no sign extension
        i (p/bit-and int32-mask (long i))
        ;; Be careful to preserve no sign extension bits creeping into
        ;; intermediate calculations.
        i (p/bit-and int32-mask (p/bit-xor i (p/<<  i 13)))
        i (p/bit-and int32-mask (p/bit-xor i (p/>>> i 17)))
        i (p/bit-and int32-mask (p/bit-xor i (p/<<  i  5)))]
    (unchecked-int i)))


(defn alt-hash-0
  "(alt-hash-0 obj) is intended to return the same value as (hash obj)
in Clojure 1.5.1 for all integers, keywords, sets, and vectors, with
nesting of collections allowed.  Throws an exception if any other
types are encountered."
  [obj]
  (cond (integer? obj) (hash obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map alt-hash-0 obj)))
        (vector? obj) (clj-sequence-hash-combine obj alt-hash-0)
        :else (throw (IllegalArgumentException. (format "alt-hash-0 called with object of class %s" (class obj))))))


(defn alt-hash-1
  "alt-hash-1 is same as alt-hash-0 and Clojure 1.5.1 hash, except
ints and longs are hashed using murmur3-32."
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
        :else (throw (IllegalArgumentException. (format "alt-hash-2 called with object of class %s" (class obj))))))


(defn engelberg-hash-2013-10-29
  "Combines all recommendations from Mark Engelberg's Oct 29 2013
executive summary document, except for those on maps."
  [obj]
  (cond (integer? obj) (engelberg-long-hash-munge-2013-10-29 obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map (fn [x]
                                                   (engelberg-xor-shift-32-2013-10-29
                                                    (engelberg-hash-2013-10-29 x)))
                                                 obj)))
        (vector? obj) (engelberg-sequence-hash-combine-2013-10-29
                       obj engelberg-hash-2013-10-29)
        :else (throw (IllegalArgumentException. (format "engelberg-hash-2013-10-29 called with object of class %s" (class obj))))))


(defn engelberg-hash-2013-10-30
  "Combines all recommendations from Mark Engelberg's Oct 30 2013
executive summary document, except for those on maps."
  [obj]
  (cond (integer? obj) (engelberg-long-hash-munge-2013-10-30 obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map (fn [x]
                                                   (engelberg-xor-shift-32-2013-10-30
                                                    (engelberg-hash-2013-10-30 x)))
                                                 obj)))
        (vector? obj) (engelberg-sequence-hash-combine-2013-10-30
                       obj engelberg-hash-2013-10-30)
        :else (throw (IllegalArgumentException. (format "engelberg-hash-2013-10-30 called with object of class %s" (class obj))))))


(defn engelberg-hash-2013-10-31
  "Combines all recommendations from Mark Engelberg's Oct 31 2013
executive summary document, except for those on maps.  Same as
engelberg-hash-2013-10-30, except the multiplier for the
vector/sequence hash is different."
  [obj]
  (cond (integer? obj) (engelberg-long-hash-munge-2013-10-30 obj)
        (keyword? obj) (hash obj)
        (set? obj) (unchecked-int (reduce + (map (fn [x]
                                                   (engelberg-xor-shift-32-2013-10-30
                                                    (engelberg-hash-2013-10-30 x)))
                                                 obj)))
        (vector? obj) (engelberg-sequence-hash-combine-2013-10-31
                       obj engelberg-hash-2013-10-30)
        :else (throw (IllegalArgumentException. (format "engelberg-hash-2013-10-30 called with object of class %s" (class obj))))))


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

    (println (format "%7d %7d %10.2f %9d %s"
                     (count coll)
                     (count hash-freq)
                     (/ (* 1.0 (count coll)) (count hash-freq))
;                     (/ (* 1.0 (reduce + (map (fn [[sz num-buckets]] (* sz num-buckets))
;                                              freq-freq)))
;                        (reduce + (vals freq-freq)))
                     (apply max (keys freq-freq))
                     hash-fn-name))))


(defn print-all-hash-stats [coll]
  (doseq [[hash-fn hash-fn-name] [[hash (str "Clojure " (clojure-version) " hash")]
                                  [alt-hash-0 "alt-hash-0 (should be same as Clojure 1.5.1 hash)"]
                                  [alt-hash-1 "alt-hash-1"]
                                  [alt-hash-2 "alt-hash-2"]
                                  [engelberg-hash-2013-10-29 "engelberg-hash-2013-10-29"]
                                  [engelberg-hash-2013-10-30 "engelberg-hash-2013-10-30"]
                                  [engelberg-hash-2013-10-31 "engelberg-hash-2013-10-31"]
                                  ]]
    (print-hash-stats coll hash-fn hash-fn-name)))


(def reimplementation-of-clojure-hash
  (case (clojure-version)
    ;; For running with Clojure 1.5.1 unmodified
    "1.5.1" {:fn alt-hash-0 :name "alt-hash-0"}
    ;; For running with a version of Clojure with hash modified
    "1.6.0-master-SNAPSHOT" {:fn engelberg-hash-2013-10-30
                             :name "engelberg-hash-2013-10-30"}))

(defn check-java-vs-clojure-hash [coll]
  (let [hash-fn (:fn reimplementation-of-clojure-hash)
        hash-name (:name reimplementation-of-clojure-hash)]
    (doseq [val coll]
      (when (not= (hash val) (hash-fn val))
        (println (format "Found val with hash=0x%08x but %s=0x%08x: %s"
                         (hash val) hash-name (hash-fn val) val))))))


(defn cmp-seq-lexi
  "Compare sequences x and y lexicographically, using comparator
function cmpf to compare elements of each sequence."
  [cmpf x y]
  (loop [x x
         y y]
    (if (seq x)
      (if (seq y)
        (let [c (cmpf (first x) (first y))]
          (if (zero? c)
            (recur (rest x) (rest y))
            c))
        ;; else we reached end of y first, so x > y
        1)
      (if (seq y)
        ;; we reached end of x first, so x < y
        -1
        ;; Sequences contain same elements.  x = y
        0))))


;; If *set-type* is :sorted-set, solutions are sorted sets of vectors
;; of the form [:keyword [int-x int-y]].  I do not care what order
;; solutions are sorted in relative to each other, as long as they are
;; comparable.
(defn cmp-solns [a b]
  (cmp-seq-lexi compare (seq a) (seq b)))


(def all-set-types #{:sorted-set :hash-set})
(def ^:dynamic *set-type* nil)
;; Change *show-hash-stats* to false to skip showing hash stats.
;; Enabling them slows down the run time of the code.
(def ^:dynamic *show-hash-stats* true)


(defn empty-solution []
  (case *set-type*
    :hash-set #{}
    :sorted-set (sorted-set)))


(defn solution-set [solns]
  (case *set-type*
    :hash-set (set solns)
    :sorted-set (apply sorted-set-by cmp-solns solns)))


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
    (solution-set [(empty-solution)])   ; #{#{}}
    (solution-set
      (let [candidate (first pieces)]
        (for [solution (solve rows cols (rest pieces))
              x (range 0 cols)
              y (range 0 rows)
              :when (allowed? candidate [x y] solution)]
          (conj solution [candidate [x y]]))))))

(defn solve [rows cols pieces]
  (let [x (solve-wrapped rows cols pieces)]
    (println (format "Solve %s example set elem %s" (seq pieces) (first x)))
    (when *show-hash-stats*
      (print-all-hash-stats x)
      (check-java-vs-clojure-hash x))
    x))


(defn do-coordinates [max-coord-seq]
  (print-hash-val-header)
  (doseq [i max-coord-seq]
    (let [pairs (for [x (range 0 i), y (range 0 i)] [x y])]
      (println (format "All-pairs in [0 0] thru [%d %d]" (dec i) (dec i)))
      (print-all-hash-stats pairs)
      (check-java-vs-clojure-hash pairs))))


(defn do-nqueens [set-type]
  (binding [*set-type* set-type]
    (print-hash-val-header)
    (let [solution (time (solve
                ;;3 3 [:K :K :R]
                ;;4 4 [:R :R :N :N :N :N]
                ;;5 5 [:K :K :N :R :B :Q]  ; ~1 sec
                ;;5 6 [:K :K :N :R :B :Q]  ; sorted-set ~7 sec, hash-set ~10 sec
                6 6 [:K :K :N :R :B :Q]  ; sorted-set ~45 sec, hash-set ~7 min
                ;;6 7 [:K :K :N :R :B :Q]  ; sorted-set ~4.5 min, hash-set ~262 min
                ;;6 9 [:K :K :N :R :B :Q]  ; this is the one that Paul Butcher waited about a day for the Clojure version to finish
                ))]
      ;; (println (join "\n" solution))
      (println (count solution)))))


;; Performance of hashing long values, plus loop overhead
;; (time (hash-range-n 100000000))

(defn hash-range-n [n]
  (let [n (long n)]
    (loop [i 0, sum 0]
      (if (== i n)
        sum
        (recur (unchecked-inc i) (unchecked-add sum (long (hash i))))))))


(defn total-hash [xs]
  (loop [sum 0
         xs xs]
    (if-let [xs (seq xs)]
      (recur (unchecked-add sum (long (hash (first xs))))
             (next xs))
      sum)))


;; Performance testing of hashing many vectors of ints.  No two
;; vectors being hashed are identical, so hash computation cannot
;; simply return a cached value with no computation at all.

;; Timing is only done on the hash calculation, after all vector
;; construction has been completed.

;; I do not know yet whether the hashing is doing incremental
;; computation based upon previously calculated hashes on vectors that
;; share structure.  If so, that would come into play to speed up
;; these timing results.

;; (let [cs (doall (reductions conj [] (range 20000)))]
;;   (time (total-hash cs)))

;; Same as above, except for sets instead of vectors

;; (let [cs (doall (reductions conj #{} (range 20000)))]
;;   (time (total-hash cs)))



(def default-coordinates-args [20 50 100 400])


(defn show-usage [prog-name]
  (binding [*out* *err*]
    (printf "usage:
    %s [ help | -h | --help ]
    %s all
    %s nqueens { sorted-set | hash-set }
    %s coordinates [ maxcoord1 maxcoord2 ... ]  (default %s)
" prog-name prog-name prog-name prog-name default-coordinates-args)
    (flush)))


(def prog-name "lein run")


(defn -main [& args]
  (when (or (= 0 (count args))
            (#{"-h" "--help" "-help" "help"} (first args)))
    (show-usage prog-name)
    (System/exit 0))
  (let [[action & args] args]
    (case action

      "coordinates"
      (let [coordinates-args (if (zero? (count args))
                               default-coordinates-args
                               (map #(Long/parseLong %) args))]
        (do-coordinates coordinates-args))

      "nqueens"
      (let [set-type (keyword (first args))]
        (if (and (= 1 (count args))
                 (all-set-types set-type))
          (do-nqueens set-type)
          (do (binding [*out* *err*]
                (printf "Wrong number of args for 'nqueens' action\n"))
              (show-usage prog-name)
              (System/exit 1))))

      "all"
      (do
        (do-coordinates default-coordinates-args)
        (println "\nnqueens sorted-set\n")
        (do-nqueens :sorted-set)
        (println "\nnqueens hash-set\n")
        (do-nqueens :hash-set)))))
