#!/bin/bash

# Pfad zur JSON-Datei mit den Verbindungsdetails
CONNECTIONS_FILE="/app/connection-details.json"
SOURCE_PEER=$SOURCE_PEER # Umgebungsvariable für den Quellknoten

# Prüfen, ob die Datei existiert
if [ ! -f "$CONNECTIONS_FILE" ]; then
    echo "Die Datei $CONNECTIONS_FILE existiert nicht."
    exit 1
fi

echo "SOURCE_PEER: $SOURCE_PEER"

# Funktion zum Konfigurieren einer Netzwerkschnittstelle mit IP-Adresse
configure_interface() {
    local interface=$1
    local ip_address=$2 # Einzelne IP-Adresse

    ip addr add ${ip_address}/24 dev $interface

    echo "Für Source-Peer: " $SOURCE_PEER "wurde eine neue IP Adresse: " $ip_address "mit Interface: " $interface "hinzugefügt."  
}

# Funktion zum Finden der Eigenschaften einer spezifischen Verbindung
get_connection_properties() {
    local source_peer=$SOURCE_PEER
    local target_peer=$1
    local properties=$(jq -r --arg source "$source_peer" --arg target "$target_peer" \
        '.[] | select(.sourceName == $source and .targetName == $target) | "\(.latency) \(.bandwidth) \(.loss)"' \
        "$CONNECTIONS_FILE")
    echo $properties
}

# Schleife über alle CONNECTION-Umgebungsvariablen
for var in $(compgen -e | grep '^CONNECTION_'); do
    IFS=':' read -ra ADDR <<< "${!var}"
    interface=${ADDR[0]}
    ip_address_pair=(${ADDR[1]//,/ })
    local_ip=${ip_address_pair[0]}
    target_peer=${ip_address_pair[1]}
    target_ip=${ADDR[2]}

    echo "Konfiguriere Interface: $interface mit IP: $target_ip für Verbindung zu Peer: $target_peer"

    configure_interface "$interface" "$local_ip"

    read raw_latency bandwidth loss <<< $(get_connection_properties "${target_peer%:*}")
    latency=$(printf "%.0f" "$raw_latency") # Runden der Latenz auf Ganzzahl
    
     # Konfiguration der Netzwerkeigenschaften
    tc qdisc add dev $interface root handle 1: prio
    tc qdisc add dev $interface parent 1:3 handle 30: netem delay ${latency}ms
    tc filter add dev $interface protocol ip parent 1:0 prio 3 u32 match ip dst $target_ip/32 flowid 1:3

    if [ -n "$latency" ] && [ -n "$bandwidth" ] && [ -n "$loss" ]; then
        echo "Eigenschaften der Verbindung: Source $SOURCE_PEER, Target $target_peer, IP $target_ip - Latenz: ${latency}ms, Bandbreite: ${bandwidth}kbps, Paketverlust: ${loss}%"
    else
        echo "Keine Verbindungseigenschaften gefunden für Verbindung von $SOURCE_PEER zu $target_peer"
    fi
done

# Zeigt die aktuelle tc-Konfiguration an
echo "Aktuelle tc-Konfiguration:"
tc qdisc show
tc filter show

