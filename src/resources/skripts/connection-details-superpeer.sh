#!/bin/bash

# Path to the JSON file with connection details
CONNECTIONS_FILE="/app/connection-details.json"
SOURCE_PEER=$SOURCE_PEER # Environment variable for the source node
IP_ADDRES=$IP_ADDRES # Environment variable for the IP address

# Check if the file exists
if [ ! -f "$CONNECTIONS_FILE" ]; then
    echo "The file $CONNECTIONS_FILE does not exist."
    exit 1
fi

echo "SOURCE_PEER: $SOURCE_PEER"

# Conditional statement to configure eth1, if SOURCE_PEER is not lectureStudioServer
if [ "$SOURCE_PEER" != "lectureStudioServer" ]; then
    ip addr add ${IP_ADDRES}/24 dev eth1
    echo "IP address $IP_ADDRES has been added to eth1 because SOURCE_PEER is not lectureStudioServer."
fi

# Function to configure a network interface with IP address
configure_interface() {
    local interface=$1
    local ip_address=$2 # Single IP address

    ip addr add ${ip_address}/24 dev $interface

    echo "For Source Peer: " $SOURCE_PEER " a new IP Address: " $ip_address "with Interface: " $interface "has been added."  
}

# Function to find the properties of a specific connection
get_connection_properties() {
    local source_peer=$SOURCE_PEER
    local target_peer=$1
    local properties=$(jq -r --arg source "$source_peer" --arg target "$target_peer" \
        '.[] | select(.sourceName == $source and .targetName == $target) | "\(.latency) \(.bandwidth) \(.loss)"' \
        "$CONNECTIONS_FILE")
    echo $properties
}

# Loop over all CONNECTION environment variables
for var in $(compgen -e | grep '^CONNECTION_'); do
    IFS=':' read -ra ADDR <<< "${!var}"
    interface=${ADDR[0]}
    ip_address_pair=(${ADDR[1]//,/ })
    local_ip=${ip_address_pair[0]}
    target_peer=${ip_address_pair[1]}
    target_ip=${ADDR[2]}

    echo "Configuring Interface: $interface with IP: $target_ip for connection to Peer: $target_peer"

    configure_interface "$interface" "$local_ip"

    read raw_latency bandwidth loss <<< $(get_connection_properties "${target_peer%:*}")
    latency=$(printf "%.0f" "$raw_latency") # Rounding latency to whole number

    # Configuration of network properties
    tc qdisc add dev $interface root handle 1: prio
    tc qdisc add dev $interface parent 1:3 handle 30: netem delay ${latency}ms
    tc filter add dev $interface protocol ip parent 1:0 prio 3 u32 match ip dst $target_ip/32 flowid 1:3

    if [ -n "$latency" ] && [ -n "$bandwidth" ] && [ -n "$loss" ]; then
        echo "Connection properties: Source $SOURCE_PEER, Target $target_peer, IP $target_ip - Latency: ${latency}ms, Bandwidth: ${bandwidth}kbps, Packet Loss: ${loss}%"
    else
        echo "No connection properties found for connection from $SOURCE_PEER to $target_peer"
    fi
done

# Display the current tc configuration
echo "Current tc configuration:"
tc qdisc show
tc filter show

