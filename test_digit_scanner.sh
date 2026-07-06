#!/bin/bash

# 🎯 Digit Scanner Test Script
# Usage: ./test_digit_scanner.sh

set -e

echo "🚀 Digit Scanner Test Script"
echo "=============================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Build
echo -e "${YELLOW}Step 1: Building APK...${NC}"
./gradlew assembleDebug -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Build successful${NC}"
else
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

# 2. Check device
echo -e "${YELLOW}Step 2: Checking device...${NC}"
DEVICES=$(adb devices | grep -v "^List" | grep -v "^$" | wc -l)
if [ $DEVICES -eq 0 ]; then
    echo -e "${RED}❌ No devices found${NC}"
    echo "Start an emulator or connect a device"
    exit 1
fi
echo -e "${GREEN}✅ Device found${NC}"
adb devices | grep -v "^List" | grep -v "^$"

# 3. Install
echo -e "${YELLOW}Step 3: Installing APK...${NC}"
adb install -r app/build/outputs/apk/debug/app-debug.apk -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Installation successful${NC}"
else
    echo -e "${RED}❌ Installation failed${NC}"
    exit 1
fi

# 4. Clear logs
echo -e "${YELLOW}Step 4: Clearing logs...${NC}"
adb logcat -c

# 5. Launch app
echo -e "${YELLOW}Step 5: Launching app...${NC}"
adb shell am start -n com.einkaufsscanner/.MainActivity

# 6. Show logs
echo -e "${YELLOW}Step 6: Monitoring logs...${NC}"
echo ""
echo "🔍 Watching for digit scanner logs..."
echo "📋 Expected log messages:"
echo "   - 'PreviewView created'"
echo "   - 'Camera provider obtained'"
echo "   - 'Live digit scanning camera bound'"
echo "   - 'Digits detected:'"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo ""

adb logcat | grep -E "DigitScanner|CameraManager|PreviewView" || true

# Keep script running
wait
