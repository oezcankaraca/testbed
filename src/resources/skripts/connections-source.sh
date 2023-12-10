#!/bin/bash

# Pfad zur JSON-Datei mit den Verbindungsdetails
CONNECTIONS_FILE="/app/connection-details.json"

# Prüfen, ob die Datei existiert
if [ ! -f "$CONNECTIONS_FILE" ]; then
    echo "Die Datei $CONNECTIONS_FILE existiert nicht."
    exit 1
fi

# Umgebungsvariablen
SOURCE_PEER=$SOURCE_PEER
TARGET_PEERS=$TARGET_PEERS
TARGET_PEERS_IP=$TARGET_PEERS_IP

echo "SOURCE_PEER: $SOURCE_PEER"
echo "TARGET_PEERS: $TARGET_PEERS"
echo "TARGET_PEERS_IP: $TARGET_PEERS_IP"

# Funktion zum Finden und Runden der Latenzeigenschaften einer spezifischen Verbindung
get_connection_properties() {
    local target_peer=$1
    local properties=$(jq -r --arg source "$SOURCE_PEER" --arg target "$target_peer" \
        '.[] | select(.sourceName == $source and .targetName == $target) | "\(.latency) \(.bandwidth) \(.loss)"' \
        "$CONNECTIONS_FILE")
    echo $properties
}

# Funktion zum Einstellen der Latenz, Bandbreite und des Paketverlusts für jede Ziel-IP
set_network_properties() {
    local latency=$1
    local bandwidth=$2
    local loss=$3
    local target_ip=$4
    local handle_id=$5

    echo "Setting latency=${latency}ms, bandwidth=${bandwidth}kbps, loss=${loss}% for $target_ip"

    # Einstellen der Netzwerkeigenschaften
    tc qdisc add dev eth0 root handle 1: htb default 30
    tc class add dev eth0 parent 1: classid 1:${handle_id} htb rate ${bandwidth}kbit
    tc qdisc add dev eth0 parent 1:${handle_id} handle ${handle_id}: netem delay ${latency}ms loss ${loss}%
    tc filter add dev eth0 protocol ip parent 1: prio 1 u32 match ip dst $target_ip/32 flowid 1:${handle_id}

    echo "Network properties set for $target_ip"
}

# Schleife über alle TARGET_PEERS und TARGET_PEERS_IP
IFS=',' read -ra ADDR <<< "$TARGET_PEERS"
IFS=',' read -ra IP_ADDR <<< "$TARGET_PEERS_IP"
for i in "${!ADDR[@]}"; do
    target="${ADDR[$i]}"
    target_ip="${IP_ADDR[$i]}"
    read latency bandwidth loss <<< $(get_connection_properties "$target")
    if [ -n "$latency" ] && [ -n "$bandwidth" ] && [ -n "$loss" ]; then
        handle_id=$((100 + i))
        set_network_properties "$latency" "$bandwidth" "$loss" "$target_ip" "$handle_id"
    else
        echo "Keine Verbindungseigenschaften gefunden für $SOURCE_PEER zu $target"
    fi
done

# Zeigt die aktuelle tc-Konfiguration an
echo "Aktuelle tc-Konfiguration für eth0:"
tc qdisc show dev eth0
tc filter show dev eth0

