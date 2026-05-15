#!/usr/bin/env python3
"""Run a shell command on the LexAI demo server via SSH (password auth).

Usage:
  python scripts/ssh_run.py "command to run"
"""
import os
import sys
import paramiko

HOST = os.environ.get("LEXAI_SSH_HOST", "124.223.111.253")
USER = os.environ.get("LEXAI_SSH_USER", "ubuntu")
PORT = int(os.environ.get("LEXAI_SSH_PORT", "22"))
PASSWORD = os.environ["LEXAI_SSH_PASSWORD"]


def run(cmd: str) -> int:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, port=PORT, username=USER, password=PASSWORD,
                   look_for_keys=False, allow_agent=False, timeout=20)
    transport = client.get_transport()
    transport.set_keepalive(30)
    chan = transport.open_session()
    chan.get_pty()
    chan.exec_command(cmd)
    while True:
        data = chan.recv(4096)
        if not data:
            break
        sys.stdout.buffer.write(data)
        sys.stdout.flush()
    rc = chan.recv_exit_status()
    client.close()
    return rc


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: ssh_run.py 'command'", file=sys.stderr)
        sys.exit(2)
    sys.exit(run(sys.argv[1]))
