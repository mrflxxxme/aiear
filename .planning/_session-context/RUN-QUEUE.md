# RUN-QUEUE — autonomous runner interrupt queue

> Append-only log of runner interrupt events (ADR-011 D8): ack-needed / escalation / revert / stuck / complete. Pending entries are waiting for the founder; resolve with `/autonomy:ack <ID> <verdict>`. Written by `scripts/autonomy/run_queue.py`.
