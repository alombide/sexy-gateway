(ns edge.phonebook.db-test
  (:require
   [edge.phonebook.db :as db]
   [clojure.test :refer :all]
   [monger.collection :as mc]
   [edge.system :as system]))


(def phonebook-colname "phonebook")
(def db (db/create-db (:mongodb (system/config :test)) (:phonebook (system/config :test))))

(defn clear-db! [db]
  (mc/remove db phonebook-colname))

(defn clear-db-fixture [f]
  (f)
  (clear-db! db))

(use-fixtures :each clear-db-fixture)

(deftest db-setup-test
  (is db "Check if db is initialized.")
  (is (= (db/count-entries db) 0) "Check if we start the tests with an empty db."))

(deftest add-entry-test
  (let [entry {:surname "surname" :firstname "firstname" :phone "1234567"}
        id (db/add-entry! db entry)
        result (db/get-entry db id)]
    (is (= (:surname entry) (:surname result)) "Compare surname field of added entry.")
    (is (= (:firstname entry) (:firstname result)) "Compare firstname field of added entry.")
    (is (= (:phone entry) (:phone result)) "Compare phone field of added entry.")
    (is (:_id result) "Checking if added entry has an _id field.")))


(deftest update-entry-test
  (let [entry {:surname "surname" :firstname "firstname" :phone "1234567"}
        id (db/add-entry! db entry)
        updated-entry {:surname "changeds" :firstname "changedf" :phone "111111"}]
    (db/update-entry! db id updated-entry)
    (let [result (db/get-entry db id)]
      (is (= (:surname updated-entry) (:surname result)) "Compare surname field of updated entry.")
      (is (= (:firstname updated-entry) (:firstname result)) "Compare firstname field of updated entry.")
      (is (= (:phone updated-entry) (:phone result)) "Compare phone field of updated entry.")
      (is (= (:_id result) id) "Checking if added entry has still the same _id."))))


(deftest delete-entry-test
  (let [entry {:surname "surname" :firstname "firstname" :phone "1234567"}
        id (db/add-entry! db entry)]
    (db/delete-entry! db id)
    (let [result (db/get-entry db id)]
      (is (nil? result) "Check if entry is deleted."))))


(deftest get-entries-test
  (db/add-entry! db {:surname "surname1" :firstname "firstname1" :phone "111111"})
  (db/add-entry! db {:surname "surname2" :firstname "firstname2" :phone "222222"})
  (is (= (count (db/get-entries db)) 2) "Check if get-entries returns all entries."))


(deftest count-entries-zero-test
  (is (= (db/count-entries db) 0) "Check if count-entries returns correct result for 0 entries."))

(deftest count-entries-test
  (db/add-entry! db {:surname "surname1" :firstname "firstname1" :phone "111111"})
  (db/add-entry! db {:surname "surname2" :firstname "firstname2" :phone "222222"})
  (is (= (db/count-entries db) 2) "Check if count-entries returns correct result."))


(deftest search-entries-zero-test
  (let [result (db/search-entries db "test")]
      (is (= (count result) 0) "Check if correct number of entries are found.")))

(deftest search-entries-no-match-test
  (db/add-entry! db {:surname "aaaaaa" :firstname "aaaaa" :phone "111111"})
  (let [result (db/search-entries db "test")]
    (is (= (count result) 0) "Check if correct number of entries are found.")))

(deftest search-entries-test
  (db/add-entry! db {:surname "testaaaa" :firstname "aaaaaa" :phone "111111"})
  (db/add-entry! db {:surname "aaatestaaa" :firstname "aaaaa" :phone "111111"})
  (db/add-entry! db {:surname "aaaaaatest" :firstname "aaaaa" :phone "111111"})
  (db/add-entry! db {:surname "aaaaaa" :firstname "testaaaaa" :phone "111111"})
  (db/add-entry! db {:surname "aaaaaa" :firstname "aaaaatest" :phone "111111"})
  (db/add-entry! db {:surname "aaaaaa" :firstname "aaaatestaaaa" :phone "111111"})
  (db/add-entry! db {:surname "aatestaa" :firstname "aaatestaaa" :phone "111111"})
  (db/add-entry! db {:surname "aaaaaa" :firstname "aaaaa" :phone "111111"})
  (let [result (db/search-entries db "test")
        sample (first result)]
    (is (= (count result) 7) "Check if correct number of entries are found.")
    (is (:_id sample) "Check if sample result has _id field.")
    (is (:surname sample) "Check if sample result has surname field.")
    (is (:firstname sample) "Check if sample result has firstname field.")
    (is (:phone sample) "Check if sample result has phone field.")))
