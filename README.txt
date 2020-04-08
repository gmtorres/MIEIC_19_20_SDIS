SDIS 2019/2020 - 2nd Semester
Project 1 -- Distributed Backup Service

Gustavo Torres up201706473
Joaquim Rodrigues up201704844



HOW TO COMPILE:

To compile the project, inside the src directory, run:

	javac *.java



HOW TO RUN:

.PEER

To run the peers:

java InitPeer version id acess_point cntrl_ip:cntrl_port bck_ip:bck_port rst_ip:rst_port

where:
	version - version of the peer and the protocols to use
	id - unique id among peers to distinguish between them
	acess_point - "name" of the peer, unique among peers in the same machine, represent the access name for the client interface app
	cntrl_ip:cntrl_port - multicast ip address and the port for the control channel, separated by ":", generally common among peers
	bck_ip:bck_port - multicast ip address and the port for the backup channel, separated by ":", generally common among peers
	rst_ip:rst_port - multicast ip address and the port for the restore channel, separated by ":", generally common among peers

To end the peer, click CNTRL+C.

.TESTAPP
To run the client interface app:

java App peer_ip:access_point op [args]

where:
	peer_ip:access_point -  ip address of the machine of the where the peer that we want to communicate is and the acces_point is the name of the peer. if the ip address is not given, it is considered as "localhost"
	op - operation we want to execute:
							BACKUP - backup a file to the system with a given replication degree
							RESTORE - restore a file from the system 
							DELETE - delete a file from the system
							RECLAIM - manage the available space of that peer to store chunks of other peers
							STATUS - retrieve information about the peer
	args - variable length parameter that depends on the operation:  (#=number of arguments )
							BACKUP - file_path desired_replication    (#=2)
											file_path - file_path of the file we want to backup
											desired_replication - replication degree  we desire to achive when storing the file in other peers
							RESTORE - file_path    (#=1)
											file_path - file_path of the file we want to delete
							DELETE - file_path    (#=1)
											file_path - file_path of the file we want to delete
							RECLAIM - size    (#=1)
											size - new memory size available to the peer				
							STATUS -    (#=0)
							