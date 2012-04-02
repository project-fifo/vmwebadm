(ns dtrace.metrics.fs
  (:require [dtrace :as dtrace]))

;; args[0]
;; typedef struct bufinfo {
;;     int b_flags;       /* flags */
;;     size_t b_bcount;   /* number of bytes */
;;     caddr_t b_addr;    /* buffer address */
;;     uint64_t b_blkno;  /* expanded block # on device */
;;     uint64_t b_lblkno; /* block # on device */
;;     size_t b_resid;    /* # of bytes not transferred */
;;     size_t b_bufsize;  /* size of allocated buffer */
;;     caddr_t b_iodone;  /* I/O completion routine */
;;     dev_t b_edev;      /* extended device */
;; } bufinfo_t;

;; args[1]
;; typedef struct devinfo {
;;     int dev_major;       /* major number */
;;     int dev_minor;       /* minor number */
;;     int dev_instance;    /* instance number */
;;     string dev_name;     /* name of device */
;;     string dev_statname; /* name of device + instance/minor */
;;     string dev_pathname; /* pathname of device */
;; } devinfo_t;

;; args[2]
;; typedef struct fileinfo {
;;     string fi_name;     /* name (basename of fi_pathname) */
;;     string fi_dirname;  /* directory (dirname of fi_pathname) */
;;     string fi_pathname; /* full pathname */
;;     offset_t fi_offset; /* offset within file */
;;     string fi_fs;       /* filesystem */
;;     string fi_mount;    /* mount point of file system */
;; } fileinfo_t;

(def field->d
  (merge
   dtrace/default-fields
   {    
    "optype" {:type :str
              :name "probefunc"}
    "size" {:type :int
            :name "arg2"}
    "latency" {:type :int
               :name "(timestamp-self->start[probefunc])"
               :decomposition "(timestamp - self->start[probefunc])/1000"}}))

(print (pr field->d) "\n")




(defn compile [zones
               {decomposition "decomposition"
                predicate "predicate"}]
  (let [pred (dtrace/compile-predicate field->d predicate)]
    (str
     "syscall::write:entry,"
     "syscall::read:entry"
     "{self->start[probefunc] = timestamp;}"
     "syscall::read:return,"
     "syscall::write:return"
     "/"
     "self->start[probefunc]"
     (if predicate 
       (str "&&" pred)
       "")
     "/"
     "{@["(dtrace/compile-decomposition field->d decomposition)"] = "
     (or
      (get-in field->d [decomposition :decomposition-fn])
      "count()") ";}")))

(dtrace/register-metric
 ["fs" "logical_ops"]
 {:module "fs"
  :stat "logical_ops"
  :label "logical filesystem operations"
  :interval "interval"
  :fields (keys field->d)
  :unit "operations"}
 compile)
