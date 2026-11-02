#!/usr/bin/env bash
# Compiles the reproducer against each Truffle version given (default: 25.0 to 25.4) and prints the first errors.
# Usage: ./check-versions.sh [version...]   (needs JDK 21+ on JAVA_HOME and mvn on PATH)
cd "$(dirname "$0")"
versions=("$@")
[ ${#versions[@]} -eq 0 ] && versions=(25.0.4 25.1.3 25.2.4 25.3.4.1 25.4.4.1.1)
for v in "${versions[@]}"; do
    log="target-$v.log"
    if mvn -q -B clean compile -Dgraalvm.version="$v" > "$log" 2>&1; then
        echo "$v: compiles"
    else
        echo "$v: FAILS"
        grep -E "symbol:|location:" "$log" | sort -u | sed 's/^/    /'
    fi
done
