#!/bin/bash
set -e
APK_OUTPUT="app/build/outputs/apk/debug/app-debug.apk"
BRANCH="main"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}"
echo "=========================================="
echo "   🌸 BLOOM V4 — BUILD & PUSH"
echo "=========================================="
echo -e "${NC}"

if [ ! -f "build.gradle.kts" ]; then
    echo -e "${BLUE}📥 Clone du dépôt GitHub...${NC}"
    git clone https://github.com/thibautfihey49-hue/Bloom.git .
fi

echo -e "${BLUE}📥 Pull dernières modifications...${NC}"
git pull origin "$BRANCH" || true

echo -e "${BLUE}📤 Commit & Push...${NC}"
git add .
read -p "💬 Message commit : " COMMIT_MSG
COMMIT_MSG=${COMMIT_MSG:-Mise à jour $(date +'%Y-%m-%d %H:%M')}
git commit -m "$COMMIT_MSG" || true
git push origin "$BRANCH"
echo -e "${GREEN}✅ Poussé sur GitHub !${NC}"

echo -e "${BLUE}🔨 Build APK Debug...${NC}"
chmod +x ./gradlew
./gradlew clean assembleDebug

if [ -f "$APK_OUTPUT" ]; then
    echo -e "${GREEN}✅ SUCCÈS !${NC}"
    echo "📦 APK : $APK_OUTPUT"
    echo "📊 Taille : $(du -h $APK_OUTPUT | cut -f1)"
else
    echo -e "${RED}❌ ÉCHEC DU BUILD${NC}"
    exit 1
fi
