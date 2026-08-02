import requests
import tempfile
from pathlib import Path

pdf = b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n4 0 obj\n<< /Length 44 >>\nstream\nBT /F1 24 Tf 72 120 Td (Hi) Tj ET\nendstream\nendobj\n5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\nxref\n0 6\n0000000000 65535 f \n0000000010 00000 n \n0000000061 00000 n \n0000000116 00000 n \n0000000255 00000 n \n0000000322 00000 n \ntrailer\n<< /Root 1 0 R /Size 6 >>\nstartxref\n382\n%%EOF\n"

tmp = tempfile.NamedTemporaryFile(suffix='.pdf', delete=False)
try:
    tmp.write(pdf)
    tmp.close()
    path = Path(tmp.name)
    login = requests.post('http://127.0.0.1:8000/auth/login', json={'username': 'sneha', 'password': 'sneha'}, timeout=10)
    print('login', login.status_code, login.text)
    token = login.json().get('access_token') if login.ok else None
    headers = {'Authorization': f'Bearer {token}'} if token else {}
    with open(path, 'rb') as f:
        upload = requests.post('http://127.0.0.1:8000/upload', files={'file': ('sample_statement.pdf', f, 'application/pdf')}, headers=headers, timeout=30)
    print('upload', upload.status_code, upload.text)
finally:
    path.unlink()
