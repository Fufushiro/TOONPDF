#!/bin/bash
# Script para ver los logs de extracción de colores en tiempo real

echo "=== Watching PDF Color Extraction Logs ==="
echo "Press Ctrl+C to stop"
echo ""

adb logcat -c  # Limpiar logs anteriores
adb logcat | grep -E "(PdfColorExtractor|PdfCardColorizer|MainActivity.*applyPdfDynamicColors)"

