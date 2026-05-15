SELECT 'tasks_by_type_status' AS section;
SELECT type, status, COUNT(*) AS n
FROM workspace_tasks
GROUP BY type, status
ORDER BY type, status;

SELECT 'tasks_total' AS section, COUNT(*) AS n FROM workspace_tasks;
SELECT 'contracts_total' AS section, COUNT(*) AS n FROM contracts;

SELECT 'contracts_by_source' AS section;
SELECT source, COUNT(*) AS n FROM contracts GROUP BY source ORDER BY source;

SELECT 'recent_tasks' AS section;
SELECT id, task_no, type, status, related_id, LEFT(title, 60) AS title
FROM workspace_tasks
ORDER BY id DESC
LIMIT 15;
