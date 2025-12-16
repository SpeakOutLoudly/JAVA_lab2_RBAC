#!/bin/bash
# Quick start script for RBAC CLI System

echo "========================================"
echo "RBAC CLI System - Quick Start"
echo "========================================"
echo ""

echo "Step 1: Compiling project..."
mvn clean compile
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Compilation failed!"
    exit 1
fi

echo ""
echo "Step 2: Starting application..."
echo ""
echo "Default credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo "========================================"
echo ""

mvn exec:java -Dexec.mainClass="com.study.Main"
