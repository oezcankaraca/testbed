#!/bin/bash

# Umgebungsvariablen für IP-Adresse und Interface
IP_ADDRESS=$IP_ADDRES # IP-Adresse aus der Umgebungsvariablen
INTERFACE="eth1" # Festgelegtes Interface

# Funktion zum Konfigurieren einer Netzwerkschnittstelle mit IP-Adresse
configure_interface() {
    local interface=$1
    local ip_address=$2

    ip addr add ${ip_address}/24 dev $interface
    echo "Neue IP-Adresse: $ip_address für Interface: $interface hinzugefügt."
}

# Konfiguriere die Netzwerkschnittstelle
configure_interface "$INTERFACE" "$IP_ADDRESS"

