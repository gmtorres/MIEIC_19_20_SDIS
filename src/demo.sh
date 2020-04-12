java TestApp localhost:Peer1 RECLAIM 10000000

java TestApp localhost:Peer2 RECLAIM 10000000

java TestApp localhost:Peer3 RECLAIM 10000000

java TestApp localhost:Peer1 BACKUP paisagem.jpg 1

java TestApp localhost:Peer1 STATUS

java TestApp localhost:Peer2 STATUS

java TestApp localhost:Peer3 STATUS

java TestApp localhost:Peer1 RESTORE paisagem.jpg

java TestApp localhost:Peer1 DELETE paisagem.jpg

java TestApp localhost:Peer1 STATUS

java TestApp localhost:Peer2 STATUS

java TestApp localhost:Peer3 STATUS

java TestApp localhost:Peer2 RECLAIM 65000

java TestApp localhost:Peer3 BACKUP bigmac.jpg 2

java TestApp localhost:Peer2 RECLAIM 0

java TestApp localhost:Peer1 STATUS

java TestApp localhost:Peer2 STATUS

java TestApp localhost:Peer3 STATUS

java TestApp localhost:Peer3 RESTORE bigmac.jpg

java TestApp localhost:Peer3 DELETE bigmac.jpg