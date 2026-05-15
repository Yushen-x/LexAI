SELECT 'before_contracts' AS marker, COUNT(*) AS total
FROM contracts
WHERE source = 'PUBLIC_VERIFY';

SELECT 'before_tasks' AS marker, COUNT(*) AS total
FROM workspace_tasks
WHERE task_no >= 'WF-2026-007' AND task_no <= 'WF-2026-018';

DELETE FROM workspace_tasks
WHERE task_no >= 'WF-2026-007' AND task_no <= 'WF-2026-018';

DELETE FROM contracts
WHERE source = 'PUBLIC_VERIFY';

SELECT 'after_contracts' AS marker, COUNT(*) AS total
FROM contracts
WHERE source = 'PUBLIC_VERIFY';

SELECT 'after_tasks' AS marker, COUNT(*) AS total
FROM workspace_tasks
WHERE task_no >= 'WF-2026-007' AND task_no <= 'WF-2026-018';
