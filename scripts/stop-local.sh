#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import os,signal,subprocess
for name,marker in [('frontend','vite'),('backend','application.jar')]:
    path=Path('.runtime')/(name+'.pid')
    if not path.exists():
        continue
    pid=int(path.read_text().strip())
    result=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
    if result.returncode==0 and marker in result.stdout and str(Path.cwd()) in result.stdout:
        os.kill(pid,signal.SIGTERM)
        print('Stopped '+name)
    path.unlink()
PY
if [ "${1:-}" = '--database' ]; then ./scripts/local-db.sh stop; fi
