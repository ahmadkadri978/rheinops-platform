#!/bin/sh
set -eu

demo_url="https://rheinops-portfolio-demo.kadriahmad59.chatgpt.site"
required_documents="
docs/portfolio/README.md
docs/portfolio/demo-scenario.md
docs/portfolio/walkthrough.md
docs/portfolio/cv-case-study.md
docs/portfolio/release-notes-v1.0.0.md
docs/portfolio/assets/system-architecture.svg
"
required_screenshots="
docs/portfolio/assets/operations-overview.webp
docs/portfolio/assets/incident-timeline.webp
docs/portfolio/assets/support-sla.webp
docs/portfolio/assets/reliability-lab.webp
docs/portfolio/assets/architecture-console.webp
"

for path in $required_documents $required_screenshots; do
  if [ ! -s "$path" ]; then
    echo "Missing or empty portfolio asset: $path" >&2
    exit 1
  fi
done

for path in $required_screenshots; do
  riff_signature="$(od -An -c -N4 "$path" | tr -d ' \n')"
  webp_signature="$(od -An -c -j8 -N4 "$path" | tr -d ' \n')"
  if [ "$riff_signature" != "RIFF" ] || [ "$webp_signature" != "WEBP" ]; then
    echo "Invalid WebP signature: $path" >&2
    exit 1
  fi

  size="$(wc -c < "$path" | tr -d ' ')"
  if [ "$size" -lt 20000 ]; then
    echo "Screenshot is unexpectedly small: $path" >&2
    exit 1
  fi
done

grep -Fq "$demo_url" README.md
grep -Fq "$demo_url" docs/portfolio/README.md
grep -Fq "fictional" docs/portfolio/README.md
grep -Fq "Three-minute technical walkthrough" docs/portfolio/walkthrough.md
grep -Fq "<svg" docs/portfolio/assets/system-architecture.svg
grep -Fq "<version>1.0.0</version>" pom.xml

echo "Portfolio release assets are valid."
