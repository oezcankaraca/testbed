#!/bin/bash

# Environment variables for IP address and interface
IP_ADDRESS=$IP_ADDRES # IP address from the environment variable
INTERFACE="eth1" # Specified interface

# Function to configure a network interface with an IP address
configure_interface() {
    local interface=$1
    local ip_address=$2

    ip addr add ${ip_address}/24 dev $interface
    echo "New IP address: $ip_address added for interface: $interface."
}

# Configure the network interface
echo "Configuring the network interface with the following details:"
echo "Interface: $INTERFACE"
echo "IP Address: $IP_ADDRESS"
configure_interface "$INTERFACE" "$IP_ADDRESS"
