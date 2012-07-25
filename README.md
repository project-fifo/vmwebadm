General
=======

This project is meant to implement Joyent's cloud API (http://apidocs.joyent.com/sdcapidoc/cloudapi/#machines) on-top of vanilla SmartOS to allow for easier maintenance and handling of the host.

You can use tools like SmartDC CLI (http://wiki.joyentcloud.com/display/jpc2/About+Using+the+Cloud+API).

Note the account part is just grouping machines by the owner_uuid right now.

Getting Started
===============

Watch this first
----------------
http://www.youtube.com/watch?v=8dyAA-6Df-c

Grab the SmartDC Tools
----------------------
You can read the how to get started here: http://apidocs.joyent.com/sdcapidoc/cloudapi/#getting-started .
Or just grab do:

```
npm install smartdc -g
```

On your client
--------------
* First clone this repository
* make bootstrap
* edit the makefile, change the DEPLOY_* variables to fit your setup
* make deploy

On your SmartOS Server
----------------------
The default behaviour for the server is to listen to 127.0.0.1:80, you can change this with ./vmwebadm port and host

* By default there is admin user called admin with the password admin
* Add users as you want with: ./vmwebadm passwd <your user> <your pass>

Make it a service
-----------------

* run ./vmwebadmd install

you can disable the service with ./vmwebadmd disable and enable it with ./vmwebadmd enable

The service is persistent and will survive reboots!

On your client
--------------
You can now set up your sdc toos with:
    
```sh
sdc-setup http://<SmartOS IP>
Username (login): (<your user>)
Password: <your pass>
```

Configuration commands
======================

```
Configuration tool
  import package <package-file(s)> - imports one or more package files.
  default-dataset <dataset>        - sets the default dataset.
  passwd <user> <pass>             - adds a new user or resets a password for an existing one.
  list users                       - lists all known users
  promote <user>                   - grants a user admin rights.
  demote <user>                    - demotes a user from admin rights.
  delete <user>                    - deletes a user.
  port <port>                      - sets the listen port for the server.
  host <host>                      - sets the listen host for the server.
```

Currently supported sdc-commands
============================

```sh
sdc-listmachines
sdc-getmachine 54e85ffa-988c-47b3-90f1-9ee5c23a8849
sdc-startmachine 40e06b7a-314a-4028-a328-84619c47fc68
sdc-stopmachine 40e06b7a-314a-4028-a328-84619c47fc68
sdc-rebootmachine 40e06b7a-314a-4028-a328-84619c47fc68
sdc-deletemachine 5a749276-9b9f-44b3-83c4-5965a7525646
sdc-createmachine \
--dataset 31bc4dbe-5e06-11e1-907c-5bed6b255fd1 \
--package '{"brand": "joyent","zfs_io_priority": 30,"quota": 20,"nowait": true,"max_physical_memory": 256,"alias": "zone4","nics": [{"nic_tag": "external","ip": "dhcp"}]}'
sdc-createmachine \
--dataset 31bc4dbe-5e06-11e1-907c-5bed6b255fd1 \
--package small
sdc-resizemachine --package '{"max_physical_memory":128}' 54e85ffa-988c-47b3-90f1-9ee5c23a8849
sdc-listpackages
sdc-getoackage
```

API Overview
============

Keys
----
```plaintext
[X] ListKeys
[X] GetKey
[X] CreateKey
[X] DeleteKey
```

Datacenters
-----------
This won't be implemented at the current state since it does not make sense.

```plaintext
[-] ListDatacenters
[-] GetDatacenter
```

Datasets
--------
```plaintext
[X] ListDatasets
[X] GetDataset
```

Packages
--------
```plaintext
[X] ListPackages
[X] GetPackage
```

Machines
--------
```plaintext
[X] ListMachines
[X] GetMachine
[X] CreateMachine
[X] StopMachine
[X] StartMachine
[X] RebootMachine
[X] ResizeMachine
[ ] CreateMachineSnapshot
[ ] StartMachineFromSnapshot
[ ] ListMachineSnapshots
[ ] GetMachineSnapshot
[ ] DeleteMachineSnapshot
[X] UpdateMachineMetadata
[X] GetMachineMetadata
[X] DeleteMachineMetadata
[X] DeleteAllMachineMetadata
[X] AddMachineTags
[X] ListMachineTags
[X] GetMachineTag
[X] DeleteMachineTag
[X] DeleteMachineTags
[X] DeleteMachine
```

Analytics
---------
This part does not really do anything yet, just the management functions to edit instrumentations are present.

```plaintext
[X] DescribeAnalytics
[X] ListInstrumentations
[X] GetInstrumentation
[X] GetInstrumentationValue
[ ] GetInstrumentationHeatmap
[ ] GetInstrumentationHeatmapDetails
[X] CreateInstrumentation
[X] DeleteInstrumentation
```

In addition to the normal decomposition vmwebadm supports a json base format to describe decompositions, the idea is to allow complex calls take the following example:


```plaintext
sdc-createinstrumentation -m fs -s logical_ops -n '[{"latency": {"count": [{"*": [{"/": ["latency", 1000]}, 1000]},{"+": [{"*": [{"/": ["latency", 1000]}, 1000]}, 999]}]}}]' -p '{"eq": ["optype","write"]}'
```

this compiles to

```D
syscall::write:entry,
syscall::read:entry
{
  self->start[probefunc] = timestamp;
}

syscall::read:return,
syscall::write:return
/self->start[probefunc]&&(probefunc=="write")/
{
  @latency[(((timestamp - self->start[probefunc])/1000)*1000),((((timestamp - self->start[probefunc])/1000)*1000)+999)]=count();
}
```

The syntax is comparable to the one used for predicates:

```
result=[<aggr>+];
aggr={<name>: <aggrdef>}
name=/a-zZ-Z/*
fieldname=/a-zZ-Z/*
field=<function>|fieldname
function={<fn-name>: [<params>+]}
args=field
fn-name=fieldname|+|*|/|-
aggrdef={<aggrfn>: [<field>+, <args>+]}
aggrfn=count|sum|avg|min|max|lquantize|quantize
```
