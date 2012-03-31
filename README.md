
General
=======

This project is ment to implement Joyent's cloud API (http://apidocs.joyent.com/sdcapidoc/cloudapi/#machines) ontop of vanilla SmartOS to allow an easyer maintainace and handling of the host.

You can use tools like SmartDC CLI (http://wiki.joyentcloud.com/display/jpc2/About+Using+the+Cloud+API).

Note the account part is just grouping machines by the owner_uuid right now.

Getting Started
===============

On your client
--------------
* First clone this repository
* make bootstrap
* edit the makefile, change the DEPLOY_* variables to fit your setup
* make deploy

On your SmartOS Server
----------------------
The default behavior for the server is to listen to 0.0.0.0:80, you can change this with ./client.sh port and host

* Add users as you want with: ./client.sh passwd <your user> <your pass>

Make it a service
-----------------

* run ./vmwebadm install

you can disable the service with ./vmwebadm disable and enable it with ./vmwebadm enable

The service is persistant over reboots!

On your client
--------------
You can now set up your sdc toos with:
```sh
sdc-setup --url http://<SmartOS IP>
Username (login): (<your user>)
Password: <your pass>
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
```plaintext
[ ] DescribeAnalytics
[ ] ListInstrumentations
[ ] GetInstrumentation
[ ] GetInstrumentationValue
[ ] GetInstrumentationHeatmap
[ ] GetInstrumentationHeatmapDetails
[ ] CreateInstrumentation
[ ] DeleteInstrumentation
```