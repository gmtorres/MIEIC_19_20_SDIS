java App localhost:Peer1 RECLAIM 10000000

java App localhost:Peer2 RECLAIM 10000000

java App localhost:Peer3 RECLAIM 10000000

java App localhost:Peer1 BACKUP paisagem.jpg 2

java App localhost:Peer1 STATUS

java App localhost:Peer2 STATUS

java App localhost:Peer3 STATUS

java App localhost:Peer1 RESTORE paisagem.jpg

java App localhost:Peer1 DELETE paisagem.jpg

java App localhost:Peer1 STATUS

java App localhost:Peer2 STATUS

java App localhost:Peer3 STATUS

java App localhost:Peer1 BACKUP bigmac.jpg 2

java App localhost:Peer2 RECLAIM 65000

java App localhost:Peer1 STATUS

java App localhost:Peer2 STATUS

java App localhost:Peer3 STATUS

java App localhost:Peer1 RESTORE bigmac.jpg

java App localhost:Peer1 DELETE bigmac.jpg