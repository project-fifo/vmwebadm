{
    "packages":
    {
	"small": 
	{
	    "brand": "joyent", 
	    "max_physical_memory": 256, 
	    "nics": [
		{
		    "ip": "dhcp", 
		    "nic_tag": "external"
		}
	    ], 
	    "nowait": true, 
	    "quota": 5, 
	    "zfs_io_priority": 10
	},
	"medium":
	{
	    "brand": "joyent", 
	    "max_physical_memory": 512, 
	    "nics": [
		{
		    "ip": "dhcp", 
		    "nic_tag": "external"
		}
	    ], 
	    "nowait": true, 
	    "quota": 10, 
	    "zfs_io_priority": 20
	},
	"large":
	{
	    "brand": "joyent", 
	    "max_physical_memory": 1024, 
	    "nics": [
		{
		    "ip": "dhcp", 
		    "nic_tag": "external"
		}
	    ], 
	    "nowait": true, 
	    "quota": 20, 
	    "zfs_io_priority": 30
	}

    }
}
