#! /usr/bin/bash

# Script for starting up the rmiregistry
# Checks if there is one argument (other than the name)
#  in that case it assumes to be the port on which 
#  the rmiregistry will be listening to

# Check number input arguments
argc=$#
port=""

if ((argc > 1 ))
then
	echo "Usage: $0 [<port_no>]]"
	exit 1
fi

if ((argc==1))
then
	port=$1
fi

rmiregistry $port