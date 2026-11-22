#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Official PlantUML JAR plus Graphviz dot for the detailed class diagrams.
: "${PLANTUML_JAR:?Set PLANTUML_JAR to the absolute path of your PlantUML jar}"
command -v dot >/dev/null || { echo 'Install Graphviz and make dot available on PATH.' >&2; exit 1; }
sources=(docs/uml/*.puml docs/uml/class-atlas/*.puml)
java -Djava.awt.headless=true -DPLANTUML_LIMIT_SIZE=30000 -jar "$PLANTUML_JAR" -tsvg -charset UTF-8 "${sources[@]}"
java -Djava.awt.headless=true -jar "$PLANTUML_JAR" -tpng -charset UTF-8 docs/uml/class-diagram.puml
