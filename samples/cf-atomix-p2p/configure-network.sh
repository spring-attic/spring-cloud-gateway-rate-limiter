#!/usr/bin/env bash

cf add-network-policy peer --destination-app=peer --protocol=tcp --port=5701
