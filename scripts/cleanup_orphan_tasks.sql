SELECT 'orphan_tasks_before' AS section, COUNT(*) AS n
FROM workspace_tasks t
WHERE t.type = 'CONTRACT_REVIEW'
  AND t.related_id REGEXP '^[0-9]+$'
  AND CAST(t.related_id AS UNSIGNED) NOT IN (SELECT id FROM contracts WHERE deleted = 0);

DELETE FROM workspace_tasks
WHERE type = 'CONTRACT_REVIEW'
  AND related_id REGEXP '^[0-9]+$'
  AND CAST(related_id AS UNSIGNED) NOT IN (SELECT id FROM contracts WHERE deleted = 0);

DELETE FROM workspace_tasks
WHERE type = 'CONTRACT_REVIEW'
  AND related_id NOT REGEXP '^[0-9]+$';

SELECT 'tasks_after' AS section, COUNT(*) AS n FROM workspace_tasks;

SELECT type, status, COUNT(*) AS n
FROM workspace_tasks
GROUP BY type, status
ORDER BY type, status;
