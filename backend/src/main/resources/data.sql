INSERT INTO contracts
    (contract_no, name, contract_type, party_a, party_b, amount, content, status, source, created_at, updated_at, deleted)
VALUES
    ('LX-2026-001', '2026年度云服务框架采购合作协议', '采购合同', '科创甲公司', '云端乙科技', 1550000.00, '甲方委托乙方提供云资源与运维服务。合同已约定服务范围、付款节点和争议解决，但保密与验收条款仍需进一步细化。', 'UNDER_REVIEW', '演示数据导入', '2026-03-28 10:24:00', '2026-03-28 10:24:00', FALSE),
    ('LX-2026-002', '新办公园区弱电工程施工合同', '工程合同', '园区建设方', '弱电工程公司', 890000.00, '双方约定由乙方完成办公园区弱电施工，包含门禁、监控与综合布线。合同明确交付计划、验收节点和违约责任。', 'SIGNED', '演示数据导入', '2026-03-27 15:30:00', '2026-03-27 15:30:00', FALSE),
    ('LX-2026-003', '测试用人事外包补充协议', '服务合同', '甲方人力', '外包服务商', 50000.00, '本补充协议用于约定人事外包服务范围、结算周期、保密义务及服务考核标准。当前仍为草稿待补充违约责任。', 'DRAFT', '演示数据导入', '2026-03-12 09:00:00', '2026-03-12 09:00:00', FALSE),
    ('LX-2026-004', '高管竞业限制与保密期权协议', '保密/期权', '甲方集团', '高管员工', 0.00, '协议约定高管离职后竞业限制义务、期权归属安排及商业秘密保护要求，但解除条件与争议解决机制尚不完整。', 'UNDER_REVIEW', '演示数据导入', '2026-03-10 14:00:00', '2026-03-10 16:00:00', FALSE),
    ('LX-2026-005', '核心机房第二季度软硬件代维合同', '运维服务', '甲方数据中心', '代维厂商', 245000.00, '乙方负责核心机房巡检、故障处理、应急响应和值班支持，合同明确 SLA、服务报告和月度考核要求。', 'IN_PROGRESS', '演示数据导入', '2026-03-01 11:00:00', '2026-03-20 11:00:00', FALSE),
    ('LX-2026-006', '年度法律咨询服务框架协议', '服务合同', '甲方实业', '律师事务所', 120000.00, '乙方向甲方提供常年法律顾问服务，包括合同审查、争议咨询与合规培训，按季度结算服务费用。', 'COMPLETED', '演示数据导入', '2026-01-15 10:00:00', '2026-02-28 17:00:00', FALSE),
    ('LX-2026-007', '仓储场地租赁合同（已终止示例）', '租赁合同', '甲方物流', '乙方物业', 360000.00, '双方就仓储场地租赁、物业费用、消防责任和提前退租条件作出约定，后因业务调整提前终止。', 'TERMINATED', '演示数据导入', '2025-12-01 09:00:00', '2026-03-01 09:00:00', FALSE),
    ('LX-2026-008', '软件定制开发合同', '技术开发', '甲方互联网', '乙方软件工作室', 480000.00, '乙方按阶段交付定制开发成果，合同覆盖需求确认、里程碑验收、知识产权归属和售后维护义务。', 'SIGNED', '演示数据导入', '2026-03-25 18:00:00', '2026-03-26 10:00:00', FALSE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    contract_type = VALUES(contract_type),
    party_a = VALUES(party_a),
    party_b = VALUES(party_b),
    amount = VALUES(amount),
    content = VALUES(content),
    status = VALUES(status),
    source = VALUES(source),
    updated_at = VALUES(updated_at),
    deleted = VALUES(deleted);

-- 待办任务统一收敛为「合同审查」一类，related_id 与 contracts.id 一致，便于双向跳转与状态联动。
INSERT INTO workspace_tasks
    (task_no, title, type, related_id, initiator, status, created_at)
VALUES
    ('WF-2026-001', '合同审查待人工确认 · LX-2026-004 · 高管竞业限制与保密期权协议', 'CONTRACT_REVIEW', '4', '演示用户-王五', 'PENDING', '2026-03-28 09:10:00'),
    ('WF-2026-002', '合同审查待人工确认 · LX-2026-001 · 2026年度云服务框架采购合作协议', 'CONTRACT_REVIEW', '1', '演示用户-王五', 'IN_PROGRESS', '2026-03-28 07:20:00'),
    ('WF-2026-003', '合同审查通过 · LX-2026-002 · 新办公园区弱电工程施工合同', 'CONTRACT_REVIEW', '2', '演示用户-钱七', 'COMPLETED', '2026-03-26 14:00:00'),
    ('WF-2026-004', '合同审查退回修改 · LX-2026-003 · 测试用人事外包补充协议', 'CONTRACT_REVIEW', '3', '演示用户-赵六', 'REJECTED', '2026-03-20 11:00:00')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    type = VALUES(type),
    related_id = VALUES(related_id),
    initiator = VALUES(initiator),
    status = VALUES(status),
    created_at = VALUES(created_at);
