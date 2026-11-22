#!/usr/bin/env python3
"""Create local secrets without overwriting an existing environment."""
from pathlib import Path
import secrets
root=Path(__file__).resolve().parent.parent
path=root/'.env'
if path.exists():
    print('.env already exists; it was preserved.')
else:
    password=secrets.token_urlsafe(12)+'Aa1!'
    path.write_text('DB_USER=iitsm\nDB_PASSWORD='+secrets.token_urlsafe(24)+'\nDB_URL=jdbc:postgresql://127.0.0.1:55432/iitsm\nJWT_SECRET='+secrets.token_urlsafe(48)+'\nCORS_ORIGINS=http://localhost:5173,http://127.0.0.1:5173\nDEMO_ENABLED=true\nDEMO_PASSWORD='+password+'\n')
    path.chmod(0o600)
    print('Created .env with random database, JWT, and demo credentials.')
values=dict(line.split('=',1) for line in path.read_text().splitlines() if '=' in line and not line.startswith('#'))
print('Demo accounts: employee@iitsm.local / technician@iitsm.local / admin@iitsm.local')
print('Demo password: '+values.get('DEMO_PASSWORD','(unset)'))
print('Demo users are created only when DEMO_ENABLED=true and the users table is empty.')
