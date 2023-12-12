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
    local ip_addresses=($2) # IP-Adressen als Array

    for ip in "${ip_addresses[@]}"; do
        ip addr add ${ip}/24 dev $interface
    done
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

# Funktion zum Einstellen der Latenz, Bandbreite und des Paketverlusts für eine Ziel-IP
set_network_properties() {
    local interface=$1
    local latency=$2
    local bandwidth=$3
    local loss=$4
    local target_ip=$5

    echo "Setting latency=${latency}ms, bandwidth=${bandwidth}kbps, loss=${loss}% for $target_ip on $interface"

    # Einstellen der Netzwerkeigenschaften mit festem handle_id
    tc qdisc add dev $interface root handle 1: prio
    tc qdisc add dev $interface parent 1:3 handle 30: netem delay ${latency}ms
    tc filter add dev $interface protocol ip parent 1:0 prio 3 u32 match ip dst $target_ip/32 flowid 1:3

    echo "Network properties set for $target_ip on $interface"
}

# Schleife über alle CONNECTION-Umgebungsvariablen
for var in $(compgen -e | grep '^CONNECTION_'); do
    IFS=':' read -ra ADDR <<< "${!var}"
    interface=${ADDR[0]}
    ip_addresses=(${ADDR[1]//,/ })

    configure_interface "$interface" "${ip_addresses[*]}"

    for ip in "${ip_addresses[@]}"; do
        read latency bandwidth loss <<< $(get_connection_properties "$ip")
        if [ -n "$latency" ] && [ -n "$bandwidth" ] && [ -n "$loss" ]; then
            set_network_properties "$interface" "$latency" "$bandwidth" "$loss" "$ip"
        else
            echo "Keine Verbindungseigenschaften gefunden für $ip"
        fi
    done
done

# Zeigt die aktuelle tc-Konfiguration an
echo "Aktuelle tc-Konfiguration:"
tc qdisc show
tc filter show

