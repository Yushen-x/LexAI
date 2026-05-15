SELECT id, contract_no, name, source, status, LENGTH(content) AS content_len
FROM contracts
WHERE contract_no = 'LX-2026-009';

SELECT task_no, type, status, related_id
FROM workspace_tasks
WHERE task_no IN ('WF-2026-007', 'WF-2026-008', 'WF-2026-009')
ORDER BY task_no;
