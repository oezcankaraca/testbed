#!/bin/bash

# Pfad zur JSON-Datei mit den Verbindungsdetails
CONNECTIONS_FILE="/app/connection-details.json"

# Prüfen, ob die Datei existiert
if [ ! -f "$CONNECTIONS_FILE" ]; then
    echo "Die Datei $CONNECTIONS_FILE existiert nicht."
    exit 1
fi

# Die Umgebungsvariablen SOURCE_PEER, TARGET_PEERS und TARGET_PEERS_IP sollten im Container gesetzt sein
SOURCE_PEER=$SOURCE_PEER
TARGET_PEERS=$TARGET_PEERS
TARGET_PEERS_IP=$TARGET_PEERS_IP

echo "SOURCE_PEER: $SOURCE_PEER"
echo "TARGET_PEERS: $TARGET_PEERS"
echo "TARGET_PEERS_IP: $TARGET_PEERS_IP"

# Funktion zum Finden der Eigenschaften einer spezifischen Verbindung
get_connection_properties() {
    local target_peer=$1
    jq -r --arg source "$SOURCE_PEER" --arg target "$target_peer" \
        '.[] | select(.sourceName == $source and .targetName == $target) | "\(.latency)"' \
        "$CONNECTIONS_FILE"
}

# Funktion zum Einstellen der Latenz mit tc
set_latency() {
    local latency=$1
    local target_ip=$2

    # Einstellen der Latenz mit tc Befehlen
    tc qdisc add dev eth0 root handle 1: prio
    tc qdisc add dev eth0 parent 1:3 handle 30: netem delay "${latency}ms"
    tc filter add dev eth0 protocol ip parent 1:0 prio 3 u32 match ip dst "$target_ip" flowid 1:3
}

# Schleife über alle TARGET_PEERS und TARGET_PEERS_IP
IFS=',' read -ra ADDR <<< "$TARGET_PEERS"
IFS=',' read -ra IP_ADDR <<< "$TARGET_PEERS_IP"
for i in "${!ADDR[@]}"; do
    target="${ADDR[$i]}"
    target_ip="${IP_ADDR[$i]}"
    properties=$(get_connection_properties "$target")
    if [ -n "$properties" ]; then
        echo "Eigenschaften der Verbindung von $SOURCE_PEER zu $target: Latenz: $properties ms"
        set_latency "$properties" "$target_ip"
    else
        echo "Keine Verbindungseigenschaften gefunden für $SOURCE_PEER zu $target"
    fi
done

