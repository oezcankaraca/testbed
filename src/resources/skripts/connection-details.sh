#!/bin/bash

# Pfad zur JSON-Datei mit den Verbindungsdetails
CONNECTIONS_FILE="/app/connection-details.json"

# Prüfen, ob die Datei existiert
if [ ! -f "$CONNECTIONS_FILE" ]; then
    echo "Die Datei $CONNECTIONS_FILE existiert nicht."
    exit 1
fi

# Die Umgebungsvariablen SOURCE_PEER und TARGET_PEERS sollten im Container gesetzt sein
SOURCE_PEER=$SOURCE_PEER
TARGET_PEERS=$TARGET_PEERS

echo "SOURCE_PEER: $SOURCE_PEER"
echo "TARGET_PEERS: $TARGET_PEERS"

# Funktion zum Finden der Eigenschaften einer spezifischen Verbindung
get_connection_properties() {
    local target_peer=$1
    jq -r --arg source "$SOURCE_PEER" --arg target "$target_peer" \
        '.[] | select(.sourceName == $source and .targetName == $target) | "Bandbreite: \(.bandwidth) Kbps, Latenz: \(.latency) ms, Paketverlust: \(.loss)%"' \
        "$CONNECTIONS_FILE"
}

# Schleife über alle TARGET_PEERS
IFS=',' read -ra ADDR <<< "$TARGET_PEERS"
for target in "${ADDR[@]}"; do
    properties=$(get_connection_properties "$target")
    if [ -n "$properties" ]; then
        echo "Eigenschaften der Verbindung von $SOURCE_PEER zu $target: $properties"
    else
        echo "Keine Verbindungseigenschaften gefunden für $SOURCE_PEER zu $target"
    fi
done

