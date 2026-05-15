#!/usr/bin/env python3
"""SCP-upload a local file or directory to the remote server (recursive)."""
import os
import sys
import paramiko
from scp import SCPClient

HOST = os.environ.get("LEXAI_SSH_HOST", "124.223.111.253")
USER = os.environ.get("LEXAI_SSH_USER", "ubuntu")
PORT = int(os.environ.get("LEXAI_SSH_PORT", "22"))
PASSWORD = os.environ["LEXAI_SSH_PASSWORD"]


def progress(filename, size, sent):
    pct = (sent / size) * 100 if size else 0
    sys.stderr.write(
        f"\r{filename.decode() if isinstance(filename, bytes) else filename} "
        f"{sent}/{size} ({pct:5.1f}%)"
    )
    if size and sent >= size:
        sys.stderr.write("\n")


def main(local: str, remote: str) -> None:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, port=PORT, username=USER, password=PASSWORD,
                   look_for_keys=False, allow_agent=False, timeout=30)
    with SCPClient(client.get_transport(), progress=progress, socket_timeout=120) as scp:
        scp.put(local, remote, recursive=os.path.isdir(local))
    client.close()


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("usage: ssh_put.py LOCAL REMOTE", file=sys.stderr)
        sys.exit(2)
    main(sys.argv[1], sys.argv[2])
